import hevs.graphics.FunGraphics

import java.awt.Color

object Motor extends App {
    val room = new Room(20, 15)
    val p1 = new Player(1)
    val p2 = new Player(2)

    p1.setPos(0, 0)
    p2.setPos(18, 13)

    val cellSize = 40
    val diameter = (cellSize / 1.5).floor.toInt
    val fg = new FunGraphics(cellSize * room.width, cellSize * room.height, "BomberGrid")

    var isPlaying = true

    startGame()

    def send(s: String): Unit = {
        println(s)
    }

    def startGame(): Unit = {
        send(room.toString)
        while (isPlaying) {
            displayGame()

            fg.syncGameLogic(60)
        }
    }

    def endGame(winnerId: Int): Unit = {
        send(f"WIN:$winnerId")
        isPlaying = false
        // Handle further end of game
    }

    def displayGame(): Unit = {
        fg.clear(Color.WHITE)
        for (i <- 0 until room.width;
             j <- 0 until room.height) {
            val x = cellSize * i
            val y = cellSize * j

            fg.setColor(Color.BLACK)
            val walls = room.getRoom()(i)(j).getWalls()
            if ((walls & 1) != 0)       // Upper wall
                fg.drawLine(x, y, x + cellSize, y)
            if ((walls & 2) != 0)       // Right wall
                fg.drawLine(x + cellSize, y, x + cellSize, y + cellSize)
            if ((walls & 4) != 0)       // Bottom wall
                fg.drawLine(x, y + cellSize, x + cellSize, y + cellSize)
            if ((walls & 8) != 0)       // Left wall
                fg.drawLine(x, y, x, y + cellSize)

            val posP1 = p1.getPos
            fg.setColor(Color.CYAN)
            fg.drawFilledCircle(posP1._1 * cellSize + (cellSize - diameter) / 2, posP1._2 * cellSize + (cellSize - diameter) / 2, diameter)
            val posP2 = p2.getPos
            fg.setColor(Color.ORANGE)
            fg.drawFilledCircle(posP2._1 * cellSize + (cellSize - diameter) / 2, posP2._2 * cellSize + (cellSize - diameter) / 2, diameter)
        }
    }
}