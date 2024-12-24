class Cell {
  private var walls: Int = 0

  /**
   * Build one or multiples wall for the cell according to the following notation:
   * 1: Top,
   * 2: Right,
   * 4: Bottom,
   * 8: Left
   *
   * Example: 14 = Right, Bottom and Left
   */
  def buildWalls(id: Int): Unit = {
    walls = walls | id
  }

  def getWalls(): Int = walls

  override def toString: String = {
    f"$walls"
  }
}
