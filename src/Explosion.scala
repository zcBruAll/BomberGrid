case class Explosion(x: Int, y: Int, startTime: Long) {
  def getOpacity(currentTime: Long): Float = {
    val age = currentTime - startTime
    val duration = 500 // explosion animation lasts 500ms
    math.max(0, 1 - (age.toFloat / duration))
  }

  def isFinished(currentTime: Long): Boolean = {
    currentTime - startTime > 500
  }
}