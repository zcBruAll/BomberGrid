/**
 * Represents an explosion effect in the game
 *
 * @param x         X-coordinate of the explosion center
 * @param y         Y-coordinate of the explosion center
 * @param startTime Time when the explosion started
 */
case class Explosion(x: Int, y: Int, startTime: Long) {
  /**
   * Calculate the current opacity of the explosion effect
   *
   * @param currentTime Current game timestamp
   * @return Opacity value between 0.0 and 1.0
   */
  def getOpacity(currentTime: Long): Float = {
    val age = currentTime - startTime
    val duration = 500
    math.max(0, 1 - (age.toFloat / duration))
  }

  /**
   * Check if the explosion animation has completed
   *
   * @param currentTime Current game timestamp
   * @return True if the explosion effect should be removed
   */
  def isFinished(currentTime: Long): Boolean = {
    currentTime - startTime > 500
  }
}