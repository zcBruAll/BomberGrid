class Room(val width: Int, val height: Int) {
  private val room = Array.fill[Cell](width, height)(new Cell)

  private val player1: Player = new Player(1)
  private val player2: Player = new Player(2)

  private val directionMatrice = Array[(Int, Int)]((0,-1), (1,0), (0,1), (-1,0))

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

  def canMove(player: Player, direction: Int): Boolean = {
    val (x, y) = player.getPos
    (room(x)(y).toInt & direction) == 0
  }

  def tryMove(player: Player, direction: Int): (Boolean, Int, Int) = {
    if (canMove(player, direction)) {
      val pos = player.getPos
      val index = (math.log(direction) / math.log(2)).toInt
      movePlayer(player, pos._1 + directionMatrice(index)._1, pos._2 + directionMatrice(index)._2)
      return (true, pos._1 + directionMatrice(index)._1, pos._2 + directionMatrice(index)._2)
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
