class Player(val playerId: Int, var life: Int = 100, var lastDropped: Long = 0) {
  val cooldown = 3500
  private val moveDelay = 200
  // Positions variables
  private var x: Int = 0
  private var y: Int = 0
  private var lastMoved: Long = 0

  /**
   * Get the position of the player
   *
   * @return Returns a Tuple of 2 Int, x and y
   */
  def getPos: (Int, Int) = (x, y)

  // val cooldown = 7500 // too long because all players move too fast

  /**
   * Set the position of the player
   *
   * @param pos A tuple of 2 Int, x and y
   */
  def setPos(pos: (Int, Int)): Unit = {
    if (canMove) {
      x = pos._1
      y = pos._2
      lastMoved = System.currentTimeMillis()
    }
  }

  /**
   * Checks if the player can move, if it's waiting time before next movement is over
   *
   * @return `true` if the player can move, `false` otherwise
   */
  def canMove: Boolean =
    lastMoved + moveDelay <= System.currentTimeMillis()

  /**
   * Deal damages to the player
   *
   * @param dmg Damages to deal
   */
  def takeDmg(dmg: Int): Unit = {
    life -= dmg
    if (!checkAlive)
      Motor.endGame(3 - playerId)
  }

  /**
   * Checks if the player is still alive
   *
   * @return `true` if life is greather than 0, `false` otherwise
   */
  def checkAlive: Boolean = {
    life > 0
  }

  /**
   * Checks if the player can drop a bomb
   *
   * @return `true` if the cooldown is over, `false` otherwise
   */
  def canDrop: Boolean =
    lastDropped + cooldown <= System.currentTimeMillis()

  /**
   * Get the player formated as a String
   *
   * @return Returns the player id times 16 as a String
   */
  override def toString: String =
    f"${playerId * 16}"
}