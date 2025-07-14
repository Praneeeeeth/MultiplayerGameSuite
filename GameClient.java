import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class GameClient {
    private JFrame frame;
    private JPanel currentPanel;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String playerName;
    private boolean isDialogShowing = false;
    private int[] scores = new int[2];
    private char playerSymbol = ' ';
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameClient::new);
    }
    
    public GameClient() {
        createLoginScreen();
    }
    
    private void createLoginScreen() {
        frame = new JFrame("Multiplayer Game Suite");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(screenSize.width/2, screenSize.height/2);
        frame.setLocationRelativeTo(null);
        
        JPanel panel = new GradientPanel(new Color(52, 152, 219), new Color(46, 204, 113));
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JLabel titleLabel = new JLabel("Multiplayer Game Suite");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);
        
        JLabel nameLabel = new JLabel("Your Name:");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        nameLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(nameLabel, gbc);
        
        JTextField nameField = new JTextField(20);
        nameField.setFont(new Font("Arial", Font.PLAIN, 16));
        nameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 150)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(nameField, gbc);
        
        JLabel ipLabel = new JLabel("Server IP:");
        ipLabel.setFont(new Font("Arial", Font.BOLD, 16));
        ipLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(ipLabel, gbc);
        
        JTextField ipField = new JTextField(20);
        ipField.setText("localhost");
        ipField.setFont(new Font("Arial", Font.PLAIN, 16));
        ipField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 150)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(ipField, gbc);
        
        JButton connectBtn = createStyledButton("Connect", new Color(41, 128, 185));
        connectBtn.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(25, 15, 0, 15);
        panel.add(connectBtn, gbc);
        
        connectBtn.addActionListener(e -> {
            playerName = nameField.getText().trim();
            String serverIP = ipField.getText().trim();
            
            if (playerName.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please enter your name");
                return;
            }
            
            connectToServer(serverIP, playerName);
        });
        
        currentPanel = panel;
        frame.add(panel);
        frame.setVisible(true);
    }
    
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }
    
    private void connectToServer(String serverIP, String name) {
        try {
            socket = new Socket(serverIP, 5555);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            out.println(name);
            
            new Thread(this::listenForMessages).start();
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Could not connect to server: " + e.getMessage());
        }
    }
    
    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                processServerMessage(message);
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(frame, "Disconnected from server");
                frame.dispose();
                new GameClient();
            });
        }
    }
    
    private void processServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("WAITING:")) {
                showWaitingScreen(message.substring("WAITING:".length()));
            } else if (message.startsWith("GAME_READY:")) {
                showGameSelection(message.substring("GAME_READY:".length()));
            } else if (message.startsWith("GAME_START:")) {
                String[] parts = message.substring("GAME_START:".length()).split(":");
                startGame(parts[0], parts[1]);
            } else if (message.startsWith("WINNER:") || message.startsWith("DRAW:")) {
                showGameResult(message);
            } else if (message.startsWith("BOARD:")) {
                updateTTTBoard(message.substring("BOARD:".length()));
            } else if (message.startsWith("TURN:")) {
                showTurnMessage(message.substring("TURN:".length()));
            } else if (message.startsWith("WAIT:")) {
                showWaitMessage(message.substring("WAIT:".length()));
            } else if (message.startsWith("GAME_OVER:")) {
                // Handled in showGameResult now
            } else if (message.startsWith("ERROR:")) {
                showErrorMessage(message.substring("ERROR:".length()));
            } else if (message.startsWith("SCORE:")) {
                updateScores(message.substring("SCORE:".length()));
            } else if (message.startsWith("DICE_ROLL:")) {
                showDiceRoll(message.substring("DICE_ROLL:".length()));
            } else if (message.startsWith("DICE_RESULT:")) {
                showDiceResult(message.substring("DICE_RESULT:".length()));
            } else if (message.startsWith("SESSION_END:")) {
                showGameSelection("Game session ended. Choose a game:");
            } else if (message.startsWith("OPPONENT_LEFT:")) {
                showWaitingScreen(message.substring("OPPONENT_LEFT:".length()));
            } else if (message.startsWith("POPUP:")) {
                JOptionPane.showMessageDialog(frame, message.substring("POPUP:".length()));
            }
        });
    }
    
    private void updateScores(String scoreMessage) {
        String[] parts = scoreMessage.split(":");
        scores[0] = Integer.parseInt(parts[0]);
        scores[1] = Integer.parseInt(parts[1]);
    }
    
    private void showWaitingScreen(String message) {
        frame.getContentPane().remove(currentPanel);
        
        JPanel panel = new GradientPanel(new Color(52, 152, 219), new Color(155, 89, 182));
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        JLabel waitingLabel = new JLabel(message, SwingConstants.CENTER);
        waitingLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        waitingLabel.setForeground(Color.WHITE);
        panel.add(waitingLabel, BorderLayout.CENTER);
        
        JButton cancelBtn = createStyledButton("Cancel", new Color(231, 76, 60));
        cancelBtn.addActionListener(e -> {
            try { socket.close(); } catch (IOException ex) {}
            frame.dispose();
            new GameClient();
        });
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.add(cancelBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        currentPanel = panel;
        frame.add(panel);
        frame.revalidate();
    }
    
    private void showGameSelection(String message) {
        frame.getContentPane().remove(currentPanel);
        
        JPanel panel = new GradientPanel(new Color(46, 204, 113), new Color(52, 152, 219));
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        JLabel titleLabel = new JLabel(message, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        panel.add(titleLabel, BorderLayout.NORTH);
        
        JPanel gamesPanel = new JPanel(new GridLayout(3, 1, 20, 20));
        gamesPanel.setOpaque(false);
        
        JButton rpsBtn = createGameButton("Rock-Paper-Scissors", "RPS", new Color(241, 196, 15));
        JButton tttBtn = createGameButton("Tic-Tac-Toe", "TTT", new Color(230, 126, 34));
        JButton diceBtn = createGameButton("Dice Roll Battle", "DICE", new Color(155, 89, 182));
        
        gamesPanel.add(rpsBtn);
        gamesPanel.add(tttBtn);
        gamesPanel.add(diceBtn);
        
        panel.add(gamesPanel, BorderLayout.CENTER);
        
        JButton exitBtn = createStyledButton("Exit", new Color(231, 76, 60));
        exitBtn.addActionListener(e -> {
            try { socket.close(); } catch (IOException ex) {}
            frame.dispose();
        });
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.add(exitBtn);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        currentPanel = panel;
        frame.add(panel);
        frame.revalidate();
    }
    
    private JButton createGameButton(String text, String gameCode, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 18));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        button.addActionListener(e -> {
            out.println("CHOOSE_GAME:" + gameCode);
        });
        
        return button;
    }
    
    private void startGame(String gameType, String message) {
        frame.getContentPane().remove(currentPanel);
        
        switch (gameType) {
            case "RPS": showRPSGame(message); break;
            case "TTT": showTTTGame(message); break;
            case "DICE": showDiceGame(message); break;
        }
    }
    
    private void showRPSGame(String message) {
        JPanel panel = new GradientPanel(new Color(241, 196, 15), new Color(230, 126, 34));
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JPanel scorePanel = new JPanel(new GridLayout(1, 2));
        scorePanel.setOpaque(false);
        
        JLabel scoreLabel1 = new JLabel("Player 1: " + scores[0], SwingConstants.CENTER);
        JLabel scoreLabel2 = new JLabel("Player 2: " + scores[1], SwingConstants.CENTER);
        scoreLabel1.setFont(new Font("Arial", Font.BOLD, 16));
        scoreLabel2.setFont(new Font("Arial", Font.BOLD, 16));
        scoreLabel1.setForeground(Color.WHITE);
        scoreLabel2.setForeground(Color.WHITE);
        
        scorePanel.add(scoreLabel1);
        scorePanel.add(scoreLabel2);
        panel.add(scorePanel, BorderLayout.NORTH);
        
        JLabel statusLabel = new JLabel(message, SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 20));
        statusLabel.setForeground(Color.WHITE);
        panel.add(statusLabel, BorderLayout.CENTER);
        
        JPanel choicesPanel = new JPanel(new GridLayout(1, 3, 20, 20));
        choicesPanel.setOpaque(false);
        choicesPanel.setPreferredSize(new Dimension(600, 200));
        
        // Create animated emoji buttons
        JButton rockBtn = createAnimatedEmojiButton("R", new Color(52, 152, 219), "✊");
        JButton paperBtn = createAnimatedEmojiButton("P", new Color(155, 89, 182), "✋");
        JButton scissorsBtn = createAnimatedEmojiButton("S", new Color(46, 204, 113), "✌️");
        
        choicesPanel.add(rockBtn);
        choicesPanel.add(paperBtn);
        choicesPanel.add(scissorsBtn);
        
        panel.add(choicesPanel, BorderLayout.SOUTH);
        
        currentPanel = panel;
        frame.add(panel);
        frame.revalidate();
    }
    
    private JButton createAnimatedEmojiButton(String choice, Color bgColor, String emoji) {
        JButton button = new JButton();
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(150, 150));
        
        // Create emoji label with animation
        JLabel emojiLabel = new JLabel(emoji, SwingConstants.CENTER);
        emojiLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
        
        // Add pulsing animation
        javax.swing.Timer pulseTimer = new javax.swing.Timer(100, null);
        pulseTimer.addActionListener(new ActionListener() {
            private float alpha = 0.5f;
            private boolean increasing = true;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (increasing) {
                    alpha += 0.05f;
                    if (alpha >= 1.0f) increasing = false;
                } else {
                    alpha -= 0.05f;
                    if (alpha <= 0.5f) increasing = true;
                }
                emojiLabel.setForeground(new Color(1f, 1f, 1f, alpha));
            }
        });
        pulseTimer.start();
        
        button.setLayout(new BorderLayout());
        button.add(emojiLabel, BorderLayout.CENTER);
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.darker());
                pulseTimer.setDelay(50); // Speed up animation on hover
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
                pulseTimer.setDelay(100); // Slow down animation when not hovered
            }
        });
        
        button.addActionListener(e -> {
            // Add a click animation
            javax.swing.Timer clickTimer = new javax.swing.Timer(50, new ActionListener() {
                int count = 0;
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (count < 3) {
                        emojiLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60 + (count % 2 == 0 ? 10 : -10)));
                        count++;
                    } else {
                        emojiLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
                        ((javax.swing.Timer)e.getSource()).stop();
                    }
                }
            });
            clickTimer.start();
            
            out.println(choice);
        });
        
        return button;
    }
    
    private void showTTTGame(String message) {
        // Extract player symbol from message if available
        if (message.contains("You are X")) {
            playerSymbol = 'X';
        } else if (message.contains("You are O")) {
            playerSymbol = 'O';
        }
        
        JPanel panel = new GradientPanel(new Color(230, 126, 34), new Color(231, 76, 60));
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JPanel scorePanel = new JPanel(new GridLayout(1, 2));
        scorePanel.setOpaque(false);
        
        JLabel scoreLabel1 = new JLabel("Player X: " + scores[0], SwingConstants.CENTER);
        JLabel scoreLabel2 = new JLabel("Player O: " + scores[1], SwingConstants.CENTER);
        scoreLabel1.setFont(new Font("Arial", Font.BOLD, 16));
        scoreLabel2.setFont(new Font("Arial", Font.BOLD, 16));
        scoreLabel1.setForeground(Color.WHITE);
        scoreLabel2.setForeground(Color.WHITE);
        
        scorePanel.add(scoreLabel1);
        scorePanel.add(scoreLabel2);
        panel.add(scorePanel, BorderLayout.NORTH);
        
        JLabel statusLabel = new JLabel(message, SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 20));
        statusLabel.setForeground(Color.WHITE);
        panel.add(statusLabel, BorderLayout.NORTH);
        
        JPanel boardPanel = new JPanel(new GridLayout(3, 3, 5, 5));
        boardPanel.setOpaque(false);
        boardPanel.setPreferredSize(new Dimension(350, 350));
        
        for (int i = 0; i < 9; i++) {
            JButton btn = new JButton();
            btn.setFont(new Font("Arial", Font.BOLD, 60));
            btn.setBackground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            final int pos = i;
            btn.addActionListener(e -> {
                if (btn.getText().isEmpty()) {
                    out.println(String.valueOf(pos));
                }
            });
            boardPanel.add(btn);
        }
        
        panel.add(boardPanel, BorderLayout.CENTER);
        
        currentPanel = panel;
        frame.add(panel);
        frame.revalidate();
    }
    
    private void updateTTTBoard(String boardState) {
        if (currentPanel.getComponentCount() < 3) return;
        
        Component centerComp = currentPanel.getComponent(2);
        if (centerComp instanceof JPanel) {
            JPanel boardPanel = (JPanel) centerComp;
            Component[] buttons = boardPanel.getComponents();
            for (int i = 0; i < 9 && i < buttons.length; i++) {
                if (buttons[i] instanceof JButton) {
                    JButton btn = (JButton) buttons[i];
                    char c = boardState.charAt(i);
                    if (c == '-') {
                        btn.setText("");
                        btn.setBackground(Color.WHITE);
                    } else {
                        btn.setText(String.valueOf(c));
                        btn.setForeground(c == 'X' ? new Color(52, 152, 219) : new Color(231, 76, 60));
                        btn.setBackground(Color.WHITE);
                    }
                }
            }
        }
    }
    
    private void showDiceGame(String message) {
        JPanel panel = new GradientPanel(new Color(155, 89, 182), new Color(41, 128, 185));
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JPanel scorePanel = new JPanel(new GridLayout(1, 2));
        scorePanel.setOpaque(false);
        
        JLabel scoreLabel1 = new JLabel("Player 1: " + scores[0], SwingConstants.CENTER);
        JLabel scoreLabel2 = new JLabel("Player 2: " + scores[1], SwingConstants.CENTER);
        scoreLabel1.setFont(new Font("Arial", Font.BOLD, 16));
        scoreLabel2.setFont(new Font("Arial", Font.BOLD, 16));
        scoreLabel1.setForeground(Color.WHITE);
        scoreLabel2.setForeground(Color.WHITE);
        
        scorePanel.add(scoreLabel1);
        scorePanel.add(scoreLabel2);
        panel.add(scorePanel, BorderLayout.NORTH);
        
        JLabel turnLabel = new JLabel(message, SwingConstants.CENTER);
        turnLabel.setFont(new Font("Arial", Font.BOLD, 20));
        turnLabel.setForeground(Color.WHITE);
        panel.add(turnLabel, BorderLayout.NORTH);
        
        JPanel dicePanel = new JPanel(new GridLayout(1, 2, 20, 20));
        dicePanel.setOpaque(false);
        
        JLabel diceLabel1 = new JLabel("?", SwingConstants.CENTER);
        JLabel diceLabel2 = new JLabel("?", SwingConstants.CENTER);
        diceLabel1.setFont(new Font("Arial", Font.BOLD, 80));
        diceLabel2.setFont(new Font("Arial", Font.BOLD, 80));
        diceLabel1.setForeground(Color.WHITE);
        diceLabel2.setForeground(Color.WHITE);
        
        dicePanel.add(diceLabel1);
        dicePanel.add(diceLabel2);
        
        panel.add(dicePanel, BorderLayout.CENTER);
        
        JButton rollBtn = createStyledButton("Roll Dice", new Color(46, 204, 113));
        rollBtn.setFont(new Font("Arial", Font.BOLD, 18));
        rollBtn.addActionListener(e -> {
            animateDiceRoll(diceLabel1, diceLabel2, () -> {
                out.println("ROLL");
            });
        });
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.add(rollBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        currentPanel = panel;
        frame.add(panel);
        frame.revalidate();
    }
    
    private void animateDiceRoll(JLabel dice1, JLabel dice2, Runnable onComplete) {
        javax.swing.Timer timer = new javax.swing.Timer(100, null);
        final int[] count = {0};
        
        timer.addActionListener(e -> {
            if (count[0] < 10) {
                if (count[0] % 2 == 0) {
                    dice1.setText("?");
                    dice2.setText("?");
                } else {
                    int random1 = (int)(Math.random() * 6) + 1;
                    int random2 = (int)(Math.random() * 6) + 1;
                    dice1.setText(String.valueOf(random1));
                    dice2.setText(String.valueOf(random2));
                }
                count[0]++;
            } else {
                timer.stop();
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
        timer.start();
    }
    
    private void showDiceRoll(String rollMessage) {
        String[] parts = rollMessage.split(":");
        if (currentPanel.getComponentCount() > 2 && currentPanel.getComponent(2) instanceof JPanel) {
            JPanel dicePanel = (JPanel) currentPanel.getComponent(2);
            if (dicePanel.getComponentCount() >= 2) {
                JLabel dice1 = (JLabel) dicePanel.getComponent(0);
                JLabel dice2 = (JLabel) dicePanel.getComponent(1);
                dice1.setText(parts[0]);
                dice2.setText(parts[1]);
            }
        }
    }
    
    private void showDiceResult(String resultMessage) {
        String[] parts = resultMessage.split(":");
        String title = parts[0].equals("WINNER") ? "Winner!" : "Draw!";
        String message = parts[1] + "\n\nDo you want to play again?";
        showGameResultDialog(title, message);
    }
    
    private void showTurnMessage(String message) {
        if (currentPanel.getComponentCount() > 1 && currentPanel.getComponent(1) instanceof JLabel) {
            ((JLabel) currentPanel.getComponent(1)).setText(message);
        }
    }
    
    private void showWaitMessage(String message) {
        if (currentPanel.getComponentCount() > 1 && currentPanel.getComponent(1) instanceof JLabel) {
            ((JLabel) currentPanel.getComponent(1)).setText(message);
        }
    }
    
    private void showGameResult(String result) {
        String[] parts = result.split(":");
        String title = parts[0].equals("WINNER") ? "Winner!" : "Draw!";
        String message = parts[1] + "\n\nDo you want to play again?";
        showGameResultDialog(title, message);
    }
    
    private void showGameResultDialog(String title, String message) {
        if (!isDialogShowing) {
            isDialogShowing = true;
            int choice = JOptionPane.showOptionDialog(frame, 
                message, 
                title, 
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.QUESTION_MESSAGE, 
                null, 
                new Object[]{"Yes", "No"}, 
                "Yes");
            
            if (choice == JOptionPane.YES_OPTION) {
                out.println("YES");
            } else {
                out.println("NO");
            }
            isDialogShowing = false;
        }
    }
    
    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    class GradientPanel extends JPanel {
        private Color color1;
        private Color color2;
        
        public GradientPanel(Color color1, Color color2) {
            this.color1 = color1;
            this.color2 = color2;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            GradientPaint gp = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}