import hevs.graphics.FunGraphics

import java.awt.{Color, Font}

object GraphicMotor {
	private val defaultFont = new Font("SansSerif", Font.BOLD, 24)
	private val defaultColor = Color.BLACK

	/**
	 * Render the game with FunGraphics
	 */
	def displayGame(fg: FunGraphics, room: Room, cellSize: Int, playerDiameter: Int): Unit = {
		fg.frontBuffer.synchronized {
			fg.clear(Color.WHITE)
			for (i <- 0 until room.width;
			     j <- 0 until room.height) {
				val x = 25 + cellSize * i
				val y = 25 + cellSize * j

				fg.setColor(Color.BLACK)
				val walls = room.getRoom(i)(j).getWalls
				if ((walls & 1) != 0) // Upper wall
					fg.drawLine(x, y, x + cellSize, y)
				if ((walls & 2) != 0) // Right wall
					fg.drawLine(x + cellSize, y, x + cellSize, y + cellSize)
				if ((walls & 4) != 0) // Bottom wall
					fg.drawLine(x, y + cellSize, x + cellSize, y + cellSize)
				if ((walls & 8) != 0) // Left wall
					fg.drawLine(x, y, x, y + cellSize)

				room.getActiveBombs.foreach { bomb =>
					val x = 25 + bomb.x * cellSize + cellSize / 4
					val y = 25 + bomb.y * cellSize + cellSize / 4
					fg.setColor(Color.RED)

					val bombSize = Motor.bombImg.getWidth
					val scale = (cellSize * .75D) / bombSize
					val ds = ((bombSize * scale) / 3).toInt
					fg.drawTransformedPicture(x + ds, y + ds, 0, scale, Motor.bombImg)
				}

				val posP1 = room.getPlayer(1).getPos
				val p1Size = Motor.player1Img.getHeight
				val p1Scale = cellSize / p1Size.toDouble
				fg.drawTransformedPicture(50 + posP1._1 * cellSize + (cellSize - (p1Size * p1Scale).toInt) / 2, 50 + posP1._2 * cellSize + (cellSize - (p1Size * p1Scale).toInt) / 2, 0, p1Scale, Motor.player1Img)

				val posP2 = room.getPlayer(2).getPos
				val p2Size = Motor.player1Img.getHeight
				val p2Scale = cellSize / p1Size.toDouble
				fg.drawTransformedPicture(50 + posP2._1 * cellSize + (cellSize - (p2Size * p2Scale).toInt) / 2, 50 + posP2._2 * cellSize + (cellSize - (p2Size * p2Scale).toInt) / 2, 0, p2Scale, Motor.player2Img)
			}
		}
	}

	def drawButton(fg: FunGraphics, x: Int, y: Int, width: Int, height: Int, t: String, background: Color, foreground: Color, borderWidth: Int = 1, borderColor: Color, font: Font = defaultFont): Unit = {
		fg.setColor(background)
		fg.drawFillRect(x, y, width, height)

		if (borderWidth > 0) {
			fg.setColor(borderColor)

			fg.drawFillRect(x, y, width, borderWidth)
			fg.drawFillRect(x, y + height - borderWidth, width, borderWidth)
			fg.drawFillRect(x, y, borderWidth, height)
			fg.drawFillRect(x + width - borderWidth, y, borderWidth, height)
		}

		drawCenteredString(fg, t, x, y, width, height, font, foreground)
	}

	def drawCenteredString(fg: FunGraphics, s: String, x: Int, y: Int, width: Int, height: Int, font: Font = defaultFont, color: Color = defaultColor): Unit = {
		val msgSize = fg.getStringSize(s, font)
		val dx = (width - msgSize.getWidth.toInt) / 2
		val dy = height - ((height - msgSize.getHeight.toInt) / 2) - 4
		fg.drawString(x + dx, y + dy, s, font, color)
	}

	def drawTextbox(fg: FunGraphics, x: Int, y: Int, width: Int, height: Int, t: String, background: Color, foreground: Color, borderWidth: Int = 1, borderColor: Color, font: Font = defaultFont): Unit = {
		fg.setColor(background)
		fg.drawFillRect(x, y, width, height)

		if (borderWidth > 0) {
			fg.setColor(borderColor)

			fg.drawFillRect(x, y, width, borderWidth)
			fg.drawFillRect(x, y + height - borderWidth, width, borderWidth)
			fg.drawFillRect(x, y, borderWidth, height)
			fg.drawFillRect(x + width - borderWidth, y, borderWidth, height)
		}

		val dy = height - ((height - fg.getStringSize(t, font).getHeight.toInt) / 2) - 4
		fg.drawString(x + 10, y + dy, t, font, foreground)
	}
}
