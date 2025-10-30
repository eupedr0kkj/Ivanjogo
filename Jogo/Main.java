import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ReactionTimeGame game = new ReactionTimeGame();
            game.setVisible(true);
        });
    }
}