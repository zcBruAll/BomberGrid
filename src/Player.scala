class Player(val playerId: Int, var life: Int = 100, var lastDropped: Long = 0) {
  private var x: Int = 0
  private var y: Int = 0

  private val cooldown = 7500

  def getPos: (Int, Int) = (x, y)

  def setPos(pos: (Int, Int)): Unit = {
    x = pos._1
    y = pos._2
  }

  def takeDmg(dmg: Int): Unit = {
    life -= dmg
    if (!checkAlive) // Lost the game
      Motor.endGame(3 - playerId)
  }

  def canDrop: Boolean =
    lastDropped + cooldown <= System.currentTimeMillis()

  def checkAlive: Boolean = {
    life > 0
  }

  override def toString: String =
    f"${playerId * 16}"
}