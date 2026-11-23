import java.awt.*;
import java.awt.event.KeyEvent;

/** Draws and controls difficulty, music, and volume settings. */
public class Settings {
    private final PixelPaws game;
    private final Rectangle soundToggle = new Rectangle(PixelPaws.W / 2 - 115, 380, 230, 38);
    private final Rectangle volumeTrack = new Rectangle(PixelPaws.W / 2 - 150, 435, 300, 10);

    public Settings(PixelPaws game) { this.game = game; }

    public boolean isSoundEnabled() { return game.musicEnabled; }
    public int getVolume() { return game.musicVolume; }

    public void draw(Graphics2D g) {
        game.drawMenuBg(g);
        game.text(g, "SETTINGS", PixelPaws.W / 2.0, 65, 44, Color.BLACK);
        game.text(g, "CHOOSE YOUR GAME SPEED", PixelPaws.W / 2.0, 110, 18, Color.BLACK);

        boolean classic = game.difficulty == PixelPaws.Difficulty.DEFAULT;
        boolean easy = game.difficulty == PixelPaws.Difficulty.EASY;
        boolean medium = game.difficulty == PixelPaws.Difficulty.MEDIUM;
        boolean hard = game.difficulty == PixelPaws.Difficulty.HARD;
        game.drawButton(g, game.settingsDefault, "DEFAULT", classic ? PixelPaws.GREEN : PixelPaws.GLASS, 19);
        game.drawButton(g, game.settingsEasy, "EASY - 1X", easy ? PixelPaws.GREEN : PixelPaws.GLASS, 19);
        game.drawButton(g, game.settingsMedium, "MEDIUM - 2X", medium ? PixelPaws.GREEN : PixelPaws.GLASS, 18);
        game.drawButton(g, game.settingsHard, "HARD - 3X / 5X", hard ? PixelPaws.GREEN : PixelPaws.GLASS, 16);
        game.text(g, "Original speed curve", game.settingsDefault.getCenterX(), 294, 12, Color.WHITE);
        game.text(g, "Normal speed", game.settingsEasy.getCenterX(), 294, 13, Color.WHITE);
        game.text(g, "Twice the normal speed", game.settingsMedium.getCenterX(), 294, 12, Color.WHITE);
        game.text(g, "3X for 30s, then 5X", game.settingsHard.getCenterX(), 294, 12, Color.WHITE);
        game.text(g, "CURRENT: " + game.difficulty.label, PixelPaws.W / 2.0, 333, 17, Color.WHITE);

        game.drawButton(g, soundToggle, "MUSIC: " + (game.musicEnabled ? "ON" : "OFF"),
                game.musicEnabled ? PixelPaws.GREEN : PixelPaws.RED, 17);
        game.text(g, "VOLUME: " + game.musicVolume + "%", PixelPaws.W / 2.0, 414, 14, Color.WHITE);
        g.setColor(new Color(0, 0, 0, 100));
        g.fillRoundRect(volumeTrack.x, volumeTrack.y, volumeTrack.width, volumeTrack.height, 10, 10);
        g.setColor(new Color(124, 255, 90));
        int fill = (int) (volumeTrack.width * (game.musicVolume / 100.0));
        g.fillRoundRect(volumeTrack.x, volumeTrack.y, fill, volumeTrack.height, 10, 10);
        g.setColor(Color.WHITE);
        int knobX = volumeTrack.x + fill;
        g.fillOval(knobX - 8, volumeTrack.y - 6, 16, 22);
        g.drawOval(knobX - 8, volumeTrack.y - 6, 16, 22);

        game.drawButton(g, game.settingsBack, "BACK", PixelPaws.RED, 21);
        game.text(g, "1-4: SPEED   M: MUSIC   +/-: VOLUME   ESC: BACK", PixelPaws.W / 2.0, 513, 11, new Color(255, 255, 255, 220));
    }

    public void handleClick(Point p) {
        if (game.settingsDefault.contains(p)) game.setDifficulty(PixelPaws.Difficulty.DEFAULT);
        else if (game.settingsEasy.contains(p)) game.setDifficulty(PixelPaws.Difficulty.EASY);
        else if (game.settingsMedium.contains(p)) game.setDifficulty(PixelPaws.Difficulty.MEDIUM);
        else if (game.settingsHard.contains(p)) game.setDifficulty(PixelPaws.Difficulty.HARD);
        else if (soundToggle.contains(p)) game.setMusicEnabled(!game.musicEnabled);
        else if (volumeTrack.contains(p)) updateVolume(p.x);
        else if (game.settingsBack.contains(p)) game.state = PixelPaws.State.MENU;
    }

    public void handleDrag(Point p) {
        if ((p.y >= volumeTrack.y - 10 && p.y <= volumeTrack.y + 20) && p.x >= volumeTrack.x - 8 && p.x <= volumeTrack.x + volumeTrack.width + 8) updateVolume(p.x);
    }

    private void updateVolume(int mouseX) {
        int value = (int) Math.round(100.0 * (mouseX - volumeTrack.x) / volumeTrack.width);
        game.setMusicVolume(value);
    }

    public void handleKey(int key) {
        if (key == KeyEvent.VK_1) game.setDifficulty(PixelPaws.Difficulty.DEFAULT);
        else if (key == KeyEvent.VK_2) game.setDifficulty(PixelPaws.Difficulty.EASY);
        else if (key == KeyEvent.VK_3) game.setDifficulty(PixelPaws.Difficulty.MEDIUM);
        else if (key == KeyEvent.VK_4) game.setDifficulty(PixelPaws.Difficulty.HARD);
        else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) game.setDifficulty(PixelPaws.Difficulty.values()[Math.floorMod(game.difficulty.ordinal() - 1, PixelPaws.Difficulty.values().length)]);
        else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) game.setDifficulty(PixelPaws.Difficulty.values()[(game.difficulty.ordinal() + 1) % PixelPaws.Difficulty.values().length]);
        else if (key == KeyEvent.VK_M) game.setMusicEnabled(!game.musicEnabled);
        else if (key == KeyEvent.VK_PLUS || key == KeyEvent.VK_EQUALS) game.setMusicVolume(game.musicVolume + 5);
        else if (key == KeyEvent.VK_MINUS) game.setMusicVolume(game.musicVolume - 5);
        else if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_BACK_SPACE) game.state = PixelPaws.State.MENU;
    }
}
