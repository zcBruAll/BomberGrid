case class Bomb(x: Int, y: Int, dropTime: Long, duration: Long = 5000){
  def hasExploded(currentTime: Long): Boolean = {
    currentTime >= dropTime + duration
  }
}
