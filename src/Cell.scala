/**
 * Cell of the grid
 */
class Cell {
  private var walls: Int = 0
  private var playerId: Int = 0

  /**
   * Build one or multiples wall for the cell according to the following notation:
   * 1: Top,
   * 2: Right,
   * 4: Bottom,
   * 8: Left
   *
   * Example: 14 = Right, Bottom and Left
   *
   * @param id Id of the wall(s), if multiple sum their id
   */
  def buildWalls(id: Int): Unit = {
    walls |= id
  }

  /**
   * Return the sum of ids of the walls of the cell
   *
   * @return
   */
  def getWalls: Int = walls

  /**
   * Set the player ids located in the cell:
   * 1: Player 1
   * 2: Player 2
   *
   * @param id Id(s) of the players
   */
  def setPlayerId(id: Int): Unit = {
    if (id != 1 && id != 2) return
    playerId |= id
  }

  /**
   * Get the Cell formated as a String.
   *
   * @return Returns the sum of the wall and (player ids times 16) as a String
   */
  override def toString: String = {
    f"${this.toInt}"
  }

  /**
   * Get the Cell formated as an Int.
   *
   * @return Returns the sum of the wall and (player ids time 16) as a String
   */
  def toInt: Int = walls + playerId * 16
}
