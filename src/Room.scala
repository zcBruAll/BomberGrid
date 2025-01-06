class Room(val width: Int, val height: Int) {
  private val room = Array.fill[Cell](width, height)(new Cell)

  private val player1: Player = new Player(1)
  private val player2: Player = new Player(2)

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
  }

  override def toString: String = {
    f"${width}x$height;${room.map(_.mkString(":")).mkString("-")}"
  }
}
