import java.awt.*;
import java.awt.event.KeyEvent;

/** Draws and controls the recorded-game performance history screen. */
public class PerformanceTracker {
    private final PixelPaws game;

    public PerformanceTracker(PixelPaws game) { this.game = game; }

    /** Adds a completed score to the same history used by the game-over flow. */
    public void recordScore(int score) {
        game.performanceHistory.add(0, new PixelPaws.GameRecord(
                score, game.theme.label, PixelPaws.NAMES[game.selected], game.difficulty.label));
        if (game.performanceHistory.size() > 100) {
            game.performanceHistory.remove(game.performanceHistory.size() - 1);
        }
        game.savePerformance();
        game.repaint();
    }

    public void draw(Graphics2D g) {
        game.drawMenuBg(g);
        game.text(g, "PERFORMANCE TRACKER", PixelPaws.W / 2.0, 48, 34, Color.BLACK);
        game.text(g, "RECORDED RUNS: " + game.performanceHistory.size(), PixelPaws.W / 2.0, 82, 16, new Color(255, 255, 255, 235));
        int headerY = 120;
        g.setColor(new Color(0, 0, 0, 115));
        g.fillRoundRect(35, 99, PixelPaws.W - 70, 40, 12, 12);
        game.text(g, "POINTS", 130, headerY, 15, Color.WHITE);
        game.text(g, "THEME", 330, headerY, 15, Color.WHITE);
        game.text(g, "CHARACTER", 555, headerY, 15, Color.WHITE);
        game.text(g, "SPEED", 760, headerY, 15, Color.WHITE);

        if (game.performanceHistory.isEmpty()) {
            game.text(g, "No completed runs yet. Play a game to start tracking your performance!", PixelPaws.W / 2.0, 245, 18, Color.WHITE);
        } else {
            int visible = 8;
            int start = Math.min(game.trackerOffset, Math.max(0, game.performanceHistory.size() - 1));
            int end = Math.min(game.performanceHistory.size(), start + visible);
            for (int i = start; i < end; i++) {
                PixelPaws.GameRecord record = game.performanceHistory.get(i);
                int y = 162 + (i - start) * 34;
                if ((i - start) % 2 == 0) {
                    g.setColor(new Color(255, 255, 255, 35));
                    g.fillRoundRect(35, y - 15, PixelPaws.W - 70, 30, 8, 8);
                }
                game.text(g, String.valueOf(record.score), 130, y, 15, Color.WHITE);
                game.text(g, fitLabel(record.theme, 17), 330, y, 14, Color.WHITE);
                game.text(g, record.character, 555, y, 14, Color.WHITE);
                game.text(g, record.difficulty, 760, y, 14, Color.WHITE);
            }
            game.text(g, "Showing runs " + (start + 1) + "-" + end + " (newest first)", PixelPaws.W / 2.0, 434, 12, new Color(255, 255, 255, 210));
        }
        game.drawButton(g, game.trackerPrev, "PREVIOUS", PixelPaws.GLASS, 14);
        game.drawButton(g, game.trackerBack, "BACK", PixelPaws.RED, 18);
        game.drawButton(g, game.trackerNext, "NEXT", PixelPaws.GLASS, 14);
        game.text(g, "UP/DOWN: SCROLL   ESC: BACK", PixelPaws.W / 2.0, 504, 12, new Color(255, 255, 255, 220));
    }

    private String fitLabel(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) return value == null ? "" : value;
        return value.substring(0, Math.max(1, maxChars - 3)) + "...";
    }

    public void handleClick(Point p) {
        if (game.trackerBack.contains(p)) game.state = PixelPaws.State.MENU;
        else if (game.trackerPrev.contains(p)) game.trackerOffset = Math.max(0, game.trackerOffset - 8);
        else if (game.trackerNext.contains(p)) game.trackerOffset = Math.min(Math.max(0, game.performanceHistory.size() - 8), game.trackerOffset + 8);
    }

    public void handleKey(int key) {
        if (key == KeyEvent.VK_UP) game.trackerOffset = Math.max(0, game.trackerOffset - 1);
        else if (key == KeyEvent.VK_DOWN) game.trackerOffset = Math.min(Math.max(0, game.performanceHistory.size() - 8), game.trackerOffset + 1);
        else if (key == KeyEvent.VK_PAGE_UP) game.trackerOffset = Math.max(0, game.trackerOffset - 8);
        else if (key == KeyEvent.VK_PAGE_DOWN) game.trackerOffset = Math.min(Math.max(0, game.performanceHistory.size() - 8), game.trackerOffset + 8);
        else if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_BACK_SPACE) game.state = PixelPaws.State.MENU;
    }
}
