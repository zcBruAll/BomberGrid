import GraphicMotor._
import hevs.graphics.FunGraphics
import hevs.graphics.utils.GraphicsBitmap

import java.awt.event.{KeyAdapter, KeyEvent, MouseAdapter, MouseEvent, MouseMotionAdapter}
import java.awt.{Color, Font, Rectangle}
import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{ServerSocket, Socket}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.matching.Regex

object Motor extends App {
	// LAN variables
	// 92 : Uranium atomic number - 235 : Uranium 235 is used in nuclear bombs
	val port = 9235
	val ipRegex: Regex =
		"^((25[0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|\\d{1,2})$".r
	val ipChars: Array[String] =
		Array(".").concat(Array.range(0, 10).map(_.toString))
	var hostIp = ""

	// Front-end variables
	val cellSize = 40
	val diameter = (cellSize / 1.5).floor.toInt
	val fg: FunGraphics =
		new FunGraphics(50 + cellSize * 20, 50 + cellSize * 15, "BomberGrid")
	val menuWidth = 300
	var in: BufferedReader = _
	var out: PrintWriter = _
	var serverSocket: ServerSocket = _
	var clientSocket: Socket = _
	var isHost = true
	var pageId = 1

	// Graphic variables
	var menuTitle = "Welcome Bomber!"
	var clientTitle = "Enter host IP:"
	var hostTitle = "Hosting on:"
	var hostSubtitle = "Waiting for a client to connect"
	var hostButtonText = "Host a game"
	var clientButtonText = "Join a game"
	var quitButtonText = "Quit"
	var hostingIp = ""

	var buttonColor = new Color(0, 145, 51)
	var hoverButtonColor = new Color(8, 99, 40)

	val fontTitle = new Font("SansSerif", Font.BOLD, 36)
	val fontText = new Font("SansSerif", Font.PLAIN, 24)
	val fontImportant = new Font("SansSerif", Font.BOLD, 24)
	val fontSubtitle = new Font("SansSerif", Font.PLAIN, 18)

	val bombImg = new GraphicsBitmap("/res/img/bomb.png")
	val planeImg = new GraphicsBitmap("/res/img/plane.png")
	val player1Img = new GraphicsBitmap("/res/img/p1.png")
	val player2Img = new GraphicsBitmap("/res/img/p2.png")

	val hostButton = new Rectangle((fg.width - menuWidth) / 2, planeImg.getHeight + 15, 300, 50)
	val joinButton = new Rectangle((fg.width - menuWidth) / 2, planeImg.getHeight + 90, 300, 50)
	val exitButton = new Rectangle((fg.width - menuWidth) / 2, planeImg.getHeight + 165, 300, 50)
	var hostButtonColor = buttonColor
	var joinButtonColor = buttonColor
	var quitButtonColor = buttonColor

	// Game variables
	var room: Room = _
	var isPlaying = true
	var gameInitialized = false

	// Listener
	val mainMenuMouseListener: MouseAdapter = new MouseAdapter {
		override def mouseClicked(e: MouseEvent): Unit = {
			val x = e.getX
			val y = e.getY

			fg.mainFrame.getContentPane.removeMouseListener(mainMenuMouseListener)
			fg.mainFrame.getContentPane.removeMouseMotionListener(mainMenuMouseMotionListener)

			if (hostButton.contains(x, y)) {
				pageId = 2
				startHost()
			} else if (joinButton.contains(x, y)) {
				fg.mainFrame.addKeyListener(clientKeyListener)
				pageId = 3
				startClient()
			} else if (exitButton.contains(x, y))
				quit()
		}
	}
	val mainMenuMouseMotionListener: MouseMotionAdapter = new MouseMotionAdapter {
		override def mouseMoved(e: MouseEvent): Unit = {
			val x = e.getX
			val y = e.getY

			if (hostButton.contains(x, y)) {
				hostButtonColor = hoverButtonColor
				joinButtonColor = buttonColor
				quitButtonColor = buttonColor
			} else if (joinButton.contains(x, y)) {
				hostButtonColor = buttonColor
				joinButtonColor = hoverButtonColor
				quitButtonColor = buttonColor
			} else if (exitButton.contains(x, y)) {
				hostButtonColor = buttonColor
				joinButtonColor = buttonColor
				quitButtonColor = hoverButtonColor
			} else {
				hostButtonColor = buttonColor
				joinButtonColor = buttonColor
				quitButtonColor = buttonColor
			}
		}
	}
	val clientKeyListener = new KeyAdapter {
		override def keyPressed(e: KeyEvent): Unit = {
			if (ipChars.contains(e.getKeyChar.toString)) {
				hostIp += e.getKeyChar
			}

			if (e.getKeyCode == KeyEvent.VK_BACK_SPACE) {
				hostIp = hostIp.substring(0, math.max(hostIp.length - 1, 0))
			}

			if (e.getKeyCode == KeyEvent.VK_ENTER) {
				if (!ipRegex.matches(hostIp)) return

				clientTitle = "Connecting..."
				connectTo(hostIp)
			}
		}
	}
	val gameKeyListener = new KeyAdapter {
		override def keyPressed(e: KeyEvent): Unit = {
			if (isPlaying) {
				val keyToMove = Map(
					'w' -> 1,
					'd' -> 2,
					's' -> 4,
					'a' -> 8
				)

				val moveToVerify = keyToMove.getOrElse(e.getKeyChar, 0)

				if (moveToVerify != 0) {
					val player = room.getPlayer(if (isHost) 1 else 2)
					val move = room.tryMove(player, moveToVerify)
					if (move._1)
						send(s"UPDTMOVE${player.playerId};${move._2}:${move._3}")
				}

				if (e.getKeyCode == KeyEvent.VK_SPACE) {
					val player = room.getPlayer(if (isHost) 1 else 2)
					if (player.canDrop) {
						val pos = player.getPos
						val timestamp = System.currentTimeMillis()
						player.lastDropped = timestamp
						val bomb = Bomb(pos._1, pos._2, timestamp)

						room.addBomb(bomb)

						send(s"UPDTDROP${pos._1}:${pos._2};$timestamp")
					}
				}
			}
		}
	}

	Runtime.getRuntime.addShutdownHook(new Thread(() => {
		if (isHost && serverSocket != null && !serverSocket.isClosed && clientSocket != null && !clientSocket.isClosed) {
			send("EXIT")
			serverSocket.close()
		} else if (!isHost && clientSocket != null && !clientSocket.isClosed) {
			send("EXIT")
			clientSocket.close()
		}
	}))

	fg.mainFrame.getContentPane.addMouseListener(mainMenuMouseListener)
	fg.mainFrame.getContentPane.addMouseMotionListener(mainMenuMouseMotionListener)

	Future {
		while (true) {
			if (pageId == 4)
				room.checkExplosions()

			fg.frontBuffer.synchronized {
				displayPage()
			}

			fg.syncGameLogic(60)
		}
	}

	def displayMenu(): Unit = {
		fg.drawTransformedPicture(fg.width / 2, -25 + planeImg.getHeight / 2, 0, 1, planeImg)

		drawCenteredString(fg, menuTitle, (fg.width - menuWidth) / 2, planeImg.getHeight - 60, 300, 50, fontTitle)

		drawButton(fg, hostButton.x, hostButton.y, hostButton.width, hostButton.height, hostButtonText, hostButtonColor, Color.BLACK, 3, Color.BLACK, 10, fontText)
		drawButton(fg, joinButton.x, joinButton.y, joinButton.width, joinButton.height, clientButtonText, joinButtonColor, Color.BLACK, 3, Color.BLACK, 10, fontText)
		drawButton(fg, exitButton.x, exitButton.y, exitButton.width, exitButton.height, quitButtonText, quitButtonColor, Color.BLACK, 3, Color.BLACK, 10, fontText)
	}

	/**
	 * Start the game setup and loop as the host (Player1)
	 */
	def startHost(): Unit = {
		isHost = true
		serverSocket = new ServerSocket(port)
		hostingIp = java.net.InetAddress.getLocalHost.getHostAddress

		clientSocket = serverSocket.accept()

		initCommunication()

		initGame()
	}

	/**
	 * Initialize the discussion between the host and the client
	 */
	def initCommunication(): Unit = {
		in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream))
		out = new PrintWriter(clientSocket.getOutputStream, true)

		listen()
	}

	/**
	 * Listen for a new game info
	 */
	def listen(): Unit = {
		Future {
			while (true) {
				val msg = in.readLine()
				if (msg == null || msg.equalsIgnoreCase("exit")) {
					if (isHost)
						serverSocket.close()
					else
						clientSocket.close()
					gameInitialized = false
					pageId = 1
					fg.mainFrame.removeKeyListener(clientKeyListener)
					fg.mainFrame.removeKeyListener(gameKeyListener)
					fg.mainFrame.getContentPane.addMouseListener(mainMenuMouseListener)
					fg.mainFrame.getContentPane.addMouseMotionListener(mainMenuMouseMotionListener)

					if (isHost && serverSocket != null && !serverSocket.isClosed) serverSocket.close()
					if (!isHost && clientSocket != null && !clientSocket.isClosed) clientSocket.close()
					return
				}
				println(s"Received: $msg")
				updateGame(msg)
			}
		}
	}

	/**
	 * Update the game state with the received message
	 *
	 * @param msg The message used to update the game
	 */
	def updateGame(msg: String): Unit = {
		if (msg.startsWith("INIT")) {
			// INIT4x5;19:0:1:5:0-2:0:0:0:6-...
			val msgInfo = msg.substring(4).split(";")
			val dim = msgInfo(0).split("x")
			room = new Room(dim(0).toInt, dim(1).toInt)

			val r = msgInfo(1).split("-").map(_.split(":").map(_.toInt))
			for (i <- r.indices;
			     j <- r(i).indices) {
				if ((r(i)(j) & 16) == 16) {
					r(i)(j) -= 16
					room.movePlayer(room.getPlayer(1), i, j)
				}
				if ((r(i)(j) & 32) == 32) {
					r(i)(j) -= 32
					room.movePlayer(room.getPlayer(2), i, j)
				}
				room.getRoom(i)(j).buildWalls(r(i)(j))
			}
			gameInitialized = true
		} else if (msg.startsWith("UPDT")) {
			val newMsg = msg.substring(4)
			if (newMsg.startsWith("MOVE")) {
				// UPDTMOVE1;3:4
				val moveInfo = newMsg.substring(4).split(";")
				val playerId = moveInfo(0).toInt
				val pos = moveInfo(1).split(":").map(_.toInt)
				room.movePlayer(room.getPlayer(playerId), pos(0), pos(1))
			} else if (newMsg.startsWith("DROP")) {
				// UPDTDROP3:4;12335781
				val dropInfo = newMsg.substring(4).split(";")
				val pos = dropInfo(0).split(":").map(_.toInt)
				val timeDropped = dropInfo(1).toLong
				room.addBomb(Bomb(pos(0), pos(1), timeDropped))
			}
		} else if (msg.startsWith("WIN")) {
			// WIN2
			val winnerId = msg.substring(3)
			isPlaying = false
			gameInitialized = false

			menuTitle = s"Player $winnerId WON!"

			fg.mainFrame.removeKeyListener(gameKeyListener)
			fg.mainFrame.removeKeyListener(clientKeyListener)
			fg.mainFrame.getContentPane.addMouseListener(mainMenuMouseListener)
			fg.mainFrame.getContentPane.addMouseMotionListener(mainMenuMouseMotionListener)
			pageId = 1

			if (isHost) {
				if (serverSocket != null && !serverSocket.isClosed) serverSocket.close()
			} else {
				if (clientSocket != null && !clientSocket.isClosed) clientSocket.close()
			}
		} else {
			println(s"Incorrect message, skipping it: $msg")
		}
	}

	def initGame(): Unit = {
		room = new Room(20, 15)
		room.generateRoom()

		room.movePlayer(room.getPlayer(1), 0, 0)
		room.movePlayer(room.getPlayer(2), 14, 14)

		send(s"INIT${room.toString}")

		startGame()
	}

	/**
	 * Start the game loop
	 */
	def startGame(): Unit = {
		fg.mainFrame.addKeyListener(gameKeyListener)
		fg.mainFrame.removeKeyListener(clientKeyListener)
		pageId = 4
	}

	private def displayPage(): Unit = {
		fg.clear
		pageId match {
			case 1 =>
				displayMenu()
			case 2 =>
				displayHost()
			case 3 =>
				displayClient()
			case 4 =>
				displayGame(fg, room, cellSize, diameter)
		}
	}

	private def displayHost(): Unit = {
		drawCenteredString(fg, hostTitle, (fg.width - menuWidth) / 2, fg.height / 2 - 45, 300, 24, fontText)
		drawCenteredString(fg, hostingIp, (fg.width - menuWidth) / 2, fg.height / 2 - 15, 300, 24, fontImportant)
		drawCenteredString(fg, hostSubtitle, (fg.width - menuWidth) / 2, fg.height / 2 + 15, 300, 18, fontSubtitle)
	}

	private def displayClient(): Unit = {
		drawCenteredString(fg, clientTitle, (fg.width - menuWidth) / 2, fg.height / 2 - 85, 300, 30, fontText)
		drawTextbox(fg, (fg.width - menuWidth) / 2, fg.height / 2 - 25, 300, 50, s"$hostIp", Color.WHITE, Color.BLACK, 2, Color.BLACK, fontText)
	}

	/**
	 * Start the game setup and loop as the client (Player 2)
	 */
	def startClient(): Unit = {
		isHost = false

		clientTitle = "Enter host IP:"
	}

	def quit(): Unit = {
		System.exit(0)
	}

	def connectTo(ip: String): Unit = {
		clientSocket = new Socket(ip, port)
		println(s"Connected to the server at $ip:$port")

		initCommunication()

		while (!gameInitialized)
			Thread.sleep(100)
		startGame()
	}

	/**
	 * End the game with a winner
	 *
	 * @param winnerId The winner of the game
	 */
	def endGame(winnerId: Int): Unit = {
		send(f"WIN$winnerId")
		isPlaying = false
		gameInitialized = false

		menuTitle = s"Player $winnerId WON!"

		pageId = 1
		fg.mainFrame.removeKeyListener(clientKeyListener)
		fg.mainFrame.removeKeyListener(gameKeyListener)
		fg.mainFrame.getContentPane.addMouseListener(mainMenuMouseListener)
		fg.mainFrame.getContentPane.addMouseMotionListener(mainMenuMouseMotionListener)

		if (isHost) {
			if (serverSocket != null && !serverSocket.isClosed) serverSocket.close()
		} else {
			if (clientSocket != null && !clientSocket.isClosed) clientSocket.close()
		}
	}

	/**
	 * Send a message to the other player
	 *
	 * @param s The message to be sent
	 */
	def send(s: String): Unit = {
		println(s"Sent: $s")
		out.println(s)
	}
}