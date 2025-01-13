class Player(val playerId: Int, var life: Int = 100, var lastDropped: Long = 0) {
  private var x: Int = 0
  private var y: Int = 0

  private var lastMoved: Long = 0
  private val moveDelay = 200 // 200ms between moves = 5 moves per second max

  // Add this method to check if player can move
  def canMove: Boolean =
    lastMoved + moveDelay <= System.currentTimeMillis()

  // val cooldown = 7500 // too long because all players move too fast
  val cooldown = 3500

  def getPos: (Int, Int) = (x, y)

  // modified for player cooldonw
//  def setPos(pos: (Int, Int)): Unit = {
//    x = pos._1
//    y = pos._2
//  }
  def setPos(pos: (Int, Int)): Unit = {
    if (canMove) {
      x = pos._1
      y = pos._2
      lastMoved = System.currentTimeMillis()
    }
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