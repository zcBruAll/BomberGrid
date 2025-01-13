class Room(val width: Int, val height: Int) {
  private val room = Array.fill[Cell](width, height)(new Cell)

  private val player1: Player = new Player(1)
  private val player2: Player = new Player(2)

  private var activeBombs: List[Bomb] = List()


  // Radar data
  private var activeRadarPickups: List[RadarPickup] = List()
  private var lastRadarSpawn: Long = 0
  private val radarSpawnDelay = 25 // 45 seconds between radar spawns
  private var player1HasRadar = false
  private var player2HasRadar = false
  private var lastRadarPing: Long = 0
  private val radarPingDelay = 3000 // 3 seconds between pings
  private val radarCooldown = 15000

  private var activeRadar: Option[RadarPickup] = None
  private var activeRadarEffect: Option[RadarEffect] = None


  private val directions = Map(
    1 -> (0, -1),   // Up
    2 -> (1, 0),    // Right
    4 -> (0, 1),    // Down
    8 -> (-1, 0)    // Left
  )

  def getRoom: Array[Array[Cell]] = room

  def getPlayer(id: Int): Player = {
    id match {
      case 1 => player1
      case 2 => player2
      case _ => player1
    }
  }

  def movePlayer(player: Player, x: Int, y: Int): Unit = {
    player.setPos(x, y)
    room(x)(y).setPlayerId(player.playerId)
  }

  def tryMove(player: Player, direction: Int): (Boolean, Int, Int) = {
    directions.get(direction) match {
      case Some((dx, dy)) =>
        val (x, y) = player.getPos
        if ((room(x)(y).toInt & direction) == 0) {
          val newX = x + dx
          val newY = y + dy
          if (newX >= 0 && newX < width && newY >= 0 && newY < height) {
            movePlayer(player, newX, newY)
            return (true, newX, newY)
          }
        }
      case None => // Invalid direction
    }

    (false, 0, 0)
  }

  def addBomb(bomb: Bomb): Unit = {
    activeBombs = activeBombs :+ bomb
  }

  def getActiveBombs: List[Bomb] = activeBombs

  def checkExplosions(): Unit = {
    val currentTime = System.currentTimeMillis()
    activeBombs.foreach { b =>
      if (b.hasExploded(currentTime))
        bombExplode(b)
    }

    activeBombs = activeBombs.filterNot(_.hasExploded(currentTime))
  }

  def bombExplode(b: Bomb): Unit = {
    val p1Pos = player1.getPos
    val p2Pos = player2.getPos

    val distP1 = math.sqrt(math.pow(b.x - p1Pos._1, 2) + math.pow(b.y - p1Pos._2, 2)).floor.toInt
    val distP2 = math.sqrt(math.pow(b.x - p2Pos._1, 2) + math.pow(b.y - p2Pos._2, 2)).floor.toInt

    if (!isLineOfSightBlocked(p1Pos._1, p1Pos._2, b.x, b.y))
      player1.takeDmg(25 * math.max(3 - distP1, 0))
    if (!isLineOfSightBlocked(p2Pos._1, p2Pos._2, b.x, b.y))
      player2.takeDmg(25 * math.max(3 - distP2, 0))
  }

  //-------------------- Radar Methods ----------------------
  def spawnRadar(): Unit = {
    val currentTime = System.currentTimeMillis()
    if (activeRadar.isEmpty && (currentTime - lastRadarSpawn > radarCooldown)) {

      //
//      val x = scala.util.Random.nextInt(width)
//      val y = scala.util.Random.nextInt(height)

      // placing in the central 3rd of the map only
      val x = 7 + scala.util.Random.nextInt(6) // x between 7-13 (middle third)
      val y = 5 + scala.util.Random.nextInt(4)
      activeRadar = Some(RadarPickup(x, y, currentTime))
    }
  }

  def checkRadarPickup(playerId: Int): Unit = { def getActiveRadar: Option[RadarPickup] = activeRadar
    val currentTime = System.currentTimeMillis()
    val playerPos = getPlayer(playerId).getPos

    activeRadar.foreach { radar =>
      if (radar.x == playerPos._1 && radar.y == playerPos._2) {
        // Player picked up radar
        activeRadar = None
        lastRadarSpawn = currentTime
        activeRadarEffect = Some(RadarEffect(playerId, currentTime))
      }
    }
  }

  def getRadarPingInfo(playerId: Int): Option[(Float, (Int, Int))] = {
    val currentTime = System.currentTimeMillis()
    activeRadarEffect.flatMap { effect =>
      if (effect.isActive(currentTime)) {
        val opacity = effect.getPingOpacity(currentTime)
        val opponentPos = getPlayer(3 - playerId).getPos
        Some((opacity, opponentPos))
      } else {
        activeRadarEffect = None
        None
      }
    }
  }

  // Add method to get radar pickups for rendering
  def getActiveRadarPickups: List[RadarPickup] = activeRadarPickups
  def getActiveRadar: Option[RadarPickup] = activeRadar

  // Add method to check if player can see opponent via radar
  def canPingOpponent(playerId: Int): Boolean = {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastRadarPing > radarPingDelay) {
      lastRadarPing = currentTime
      if (playerId == 1) player1HasRadar else player2HasRadar
    } else false
  }



  def buildWalls(x: Int, y: Int, wall: Int): Unit = {
    if (x >= 0 && x < width && y >= 0 && y < height) {
      room(x)(y).buildWalls(wall)
      if (x > 0 && (wall & 8) != 0) room(x-1)(y).buildWalls(2)
      if (x < width-1 && (wall & 2) != 0) room(x+1)(y).buildWalls(8)
      if (y > 0 && (wall & 1) != 0) room(x)(y-1).buildWalls(4)
      if (y < height-1 && (wall & 4) != 0) room(x)(y+1).buildWalls(1)
    }
  }

  def isLineOfSightBlocked(x0: Int, y0: Int, x1: Int, y1: Int): Boolean = {
    var x = x0
    var y = y0

    val dx = math.abs(x1 - x0)  // dx = 2 (from 3 to 1)
    val dy = math.abs(y1 - y0)  // dy = 2 (from 3 to 5)

    val sx = if (x0 < x1) 1 else -1  // sx = -1 (moving left)
    val sy = if (y0 < y1) 1 else -1  // sy = 1 (moving down)

    var err = dx - dy  // Initial error

    //    println(s"Starting at ($x,$y)")  // Debug logging

    while (x != x1 || y != y1) {
      val e2 = 2 * err

      // CRITICAL FIX: Check BOTH possible next positions before moving
      val willMoveX = e2 > -dy
      val willMoveY = e2 < dx

      // Check walls in both potential movement directions
      if (willMoveX) {
        val nextX = x + sx
        // Check wall in x direction
        if ((sx > 0 && (room(x)(y).getWalls & 2) != 0) ||
          (sx < 0 && (room(x)(y).getWalls & 8) != 0)) {
          //          println(s"Blocked by horizontal wall at ($x,$y)")
          return true
        }
      }

      if (willMoveY) {
        val nextY = y + sy
        // Check wall in y direction
        if ((sy > 0 && (room(x)(y).getWalls & 4) != 0) ||
          (sy < 0 && (room(x)(y).getWalls & 1) != 0)) {
          //          println(s"Blocked by vertical wall at ($x,$y)")
          return true
        }
      }

      // Now check diagonal wall crossing
      if (willMoveX && willMoveY) {
        val nextX = x + sx
        val nextY = y + sy
        // Check both cells we're crossing between
        if ((room(x)(nextY).getWalls & (if (sx > 0) 2 else 8)) != 0 ||
          (room(nextX)(y).getWalls & (if (sy > 0) 4 else 1)) != 0) {
          //          println(s"Blocked by diagonal crossing at ($x,$y) to ($nextX,$nextY)")
          return true
        }
      }

      if (e2 > -dy) {
        err -= dy
        x += sx
      }
      if (e2 < dx) {
        err += dx
        y += sy
      }

      //      println(s"Moved to ($x,$y)")  // Debug logging
    }

    false
  }

  def generateRoom(): Unit = {
    for (i <- 0 until width;
         j <- 0 until height) {
      if (i == 0)
        room(i)(j).buildWalls(8)
      else if (i == width - 1)
        room(i)(j).buildWalls(2)
      if (j == 0)
        room(i)(j).buildWalls(1)
      else if (j == height - 1)
        room(i)(j).buildWalls(4)
    }

    for (i <- 0 until width by 3;
         j <- 0 until height by 4) {
      (math.random() * 5).floor.toInt match {
        case 0 => generateChair(i, j)
        case 1 => generateL(i, j)
        case 2 => generateReverseL(i, j)
        case 3 => generateSan(i, j)
        case 4 => generatePillar(i, j)
      }
    }

    def generateChair(x: Int, y: Int): Unit = {
      buildWalls(x+1, y+1, 12)
      buildWalls(x+1, y+2, 11)
    }

    def generateL(x: Int, y: Int): Unit = {
      buildWalls(x, y+1, 5)
      buildWalls(x+1, y+1, 3)
      buildWalls(x+1, y+2, 10)
      buildWalls(x+1, y+3, 10)
    }

    def generateReverseL(x: Int, y: Int): Unit = {
      buildWalls(x+1, y, 10)
      buildWalls(x+1, y+1, 10)
      buildWalls(x+1, y+2, 12)
      buildWalls(x+2, y+2, 5)
    }

    def generateSan(x: Int, y: Int): Unit = {
      buildWalls(x+1, y+1, 5)
      buildWalls(x+1, y+2, 5)
    }

    def generatePillar(x: Int, y: Int): Unit = {
      buildWalls(x+1, y+1, 10)
      buildWalls(x+1, y+2, 10)
    }
  }

  override def toString: String = {
    f"${width}x$height;${room.map(_.mkString(":")).mkString("-")}"
  }
}
