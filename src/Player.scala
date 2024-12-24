import java.time.Instant
class Bomb(val x: Int, val y: Int, val droppedAt: Long = Instant.now().getEpochSecond) {
  def checkBomb(): Unit = {
    val now = Instant.now().getEpochSecond
    if (now - droppedAt >= 5) explode()
  }

  def explode(): Unit = {

  }
}

class Player(val playerId: Int, var life: Int = 100, var cooldown: Int = 0) {
  private var x: Int = 0
  private var y: Int = 0

  def getPos: (Int, Int) = (x, y)

  def setPos(pos: (Int, Int)): Unit = {
    x = pos._1
    y = pos._2
  }

  def dropBomb(): Unit = {
    if (cooldown > 0) return
    // Create Bomb
  }

  def takeDmg(dmg: Int): Unit = {
    life -= dmg
    if (!checkAlive()) // Lost the game
      Motor.endGame(3 - playerId)

  }

  def checkAlive(): Boolean = {
    life > 0
  }

  def reduceCooldown(): Unit = {
    cooldown = math.max(cooldown - 1, 0)
  }
}