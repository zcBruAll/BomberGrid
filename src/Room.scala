class Room(val width: Int, val height: Int) {
  private val room = Array.fill[Cell](width, height)(new Cell)

  private val player1: Player = new Player(1)
  private val player2: Player = new Player(2)

  private var activeBombs: List[Bomb] = List()

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

    if (!isWallObstructingBresenham(p1Pos._1, p1Pos._2, b.x, b.y))
      player1.takeDmg(25 * math.max(3 - distP1, 0))
    if (!isWallObstructingBresenham(p2Pos._1, p2Pos._2, b.x, b.y))
      player2.takeDmg(25 * math.max(3 - distP2, 0))
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

  def isWallObstructingBresenham(x0: Int, y0: Int, x1: Int, y1: Int): Boolean = {
    var x = x0
    var y = y0

    val dx = math.abs(x1 - x0)
    val dy = math.abs(y1 - y0)

    val sx = if (x0 < x1) 1 else -1
    val sy = if (y0 < y1) 1 else -1

    var err = dx - dy

    while (x != x1 || y != y1) {
      val currentCellWalls = room(x)(y).getWalls

      val dxMove = x1 - x
      val dyMove = y1 - y

      if ((dyMove == -1 && (currentCellWalls & 1) != 0) ||  // Up
          (dxMove == 1 && (currentCellWalls & 2) != 0) ||   // Right
          (dyMove == 1 && (currentCellWalls & 4) != 0) ||   // Down
          (dxMove == -1 && (currentCellWalls & 8) != 0)) {  // Left
        return true
      }

      val e2 = 2 * err
      if (e2 > -dy) {
        err -= dy
        x += sx
      }
      if (e2 < dx) {
        err += dx
        y += sy
      }
    }

    // Ensure visibility isn't blocked within the target cell
    val targetCellWalls = room(x1)(y1).getWalls
    val dxFinal = x1 - x0
    val dyFinal = y1 - y0

    if ((dyFinal == -1 && (targetCellWalls & 4) != 0) ||  // Target cell's "down" wall
        (dxFinal == 1 && (targetCellWalls & 8) != 0) ||   // Target cell's "left" wall
        (dyFinal == 1 && (targetCellWalls & 1) != 0) ||   // Target cell's "up" wall
        (dxFinal == -1 && (targetCellWalls & 2) != 0)) {  // Target cell's "right" wall
      return false
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
