import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

public class ReactionTimeGame extends JFrame {
    private GamePanel gamePanel;
    
    public ReactionTimeGame() {
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("⚡ Teste de Tempo de Reação ⚡");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        gamePanel = new GamePanel();
        add(gamePanel);
        
        pack();
        setLocationRelativeTo(null);
    }
}

class GamePanel extends JPanel {
    private JLabel instructionLabel;
    private JLabel targetKeyLabel;
    private JLabel timerLabel;
    private JLabel resultLabel;
    private JLabel rankingLabel;
    private JLabel difficultyLabel;
    private JLabel bestScoreLabel;
    private JLabel currentPlayerLabel;
    private JButton startButton;
    private JButton resetRankingButton;
    private JButton changePlayerButton;
    private JComboBox<String> difficultyComboBox;
    
    private String targetKey;
    private long startTime;
    private boolean gameRunning;
    private boolean waitingForStart;
    private javax.swing.Timer gameTimer;
    private javax.swing.Timer timeoutTimer;
    private javax.swing.Timer feedbackTimer;
    private Random random;
    private String currentPlayerName;
    
    // Sistema de ranking
    private ArrayList<PlayerScore> ranking;
    private final int MAX_RANKING_ENTRIES = 10;
    
    // Cores modernas
    private final Color PRIMARY_COLOR = new Color(41, 128, 185);
    private final Color SUCCESS_COLOR = new Color(39, 174, 96);
    private final Color WARNING_COLOR = new Color(243, 156, 18);
    private final Color DANGER_COLOR = new Color(231, 76, 60);
    private final Color DARK_COLOR = new Color(44, 62, 80);
    private final Color LIGHT_COLOR = new Color(236, 240, 241);
    
    // Configurações de dificuldade
    private enum Difficulty {
        FÁCIL("FÁCIL", 3000, 5000, Color.GREEN),
        MÉDIO("MÉDIO", 2000, 4000, Color.ORANGE),
        DIFÍCIL("DIFÍCIL", 1500, 3000, Color.RED);
        
        final String displayName;
        final int maxTime;
        final int timeout;
        final Color color;
        
        Difficulty(String displayName, int maxTime, int timeout, Color color) {
            this.displayName = displayName;
            this.maxTime = maxTime;
            this.timeout = timeout;
            this.color = color;
        }
    }
    
    private Difficulty currentDifficulty;
    
    // Array de teclas disponíveis para o jogo
    private final String[] availableKeys = {
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
        "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
    };
    
    // Classe interna para armazenar os scores dos jogadores
    class PlayerScore implements Comparable<PlayerScore> {
        String playerName;
        long reactionTime;
        Difficulty difficulty;
        Date date;
        
        PlayerScore(String playerName, long reactionTime, Difficulty difficulty) {
            this.playerName = playerName;
            this.reactionTime = reactionTime;
            this.difficulty = difficulty;
            this.date = new Date();
        }
        
        @Override
        public int compareTo(PlayerScore other) {
            return Long.compare(this.reactionTime, other.reactionTime);
        }
        
        @Override
        public String toString() {
            return String.format("%s - %d ms (%s)", playerName, reactionTime, difficulty.displayName);
        }
    }
    
    public GamePanel() {
        ranking = new ArrayList<>();
        currentDifficulty = Difficulty.MÉDIO;
        initializeComponents();
        setupLayout();
        setupEventListeners();
        resetGame();
        updateRankingDisplay();
        askPlayerName(); // Pedir nome apenas uma vez no início
    }
    
    private void initializeComponents() {
        setPreferredSize(new Dimension(700, 600));
        setBackground(LIGHT_COLOR);
        
        // Configurar fontes modernas
        Font titleFont = new Font("Segoe UI", Font.BOLD, 20);
        Font normalFont = new Font("Segoe UI", Font.PLAIN, 14);
        Font boldFont = new Font("Segoe UI", Font.BOLD, 16);
        
        instructionLabel = new JLabel("🎮 Bem-vindo ao Teste de Tempo de Reação!");
        instructionLabel.setFont(titleFont);
        instructionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        instructionLabel.setForeground(DARK_COLOR);
        
        targetKeyLabel = new JLabel("🎯");
        targetKeyLabel.setFont(new Font("Segoe UI", Font.BOLD, 24)); // REDUZIDO: era 72, agora 48
        targetKeyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        targetKeyLabel.setForeground(PRIMARY_COLOR);
        
        timerLabel = new JLabel("00:000");
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        timerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        timerLabel.setForeground(DARK_COLOR);
        timerLabel.setBorder(BorderFactory.createLineBorder(DARK_COLOR, 2, true));
        
        resultLabel = new JLabel("Pressione START para iniciar");
        resultLabel.setFont(boldFont);
        resultLabel.setHorizontalAlignment(SwingConstants.CENTER);
        resultLabel.setForeground(DARK_COLOR);
        
        rankingLabel = new JLabel("");
        rankingLabel.setFont(normalFont);
        rankingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        rankingLabel.setForeground(DARK_COLOR);
        
        bestScoreLabel = new JLabel("Melhor tempo: --");
        bestScoreLabel.setFont(normalFont);
        bestScoreLabel.setHorizontalAlignment(SwingConstants.CENTER);
        bestScoreLabel.setForeground(PRIMARY_COLOR);
        
        currentPlayerLabel = new JLabel("Jogador: --");
        currentPlayerLabel.setFont(boldFont);
        currentPlayerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        currentPlayerLabel.setForeground(PRIMARY_COLOR);
        
        difficultyLabel = new JLabel("🎚️ Dificuldade:");
        difficultyLabel.setFont(boldFont);
        difficultyLabel.setForeground(DARK_COLOR);
        
        String[] difficulties = {"FÁCIL", "MÉDIO", "DIFÍCIL"};
        difficultyComboBox = new JComboBox<>(difficulties);
        difficultyComboBox.setSelectedIndex(1);
        difficultyComboBox.setFont(normalFont);
        difficultyComboBox.setBackground(Color.WHITE);
        
        startButton = new JButton("🚀 START");
        startButton.setFont(boldFont);
        startButton.setBackground(SUCCESS_COLOR);
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        startButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        resetRankingButton = new JButton("🔄 Limpar Ranking");
        resetRankingButton.setFont(normalFont);
        resetRankingButton.setBackground(WARNING_COLOR);
        resetRankingButton.setForeground(Color.WHITE);
        resetRankingButton.setFocusPainted(false);
        
        changePlayerButton = new JButton("👤 Trocar Jogador");
        changePlayerButton.setFont(normalFont);
        changePlayerButton.setBackground(new Color(155, 89, 182));
        changePlayerButton.setForeground(Color.WHITE);
        changePlayerButton.setFocusPainted(false);
        
        random = new Random();
        
        // Configurar timers
        gameTimer = new javax.swing.Timer(10, e -> updateTimer());
        timeoutTimer = new javax.swing.Timer(5000, e -> timeoutGame());
        timeoutTimer.setRepeats(false);
        feedbackTimer = new javax.swing.Timer(2000, e -> clearFeedback());
        feedbackTimer.setRepeats(false);
        
        updateDifficultySettings();
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        
        // Painel superior
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(LIGHT_COLOR);
        topPanel.add(instructionLabel, BorderLayout.CENTER);
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Painel central principal
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBackground(LIGHT_COLOR);
        
        // Painel da tecla alvo com borda
        JPanel targetPanel = new JPanel(new BorderLayout());
        targetPanel.setBackground(LIGHT_COLOR);
        targetPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR, 3, true),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        targetPanel.add(targetKeyLabel, BorderLayout.CENTER);
        
        centerPanel.add(targetPanel, BorderLayout.CENTER);
        
        // Painel do timer
        JPanel timerPanel = new JPanel(new BorderLayout());
        timerPanel.setBackground(LIGHT_COLOR);
        timerPanel.add(timerLabel, BorderLayout.CENTER);
        timerPanel.setBorder(BorderFactory.createEmptyBorder(10, 50, 10, 50));
        
        centerPanel.add(timerPanel, BorderLayout.SOUTH);
        
        // Painel inferior
        JPanel bottomPanel = new JPanel(new GridLayout(6, 1, 8, 8));
        bottomPanel.setBackground(LIGHT_COLOR);
        
        bottomPanel.add(resultLabel);
        bottomPanel.add(currentPlayerLabel);
        bottomPanel.add(bestScoreLabel);
        bottomPanel.add(rankingLabel);
        
        // Painel de controles
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        controlPanel.setBackground(LIGHT_COLOR);
        controlPanel.add(difficultyLabel);
        controlPanel.add(difficultyComboBox);
        controlPanel.add(startButton);
        controlPanel.add(changePlayerButton);
        controlPanel.add(resetRankingButton);
        
        bottomPanel.add(controlPanel);
        
        // Adicionar componentes principais
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void setupEventListeners() {
        startButton.addActionListener(e -> startGame());
        
        resetRankingButton.addActionListener(e -> {
            int response = JOptionPane.showConfirmDialog(
                this,
                "Tem certeza que deseja limpar todo o ranking?",
                "Confirmar Limpeza",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            
            if (response == JOptionPane.YES_OPTION) {
                ranking.clear();
                updateRankingDisplay();
                showFeedback("Ranking limpo com sucesso! ✅", SUCCESS_COLOR);
            }
        });
        
        changePlayerButton.addActionListener(e -> {
            askPlayerName();
        });
        
        difficultyComboBox.addActionListener(e -> {
            String selected = (String) difficultyComboBox.getSelectedItem();
            currentDifficulty = Difficulty.valueOf(selected);
            updateDifficultySettings();
            updateBestScoreDisplay();
        });
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gameRunning && !waitingForStart) {
                    String pressedKey = KeyEvent.getKeyText(e.getKeyCode());
                    checkKeyPress(pressedKey);
                }
            }
        });
    }
    
    private void askPlayerName() {
        String newPlayerName = JOptionPane.showInputDialog(
            this, 
            "👤 Digite o nome do jogador:\n(Máx. 20 caracteres)",
            "Trocar Jogador",
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (newPlayerName != null && !newPlayerName.trim().isEmpty()) {
            currentPlayerName = newPlayerName.trim();
            // Limitar tamanho do nome
            if (currentPlayerName.length() > 20) {
                currentPlayerName = currentPlayerName.substring(0, 20);
            }
            
            currentPlayerLabel.setText("👤 Jogador: " + currentPlayerName);
            instructionLabel.setText("🎮 " + currentPlayerName + ", bem-vindo ao desafio!");
            updateBestScoreDisplay();
            showFeedback("Jogador alterado para: " + currentPlayerName, PRIMARY_COLOR);
        }
    }
    
    private void updateDifficultySettings() {
        timeoutTimer.setDelay(currentDifficulty.timeout);
        difficultyLabel.setForeground(currentDifficulty.color);
    }
    
    private void updateBestScoreDisplay() {
        if (currentPlayerName == null) return;
        
        long bestTime = getBestTimeForPlayer();
        if (bestTime != Long.MAX_VALUE) {
            bestScoreLabel.setText("🏆 Melhor de " + currentPlayerName + ": " + bestTime + " ms (" + currentDifficulty.displayName + ")");
            bestScoreLabel.setForeground(SUCCESS_COLOR);
        } else {
            bestScoreLabel.setText("🎯 " + currentPlayerName + " - Tente conseguir um bom tempo!");
            bestScoreLabel.setForeground(currentDifficulty.color);
        }
    }
    
    private long getBestTimeForPlayer() {
        if (currentPlayerName == null) return Long.MAX_VALUE;
        
        long bestTime = Long.MAX_VALUE;
        for (PlayerScore score : ranking) {
            if (score.playerName.equalsIgnoreCase(currentPlayerName) && score.difficulty == currentDifficulty) {
                bestTime = Math.min(bestTime, score.reactionTime);
            }
        }
        return bestTime == Long.MAX_VALUE ? Long.MAX_VALUE : bestTime;
    }
    
    private void startGame() {
        if (currentPlayerName == null) {
            askPlayerName();
            if (currentPlayerName == null) return;
        }
        
        resetGame();
        
        instructionLabel.setText("🎯 " + currentPlayerName + ", prepare-se...");
        resultLabel.setText("Aguardando tecla alvo...");
        targetKeyLabel.setText("⏳");
        targetKeyLabel.setForeground(WARNING_COLOR);
        startButton.setEnabled(false);
        difficultyComboBox.setEnabled(false);
        resetRankingButton.setEnabled(false);
        changePlayerButton.setEnabled(false);
        
        // Feedback visual de preparação
        
        
        // Esperar 1.5-3.5 segundos aleatórios antes de mostrar a tecla
        int randomDelay = 1500 + random.nextInt(2000);
        
        javax.swing.Timer delayTimer = new javax.swing.Timer(randomDelay, e -> {
            targetKey = availableKeys[random.nextInt(availableKeys.length)];
            targetKeyLabel.setText(targetKey);
            targetKeyLabel.setForeground(PRIMARY_COLOR);
            
            instructionLabel.setText("⚡ " + currentPlayerName + ", PRESSIONE: " + targetKey);
            resultLabel.setText("AGORA!");
            
            startTime = System.currentTimeMillis();
            gameRunning = true;
            waitingForStart = false;
            gameTimer.start();
            timeoutTimer.start();
            
            requestFocusInWindow();
        });
        delayTimer.setRepeats(false);
        delayTimer.start();
    }
    
    
    
    private void resetGame() {
        if (gameTimer.isRunning()) gameTimer.stop();
        if (timeoutTimer.isRunning()) timeoutTimer.stop();
        if (feedbackTimer.isRunning()) feedbackTimer.stop();
        
        gameRunning = false;
        waitingForStart = true;
        startTime = 0L;
        targetKey = "";
        
        timerLabel.setText("00:000");
        timerLabel.setForeground(DARK_COLOR);
        timerLabel.setBorder(BorderFactory.createLineBorder(DARK_COLOR, 2, true));
        
        startButton.setEnabled(true);
        difficultyComboBox.setEnabled(true);
        resetRankingButton.setEnabled(true);
        changePlayerButton.setEnabled(true);
    }
    
    private void updateTimer() {
        if (gameRunning && !waitingForStart) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - startTime;
            
            if (elapsedTime < 0) elapsedTime = 0;
            
            long seconds = elapsedTime / 1000;
            long milliseconds = elapsedTime % 1000;
            
            timerLabel.setText(String.format("%02d:%03d", seconds, milliseconds));
            
            // Feedback visual de tempo
            if (elapsedTime > currentDifficulty.maxTime) {
                timerLabel.setForeground(WARNING_COLOR);
                timerLabel.setBorder(BorderFactory.createLineBorder(WARNING_COLOR, 2, true));
            }
        }
    }
    
    private void timeoutGame() {
        if (gameRunning) {
            gameRunning = false;
            gameTimer.stop();
            
            showFeedback("⏰ TEMPO ESGOTADO! Muito lento!", DANGER_COLOR);
            instructionLabel.setText("😅 " + currentPlayerName + ", tente novamente!");
            targetKeyLabel.setText("💥");
            targetKeyLabel.setForeground(DANGER_COLOR);
            
            enableControls();
        }
    }
    
    private void checkKeyPress(String pressedKey) {
        if (gameRunning && !waitingForStart && pressedKey.equalsIgnoreCase(targetKey)) {
            gameRunning = false;
            gameTimer.stop();
            timeoutTimer.stop();
            
            long endTime = System.currentTimeMillis();
            long reactionTime = endTime - startTime;
            if (reactionTime < 0) reactionTime = 0;
            
            boolean isPersonalBest = addToRanking(reactionTime);
            
            if (reactionTime <= currentDifficulty.timeout) {
                if (isPersonalBest) {
                    showFeedback("🎉 " + currentPlayerName + " - NOVO RECORDE! " + reactionTime + " ms", SUCCESS_COLOR);
                } else if (reactionTime <= 200) {
                    showFeedback("🤯 " + currentPlayerName + " - INCRÍVEL! " + reactionTime + " ms", SUCCESS_COLOR);
                } else if (reactionTime <= 300) {
                    showFeedback("🚀 " + currentPlayerName + " - ÓTIMO! " + reactionTime + " ms", SUCCESS_COLOR);
                } else {
                    showFeedback("✅ " + currentPlayerName + " - " + reactionTime + " ms - Bom trabalho!", SUCCESS_COLOR);
                }
                
                targetKeyLabel.setText("🎯");
                targetKeyLabel.setForeground(SUCCESS_COLOR);
            } else {
                showFeedback("🐌 " + currentPlayerName + " - " + reactionTime + " ms - Fora do tempo limite!", WARNING_COLOR);
                targetKeyLabel.setText("😴");
                targetKeyLabel.setForeground(WARNING_COLOR);
            }
            
            instructionLabel.setText("🎮 " + currentPlayerName + ", pronto para outra?");
            enableControls();
        }
    }
    
    private void showFeedback(String message, Color color) {
        resultLabel.setText(message);
        resultLabel.setForeground(color);
        feedbackTimer.start();
    }
    
    private void clearFeedback() {
        resultLabel.setText("Pressione START para jogar");
        resultLabel.setForeground(DARK_COLOR);
    }
    
    private void enableControls() {
        startButton.setEnabled(true);
        difficultyComboBox.setEnabled(true);
        resetRankingButton.setEnabled(true);
        changePlayerButton.setEnabled(true);
    }
    
    private boolean addToRanking(long reactionTime) {
        boolean isPersonalBest = false;
        PlayerScore existingScore = null;
        
        // Encontrar score existente do mesmo jogador na mesma dificuldade
        for (PlayerScore score : ranking) {
            if (score.playerName.equalsIgnoreCase(currentPlayerName) && score.difficulty == currentDifficulty) {
                existingScore = score;
                break;
            }
        }
        
        if (existingScore != null) {
            // Atualizar se for melhor
            if (reactionTime < existingScore.reactionTime) {
                existingScore.reactionTime = reactionTime;
                existingScore.date = new Date();
                isPersonalBest = true;
            }
        } else {
            // Adicionar novo score
            ranking.add(new PlayerScore(currentPlayerName, reactionTime, currentDifficulty));
            isPersonalBest = true;
        }
        
        Collections.sort(ranking);
        if (ranking.size() > MAX_RANKING_ENTRIES) {
            ranking = new ArrayList<>(ranking.subList(0, MAX_RANKING_ENTRIES));
        }
        
        updateRankingDisplay();
        updateBestScoreDisplay();
        return isPersonalBest;
    }
    
    private void updateRankingDisplay() {
        if (ranking.isEmpty()) {
            rankingLabel.setText("📊 Ranking: (vazio) - Seja o primeiro!");
            return;
        }
        
        StringBuilder rankingText = new StringBuilder("<html><b>🏆 TOP " + MAX_RANKING_ENTRIES + ":</b><br>");
        for (int i = 0; i < Math.min(ranking.size(), 5); i++) {
            PlayerScore score = ranking.get(i);
            String medal = i == 0 ? "🥇" : i == 1 ? "🥈" : i == 2 ? "🥉" : "🔸";
            rankingText.append(String.format("%s %dº - %s<br>", medal, i + 1, score.toString()));
        }
        rankingText.append("</html>");
        rankingLabel.setText(rankingText.toString());
    }
    
    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Gradiente de fundo suave
        Graphics2D g2d = (Graphics2D) g;
        Color color1 = new Color(240, 245, 250);
        Color color2 = new Color(255, 255, 255);
        GradientPaint gp = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2);
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, getWidth(), getHeight());
    }
}