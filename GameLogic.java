// GameLogic.java
import java.io.*;
import java.net.*;
import java.util.*;

public class GameLogic {
    private List<ClientHandler> players;
    private String gameType;
    
    // RPS Game Variables
    private String[] rpsChoices;
    private boolean rpsWaitingForReplay = false;
    
    // TTT Game Variables
    private char[] tttBoard;
    private int tttCurrentPlayer;
    private char[] playerSymbols = {'X', 'O'};
    
    // Dice Game Variables
    private int[] diceRolls = new int[2];
    private int[] diceSums = new int[2];
    private int currentRoller = 0;
    private int rollsLeft = 3;
    private boolean diceGameOver = false;
    private int[] scores = new int[2];

    public GameLogic(List<ClientHandler> players, String gameType) {
        this.players = players;
        this.gameType = gameType.toUpperCase();
        
        switch (this.gameType) {
            case "RPS":
                rpsChoices = new String[2];
                break;
            case "TTT":
                tttBoard = new char[9];
                Arrays.fill(tttBoard, '-');
                tttCurrentPlayer = 0;
                break;
            case "DICE":
                Arrays.fill(diceRolls, 0);
                Arrays.fill(diceSums, 0);
                currentRoller = 0;
                rollsLeft = 3;
                diceGameOver = false;
                break;
        }
    }

    public void startGame() {
        switch (gameType) {
            case "RPS":
                Arrays.fill(rpsChoices, null);
                rpsWaitingForReplay = false;
                broadcast("GAME_START:RPS:Choose Rock (R), Paper (P), or Scissors (S)");
                broadcastScores();
                break;
                
            case "TTT":
                Arrays.fill(tttBoard, '-');
                tttCurrentPlayer = 0;
                for (int i = 0; i < players.size(); i++) {
                    players.get(i).sendMessage("GAME_START:TTT:You are " + playerSymbols[i] + 
                                             (i == tttCurrentPlayer ? " - Your turn!" : " - Opponent's turn"));
                }
                broadcastScores();
                updateTTTBoard();
                break;
                
            case "DICE":
                Arrays.fill(diceRolls, 0);
                Arrays.fill(diceSums, 0);
                currentRoller = 0;
                rollsLeft = 3;
                diceGameOver = false;
                broadcast("GAME_START:DICE:" + players.get(currentRoller).getPlayerName() + "'s turn! You have 3 rolls!");
                players.get(currentRoller).sendMessage("POPUP:It's your turn to roll the dice!");
                broadcastScores();
                break;
        }
    }

    public void processMove(ClientHandler sender, String move) {
        if (move.equalsIgnoreCase("GAME_RESULT_ACK")) {
            return;
        }
        
        switch (gameType) {
            case "RPS": 
                if (!rpsWaitingForReplay) handleRPSMove(sender, move);
                else handleReplayResponse(sender, move);
                break;
            case "TTT": 
                if (move.equalsIgnoreCase("YES") || move.equalsIgnoreCase("NO")) {
                    handleReplayResponse(sender, move);
                } else {
                    handleTTTMove(sender, move); 
                }
                break;
            case "DICE": 
                if (move.equalsIgnoreCase("YES") || move.equalsIgnoreCase("NO")) {
                    handleReplayResponse(sender, move);
                } else if (move.equalsIgnoreCase("ROLL")) {
                    handleDiceRoll(sender);
                }
                break;
        }
    }
    
    private void handleReplayResponse(ClientHandler sender, String response) {
        sender.setLastResponse(response);
        
        if (response.equalsIgnoreCase("NO")) {
            endSession();
        } else if (response.equalsIgnoreCase("YES")) {
            // Check if both players want to replay
            boolean allYes = true;
            for (ClientHandler player : players) {
                if (!player.getLastResponse().equalsIgnoreCase("YES")) {
                    allYes = false;
                    break;
                }
            }
            
            if (allYes) {
                startGame();
            }
        }
    }

    private void broadcast(String message) {
        for (ClientHandler player : players) {
            player.sendMessage(message);
        }
    }
    
    private void broadcastScores() {
        broadcast("SCORE:" + scores[0] + ":" + scores[1]);
    }

    // RPS Game Methods
    private void handleRPSMove(ClientHandler sender, String move) {
        int playerIndex = players.indexOf(sender);
        rpsChoices[playerIndex] = move.toUpperCase();
        sender.setLastResponse(move);
        
        sender.sendMessage("WAIT:Waiting for opponent...");
        
        if (rpsChoices[0] != null && rpsChoices[1] != null) {
            determineRPSWinner();
        }
    }

    private void determineRPSWinner() {
        String p1Choice = rpsChoices[0];
        String p2Choice = rpsChoices[1];
        String result;

        if (p1Choice.equals(p2Choice)) {
            result = "DRAW:It's a tie! Both chose " + getFullRPSName(p1Choice);
        } else if ((p1Choice.equals("R") && p2Choice.equals("S")) ||
                   (p1Choice.equals("P") && p2Choice.equals("R")) ||
                   (p1Choice.equals("S") && p2Choice.equals("P"))) {
            result = "WINNER:" + players.get(0).getPlayerName() + " wins! " + 
                    getFullRPSName(p1Choice) + " beats " + getFullRPSName(p2Choice);
            scores[0]++;
        } else {
            result = "WINNER:" + players.get(1).getPlayerName() + " wins! " + 
                    getFullRPSName(p2Choice) + " beats " + getFullRPSName(p1Choice);
            scores[1]++;
        }

        endGame(result);
    }

    private String getFullRPSName(String choice) {
        switch (choice) {
            case "R": return "Rock";
            case "P": return "Paper";
            case "S": return "Scissors";
            default: return choice;
        }
    }

    // TTT Game Methods
    private void handleTTTMove(ClientHandler sender, String move) {
        int playerIndex = players.indexOf(sender);
        if (playerIndex != tttCurrentPlayer) {
            sender.sendMessage("ERROR:Not your turn!");
            return;
        }

        try {
            int position = Integer.parseInt(move);
            if (position < 0 || position >= 9 || tttBoard[position] != '-') {
                sender.sendMessage("ERROR:Invalid move!");
                return;
            }

            tttBoard[position] = playerSymbols[playerIndex];

            if (checkTTTWin(playerSymbols[playerIndex])) {
                String winner = sender.getPlayerName();
                scores[playerIndex]++;
                endGame("WINNER:" + winner + " wins!");
            } else if (isTTTBoardFull()) {
                endGame("DRAW:Game ended in a draw!");
            } else {
                tttCurrentPlayer = 1 - tttCurrentPlayer;
                updateTTTBoard();
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("ERROR:Enter a number (0-8)!");
        }
    }

    private boolean checkTTTWin(char symbol) {
        // Check rows
        for (int i = 0; i < 9; i += 3) {
            if (tttBoard[i] == symbol && tttBoard[i+1] == symbol && tttBoard[i+2] == symbol) {
                return true;
            }
        }
        // Check columns
        for (int i = 0; i < 3; i++) {
            if (tttBoard[i] == symbol && tttBoard[i+3] == symbol && tttBoard[i+6] == symbol) {
                return true;
            }
        }
        // Check diagonals
        if (tttBoard[0] == symbol && tttBoard[4] == symbol && tttBoard[8] == symbol) {
            return true;
        }
        if (tttBoard[2] == symbol && tttBoard[4] == symbol && tttBoard[6] == symbol) {
            return true;
        }
        return false;
    }

    private boolean isTTTBoardFull() {
        for (char c : tttBoard) {
            if (c == '-') return false;
        }
        return true;
    }

    private void updateTTTBoard() {
        StringBuilder boardState = new StringBuilder("BOARD:");
        for (char c : tttBoard) {
            boardState.append(c);
        }
        
        for (int i = 0; i < players.size(); i++) {
            players.get(i).sendMessage(boardState.toString());
            if (i == tttCurrentPlayer) {
                players.get(i).sendMessage("TURN:Your turn (" + playerSymbols[i] + ")");
            } else {
                players.get(i).sendMessage("WAIT:Opponent's turn (" + playerSymbols[i] + ")");
            }
        }
    }

    private void handleDiceRoll(ClientHandler sender) {
        if (diceGameOver) return;
        
        int playerIndex = players.indexOf(sender);
        if (playerIndex != currentRoller) {
            sender.sendMessage("ERROR:Not your turn!");
            return;
        }

        int roll1 = new Random().nextInt(6) + 1;
        int roll2 = new Random().nextInt(6) + 1;
        diceRolls[0] = roll1;
        diceRolls[1] = roll2;
        diceSums[currentRoller] += roll1 + roll2;
        rollsLeft--;

        // Show the dice roll animation to both players
        broadcast("DICE_ROLL:" + roll1 + ":" + roll2);

        if (rollsLeft == 0) {
            if (currentRoller == 0) {
                // Switch to player 2 for their rolls
                currentRoller = 1;
                rollsLeft = 3;
                broadcast("TURN:" + players.get(currentRoller).getPlayerName() + "'s turn! You have 3 rolls!");
                players.get(currentRoller).sendMessage("POPUP:It's your turn to roll the dice!");
            } else {
                // Both players have rolled, determine winner
                determineDiceWinner();
            }
        } else {
            broadcast("TURN:" + players.get(currentRoller).getPlayerName() + "'s turn! You have " + rollsLeft + " rolls left!");
        }
    }

    private void determineDiceWinner() {
        diceGameOver = true;
        String result;
        
        if (diceSums[0] > diceSums[1]) {
            result = "WINNER:" + players.get(0).getPlayerName() + " wins with " + diceSums[0] + 
                    " vs " + diceSums[1] + "!";
            scores[0]++;
        } else if (diceSums[1] > diceSums[0]) {
            result = "WINNER:" + players.get(1).getPlayerName() + " wins with " + diceSums[1] + 
                    " vs " + diceSums[0] + "!";
            scores[1]++;
        } else {
            result = "DRAW:It's a tie! Both players scored " + diceSums[0];
        }

        endGame(result);
    }

    private void endGame(String result) {
        switch (gameType) {
            case "RPS":
                rpsWaitingForReplay = true;
                break;
            case "TTT":
                Arrays.fill(tttBoard, '-');
                break;
            case "DICE":
                Arrays.fill(diceRolls, 0);
                Arrays.fill(diceSums, 0);
                diceGameOver = true;
                break;
        }
        
        // First send the result
        broadcast(result);
        broadcastScores();
        
        // Then after a delay, ask for replay
        new java.util.Timer().schedule(new java.util.TimerTask() {
            public void run() {
                for (ClientHandler player : players) {
                    player.sendMessage("GAME_OVER:Play again? (YES/NO)");
                }
            }
        }, 1500);
    }

    private void endSession() {
        for (ClientHandler player : players) {
            player.sendMessage("SESSION_END:Returning to game selection...");
        }
    }
}