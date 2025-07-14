// GameServer.java
import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final int PORT = 5555;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static List<GameSession> gameSessions = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("🎮 Game Server Started! Waiting for players...");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("🔗 New connection: " + socket);
                
                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);
                clientHandler.start();
                
                assignToGameSession(clientHandler);
            }
        } catch (IOException e) {
            System.out.println("❌ Server error: " + e.getMessage());
        }
    }
    
    private static void assignToGameSession(ClientHandler client) {
        for (GameSession session : gameSessions) {
            if (session.isWaitingForPlayer()) {
                session.addPlayer(client);
                return;
            }
        }
        
        GameSession newSession = new GameSession();
        newSession.addPlayer(client);
        gameSessions.add(newSession);
    }
    
    public static void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("🚪 Player left. Remaining: " + clients.size());
        
        // Remove empty game sessions
        gameSessions.removeIf(session -> session.getPlayerCount() == 0);
    }
}

class ClientHandler extends Thread {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String playerName;
    private GameSession gameSession;
    private String lastResponse;
    
    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println("❌ Error setting up player: " + e.getMessage());
        }
    }
    
    public void run() {
        try {
            playerName = in.readLine();
            System.out.println("👋 " + playerName + " joined!");
            
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (gameSession != null) {
                    gameSession.processMessage(this, inputLine);
                }
            }
        } catch (IOException e) {
            System.out.println("⚠️ " + playerName + " disconnected!");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("❌ Error closing socket.");
            }
            GameServer.removeClient(this);
            if (gameSession != null) {
                gameSession.removePlayer(this);
            }
        }
    }
    
    public void sendMessage(String message) {
        out.println(message);
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setGameSession(GameSession session) {
        this.gameSession = session;
    }
    
    public void setLastResponse(String response) {
        this.lastResponse = response;
    }
    
    public String getLastResponse() {
        return lastResponse;
    }
}

class GameSession {
    private List<ClientHandler> players;
    private String gameType;
    private GameLogic gameLogic;
    
    public GameSession() {
        players = new ArrayList<>(2);
    }
    
    public boolean isWaitingForPlayer() {
        return players.size() < 2;
    }
    
    public int getPlayerCount() {
        return players.size();
    }
    
    public void addPlayer(ClientHandler player) {
        players.add(player);
        player.setGameSession(this);
        
        if (players.size() == 1) {
            player.sendMessage("WAITING:Waiting for another player...");
        } else if (players.size() == 2) {
            broadcast("GAME_READY:Choose a game: RPS (Rock-Paper-Scissors), TTT (Tic-Tac-Toe), or DICE (Dice Roll Battle)");
        }
    }
    
    public void removePlayer(ClientHandler player) {
        players.remove(player);
        if (!players.isEmpty()) {
            players.get(0).sendMessage("OPPONENT_LEFT:Opponent left. Waiting for new player...");
        }
        if (gameLogic != null) {
            gameLogic = null;
        }
    }
    
    public void processMessage(ClientHandler sender, String message) {
        if (message.startsWith("CHOOSE_GAME:")) {
            gameType = message.substring("CHOOSE_GAME:".length());
            startGame();
        } else if (gameLogic != null) {
            gameLogic.processMove(sender, message);
        }
    }
    
    private void startGame() {
        gameLogic = new GameLogic(players, gameType);
        gameLogic.startGame();
    }
    
    public void broadcast(String message) {
        for (ClientHandler player : players) {
            player.sendMessage(message);
        }
    }
}