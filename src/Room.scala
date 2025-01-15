class Room(val width: Int, val height: Int) {
  private val room = Array.fill[Cell](width, height)(new Cell)

  private val player1: Player = new Player(1)
  private val player2: Player = new Player(2)

  private val radarCooldown = 15000

  private val directions = Map(
    1 -> (0, -1), // Up
    2 -> (1, 0), // Right
    4 -> (0, 1), // Down
    8 -> (-1, 0) // Left
  )

  private var activeBombs: List[Bomb] = List()

  // Radar data
  private var lastRadarSpawn: Long = 0
  private var activeRadar: Option[RadarPickup] = None
  private var activeRadarEffect: Option[RadarEffect] = None
  private var activeExplosions: List[Explosion] = List()

  /**
   * Get the room
   *
   * @return Return the room as a 2D Array of Cell
   */
  def getRoom: Array[Array[Cell]] = room

  /**
   * Try to move the specified player in a specific direction
   *
   * @param player    The player to move
   * @param direction The direction to move in
   * @return Returns a Tuple of Boolean (`true` if can move) and 2 Int (the position moved to)
   */
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

  /**
   * move the player to a position
   *
   * @param player The player to move
   * @param x      The position in X
   * @param y      The position in Y
   */
  def movePlayer(player: Player, x: Int, y: Int): Unit = {
    player.setPos(x, y)
    room(x)(y).setPlayerId(player.playerId)
  }

  /**
   * Add a bomb to the list of active bombs
   *
   * @param bomb The bomb to add
   */
  def addBomb(bomb: Bomb): Unit = {
    activeBombs = activeBombs :+ bomb
  }

  /**
   * Get the list of active bombs
   *
   * @return Return a list of bombs
   */
  def getActiveBombs: List[Bomb] = activeBombs

  /**
   * Check if bombs must explode
   */
  def checkExplosions(): Unit = {
    val currentTime = System.currentTimeMillis()
    // Check for each bomb
    activeBombs.foreach { b =>
      if (b.hasExploded(currentTime))
        bombExplode(b)
    }

    // Remove bombs that have exploded
    activeBombs = activeBombs.filterNot(_.hasExploded(currentTime))
    cleanupExplosions()
  }

  /**
   * Handle the explosion of a bomb and its effects on players and the game world
   *
   * @param b The bomb that is exploding
   */
  def bombExplode(b: Bomb): Unit = {
    val currentTime = System.currentTimeMillis()

    val p1Pos = player1.getPos
    val p2Pos = player2.getPos
    val distP1 = math.sqrt(math.pow(b.x - p1Pos._1, 2) + math.pow(b.y - p1Pos._2, 2)).floor.toInt
    val distP2 = math.sqrt(math.pow(b.x - p2Pos._1, 2) + math.pow(b.y - p2Pos._2, 2)).floor.toInt

    if (!isLineOfSightBlocked(p1Pos._1, p1Pos._2, b.x, b.y))
      player1.takeDmg(25 * math.max(4 - distP1, 0))
    if (!isLineOfSightBlocked(p2Pos._1, p2Pos._2, b.x, b.y))
      player2.takeDmg(25 * math.max(4 - distP2, 0))

    for {
      dx <- -3 to 3
      dy <- -3 to 3
      newX = b.x + dx
      newY = b.y + dy
      if newX >= 0 && newX < width && newY >= 0 && newY < height
    } {
      val distance = math.sqrt(dx * dx + dy * dy).floor.toInt
      if (distance <= 3 && !isLineOfSightBlocked(b.x, b.y, newX, newY)) {
        activeExplosions = activeExplosions :+ Explosion(newX, newY, currentTime)
      }
    }
  }

  /**
   * Check if there is a clear line of sight between two points in the room
   * Uses a modified Bresenham's line algorithm to check for wall collisions
   *
   * @param startX Starting X-coordinate
   * @param startY Starting Y-coordinate
   * @param endX Ending X-coordinate
   * @param endY Ending Y-coordinate
   * @return True if the line of sight is blocked by any walls
   */
  def isLineOfSightBlocked(startX: Int, startY: Int, endX: Int, endY: Int): Boolean = {
    var currentX = startX
    var currentY = startY

    val distanceX = math.abs(endX - startX)
    val distanceY = math.abs(endY - startY)

    val stepX = if (startX < endX) 1 else -1
    val stepY = if (startY < endY) 1 else -1

    var error = distanceX - distanceY // Initial error

    while (currentX != endX || currentY != endY) {
      val errorDouble = 2 * error

      // Check BOTH possible next positions before moving
      val canMoveX = errorDouble > -distanceY
      val canMoveY = errorDouble < distanceX

      // Check wall in x direction
      if (canMoveX) {
        val nextX = currentX + stepX
        if ((stepX > 0 && (room(currentX)(currentY).getWalls & 2) != 0) ||
          (stepX < 0 && (room(currentX)(currentY).getWalls & 8) != 0)) {
          return true
        }
      }

      // Check wall in y direction
      if (canMoveY) {
        val nextY = currentY + stepY
        if ((stepY > 0 && (room(currentX)(currentY).getWalls & 4) != 0) ||
          (stepY < 0 && (room(currentX)(currentY).getWalls & 1) != 0)) {
          return true
        }
      }

      // Check diagonal wall crossing
      if (canMoveX && canMoveY) {
        val nextX = currentX + stepX
        val nextY = currentY + stepY
        if ((room(currentX)(nextY).getWalls & (if (stepX > 0) 2 else 8)) != 0 ||
          (room(nextX)(currentY).getWalls & (if (stepY > 0) 4 else 1)) != 0) {
          return true
        }
      }

      if (errorDouble > -distanceY) {
        error -= distanceY
        currentX += stepX
      }
      if (errorDouble < distanceX) {
        error += distanceX
        currentY += stepY
      }
    }

    false
  }

  /**
   * Remove finished explosion effects from the game
   */
  def cleanupExplosions(): Unit = {
    val currentTime = System.currentTimeMillis()
    activeExplosions = activeExplosions.filterNot(_.isFinished(currentTime))
  }

  /**
   * Get the list of active explosion effects in the game world
   *
   * @return List of currently active explosion effects
   */
  def getActiveExplosions: List[Explosion] = activeExplosions


  /**
   * Spawn a new radar pickup in the game world if conditions are met
   * Radar pickups only spawn in the central third of the map
   */
  def spawnRadar(): Unit = {
    val currentTime = System.currentTimeMillis()
    if (activeRadar.isEmpty && (currentTime - lastRadarSpawn > radarCooldown)) {

      // placing in the central 3rd of the map only
      val x = 7 + scala.util.Random.nextInt(6) // x between 7-13 (middle third)
      val y = 5 + scala.util.Random.nextInt(4)
      activeRadar = Some(RadarPickup(x, y, currentTime))
    }
  }

  /**
   * Check if a player has picked up the radar and activate its effects
   *
   * @param playerId ID of the player to check for radar pickup
   */
  def checkRadarPickup(playerId: Int): Unit = {
    val currentTime = System.currentTimeMillis()
    val playerPos = getPlayer(playerId).getPos

    activeRadar.foreach { radar =>
      if (radar.x == playerPos._1 && radar.y == playerPos._2) {
        activeRadar = None
        lastRadarSpawn = currentTime
        activeRadarEffect = Some(RadarEffect(playerId, currentTime))
      }
    }
  }

  /**
   * Get information about the radar ping for a specific player
   *
   * @param playerId ID of the player to get radar information for
   * @return Option containing ping opacity and opponent position if radar is active
   */
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

  def getPlayer(id: Int): Player = {
    id match {
      case 1 => player1
      case 2 => player2
      case _ => player1
    }
  }

  /**
   * Get the active radar, if it exists
   *
   * @return Returns an `Option` with the active radar
   */
  def getActiveRadar: Option[RadarPickup] = activeRadar

  /**
   * Generate the room by building pieces randomly
   */
  def generateRoom(): Unit = {
    // 4 external walls
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

    // For each 3 by 4 cells, call a random
    // function building walls following a specific shape
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

    /**
     * Generate a chair shaped piece
     *
     * @param x Starting position in X
     * @param y Starting position in Y
     */
    def generateChair(x: Int, y: Int): Unit = {
      buildWalls(x + 1, y + 1, 12)
      buildWalls(x + 1, y + 2, 11)
    }

    /**
     * Generate an L shaped piece
     *
     * @param x Starting position in X
     * @param y Starting position in Y
     */
    def generateL(x: Int, y: Int): Unit = {
      buildWalls(x, y + 1, 5)
      buildWalls(x + 1, y + 1, 3)
      buildWalls(x + 1, y + 2, 10)
      buildWalls(x + 1, y + 3, 10)
    }

    /**
     * Generate a reversed L shaped piece
     *
     * @param x Starting position in X
     * @param y Starting position in Y
     */
    def generateReverseL(x: Int, y: Int): Unit = {
      buildWalls(x + 1, y, 10)
      buildWalls(x + 1, y + 1, 10)
      buildWalls(x + 1, y + 2, 12)
      buildWalls(x + 2, y + 2, 5)
    }

    /**
     * Generate a piece shaped as the japanese character
     * to write 3, can also be seen as a ladder
     *
     * @param x Starting position in X
     * @param y Starting position in Y
     */
    def generateSan(x: Int, y: Int): Unit = {
      buildWalls(x + 1, y + 1, 5)
      buildWalls(x + 1, y + 2, 5)
    }

    /**
     * Generate a pillar shaped piece
     *
     * @param x Starting position in X
     * @param y Starting position in Y
     */
    def generatePillar(x: Int, y: Int): Unit = {
      buildWalls(x + 1, y + 1, 10)
      buildWalls(x + 1, y + 2, 10)
    }
  }

  /**
   * Build walls for a cell and for the contiguous cells.
   *
   * @param x    Position in X of the main cell
   * @param y    Position in Y of the main cell
   * @param wall The wall id(s) to build
   */
  def buildWalls(x: Int, y: Int, wall: Int): Unit = {
    if (x >= 0 && x < width && y >= 0 && y < height) {
      room(x)(y).buildWalls(wall)
      if (x > 0 && (wall & 8) != 0) room(x - 1)(y).buildWalls(2)
      if (x < width - 1 && (wall & 2) != 0) room(x + 1)(y).buildWalls(8)
      if (y > 0 && (wall & 1) != 0) room(x)(y - 1).buildWalls(4)
      if (y < height - 1 && (wall & 4) != 0) room(x)(y + 1).buildWalls(1)
    }
  }

  /**
   * Get the String value of the room.
   *
   * @return Return the String representation of the room
   */
  override def toString: String = {
    f"${width}x$height;${room.map(_.mkString(":")).mkString("-")}"
  }
}
