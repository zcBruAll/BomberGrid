class Room(val width: Int, val height: Int) {
  private val room = Array.fill[Cell](width, height)(new Cell)

  for (_ <- 0 to (width * height) / 2) {
    room((math.random() * width).floor.toInt)((math.random() * height).floor.toInt).buildWalls((math.random() * 9).floor.toInt)
  }

  def getRoom(): Array[Array[Cell]] = room

  override def toString: String = {
    f"${width}x$height;${room.map(_.mkString(":")).mkString("-")}"
  }
}
