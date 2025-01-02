import hevs.graphics.FunGraphics

import java.awt.Color
import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{ServerSocket, Socket}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Motor extends App {
    val port = 9235                     // Port of the game communication       92 : Uranium atomic number - 235 : Uranium 235 is used in nuclear bombs
    var in: BufferedReader = _          // messages coming in  - destined to this
    var out: PrintWriter = _            // messages coming out - destined to the other part
    var serverSocket: ServerSocket = _  // Server socket
    var clientSocket: Socket = _        // Client socket
    var isHost = true                   // Boolean if the current instance is the host or not

    println("Enter your choice:\nH - Host a game (Host)\nC - Join a game (Client)")
    scala.io.StdIn.readLine().toUpperCase match {
        case "H" => startHost()
        case "C" => startClient()
        case _ => println("Invalid choice, exiting...")
    }

    /**
     * Start the game setup and loop as the host (Player1)
     */
    def startHost(): Unit = {
        isHost = true
        serverSocket = new ServerSocket(port)
        val localIp = java.net.InetAddress.getLocalHost.getHostAddress
        println(s"Hosting on IP: $localIp:$port")

        println("Waiting for a client to connect...")

        clientSocket = serverSocket.accept()
        println("Client connected")

        initCommunication()
    }

    /**
     * Start the game setup and loop as the client (Player 2)
     */
    def startClient(): Unit = {
        isHost = false
        println("Enter the host's IP address: ")
        val hostIP = scala.io.StdIn.readLine()

        clientSocket = new Socket(hostIP, port)
        println(s"Connected to the server at $hostIP:$port")

        initCommunication()
    }

    /**
     * Initialize the discussion between the host and the client
     */
    def initCommunication(): Unit = {
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream))
        out = new PrintWriter(clientSocket.getOutputStream, true)

        listen()
    }

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

    /**
     * Send a message to the other player
     * @param s The message to be sent
     */
    def send(s: String): Unit = {
        out.println(s)
    }

    /**
     * Listen for a new game info
     */
    def listen(): Unit = {
        Future {
            while (true) {
                val msg = in.readLine()
                if (msg == null || msg.equalsIgnoreCase("exit")) {
                    println("Other player disconnected")
                    if (isHost)
                        serverSocket.close()
                    else
                        clientSocket.close()
                    return
                }
                println(s"Host says: $msg")
                updateGame(msg)
            }
        }
    }

    /**
     * Start the game loop
     */
    def startGame(): Unit = {
        while (isPlaying) {
            // TODO: Player actions

            displayGame()

            fg.syncGameLogic(60)
        }
    }

    /**
     * Update the game state with the received message
     * @param msg The message used to update the game
     */
    def updateGame(msg: String): Unit = {
        if (msg.startsWith("INIT")) {
            val msgInfo = msg.substring(4)
        } else if (msg.startsWith("UPDT")) {
            val newMsg = msg.substring(4)
        } else if (msg.startsWith("WIN")) {
            val newMsg = msg.substring(3)
        } else {
            println(s"Incorrect message, skipping it: $msg")
        }
    }

    /**
     * End the game with a winner
     * @param winnerId The winner of the game
     */
    def endGame(winnerId: Int): Unit = {
        send(f"WIN:$winnerId")
        isPlaying = false
        // TODO: Handle further end of game
    }

    /**
     * Render the game with FunGraphics
     */
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