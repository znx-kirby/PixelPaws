import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * PIXELPAWS - A Endless Runner Game (offline, no external assets).
 *
 * Compile:  javac PixelPaws.java
 * Run:      java PixelPaws
 *
 * Controls
 *   SPACE ............ jump (or click the mouse during the game)
 *   UP / W ........... climb up to the upper road (hop if already up)
 *   DOWN / S ......... drop down from the upper road / duck on the lower road
 *                      (in the air it makes you fall faster)
 *   P ................ pause
 *   T ................ change theme (or click the CHANGE THEME button)
 *   SETTINGS ......... choose DEFAULT, EASY (1x), MEDIUM (2x), or HARD (3x then 5x)
 *   PERFORMANCE ..... review recent runs: score, theme, character, and difficulty
 *
 * Boosters (rare pick-ups on the roads)
 *   JUMP BOOST ....... much higher jumps for 10 seconds
 *   WINGS ............ fly for 10 seconds (hold SPACE/UP/click to rise, DOWN to dive)
 *   GOD MODE ......... invincible for 15 seconds
 *   DOUBLE POINTS .... double score for 8 seconds
 *   DRAGON BREATH .... automatically destroys obstacles with fire for 10 seconds (rare)
 *   PORTAL ........... enter a 12-second bonus stage full of boosters around 3,000 points
 *   ESC .............. back to menu
 *   LEFT / RIGHT ..... choose Animal on the select screen
 */
public class PixelPaws extends JPanel
        implements ActionListener, KeyListener, MouseListener, MouseMotionListener {

    // ------------------------------------------------------------ constants
    static final int W = 900, H = 520, GROUND = 400;
    static final int UPPER = GROUND - 150;           // height of the upper road
    static final int PX = 110, PW = 58, PH = 66;      // player x, width, height
    static final int GREEN = 0, RED = 1, GLASS = 2, YELLOW = 3, BLUE = 4, GRAY = 5; // button styles
    static final int NUM_THEMES = 23;

    // Booster kinds. Dragon Breath is deliberately NOT part of the guaranteed drop pool.
    static final int JUMP = 0, WINGS = 1, GOD = 2, DOUBLE = 3, DRAGON = 4;
    static final String[] PU_NAMES = {"JUMP BOOST", "WINGS", "GOD MODE", "DOUBLE POINTS", "DRAGON BREATH"};
    static final String[] PU_SHORT = {"JUMP", "WINGS", "GOD", "2X", "FIRE"};
    static final double[] PU_SECONDS = {10, 10, 15, 8, 10};
    static final Color[] PU_COLORS = {
        new Color(0x5CFF7A), new Color(0x6CCBFF), new Color(0xFFD23C),
        new Color(0xCE75FF), new Color(0xFF5722)
    };
    static final double JUMP_MUL = 1.35;              // jump boost: how much stronger the jump is
    static final double CEILING = 78;                 // highest point (feet) the player can reach

    enum State { MENU, SELECT, THEME, SETTINGS, TRACKER, PLAY, OVER }

    enum Difficulty {
        DEFAULT("DEFAULT"), EASY("EASY"), MEDIUM("MEDIUM"), HARD("HARD");
        final String label;
        Difficulty(String label) { this.label = label; }
    }

    static class GameRecord {
        final int score;
        final String theme, character, difficulty;
        GameRecord(int score, String theme, String character, String difficulty) {
            this.score = score;
            this.theme = theme;
            this.character = character;
            this.difficulty = difficulty;
        }
    }

    enum Theme {
        NEON("NEON", 0x090B2B, 0x3D1766, 0x34F5FF, 0x242044),
        DESSERT("DESSERT", 0xFFB66D, 0xFFE7A0, 0xE9C46A, 0xB87942),
        MOON("MOON", 0x050512, 0x29264F, 0xA8A9C7, 0x55566F),
        RAGNAROK("RAGNAROK", 0x481D48, 0xE58B62, 0x6D8B54, 0x4A3A48),
        VOLCANO("VOLCANO", 0x260C16, 0xB83222, 0xFF612E, 0x383039),
        ROOFTOPS("ROOFTOPS", 0x162A50, 0xF28E70, 0x55566D, 0x292D42),
        COUNTRYSIDE("COUNTRYSIDE", 0x9AD9FF, 0xE9F8C9, 0x62B84A, 0x397A36),
        WINTER("WINTER", 0xB9DFF5, 0xF3FAFF, 0xEAF5FF, 0x91B7D0),
        SPRING("SPRING", 0x9DE8D2, 0xFFD9E8, 0x72C85A, 0x3B944D),
        SEASONS("SEASONS MIX", 0x76C9FF, 0xFFD5A1, 0x72C85A, 0x397A36),
        CAVES("CAVES", 0x151B2A, 0x51465E, 0x77717D, 0x34313D),
        ALIEN_PLANET("ALIEN PLANET", 0x160D3D, 0x6D2B83, 0x49D5A5, 0x253D5C),
        JUNKYARD("JUNKYARD", 0x2A2521, 0x594639, 0xB87333, 0x3D3128),
        RAINBOW("RAINBOW", 0xFFBCE6, 0xBCE2FF, 0x93E9BE, 0x51B07F),
        HAUNTED("HAUNTED", 0x0B001A, 0x2B104D, 0x795299, 0x1D1429),
        NORTHPOLE("NORTH POLE", 0x071D33, 0x184B66, 0xEAF7FF, 0x5888A8),
        NUCLEAR_PLANT("NUCLEAR PLANT", 0x111E11, 0x234223, 0x39FF14, 0x1C2E1C),
        ARENA("ARENA", 0x3D2E1E, 0x7A5B3E, 0xD4AF37, 0x4A3828),
        JUNGLE("JUNGLE", 0x0A240F, 0x1B4D24, 0x3DAA4D, 0x133818),
        PROGNOSIN("PROGNOSIN", 0x03131A, 0x0A3A4A, 0x00E5FF, 0x062530),
        BOGLAND("BOGLAND", 0x1A2118, 0x33422E, 0x55734A, 0x222E1F),
        SUPER_HIGHWAY("MEGA HIGHWAY", 0x0F172A, 0x334155, 0xF59E0B, 0x1E293B),
        SUPER_DESERT("MEGA DESERT", 0xD97706, 0xFDE68A, 0x38BDF8, 0x92400E);

        final String label;
        final Color skyTop, skyBottom, groundTop, groundBody;

        Theme(String label, int a, int b, int c, int d) {
            this.label = label;
            skyTop = new Color(a);
            skyBottom = new Color(b);
            groundTop = new Color(c);
            groundBody = new Color(d);
        }
    }

    static final String[] NAMES = {
        "SONIC", "KURAMA", "ANUBIS", "COTTON", "REX", "BRUNO", "PIP",
        "FINN", "OLLY", "SHIVA", "HELIOS", "BENNY", "KAI", "NOVA",
        "STEW", "MILO", "LUNA", "ATLAS", "MONTY"
    };
    static final Color[] BODY = {
        new Color(0x168DE2), new Color(0xE98235), new Color(0x858B91), new Color(0xF4EDE2),
        new Color(0x77746F), new Color(0x875333), new Color(0xA98B70), new Color(0xC86A32),
        new Color(0xEEE7D9), new Color(0xE5E6E8), new Color(0x9B6544), new Color(0x98613A),
        new Color(0x9A8065), new Color(0xD39B45), new Color(0x292B2A), new Color(0xB9A18A),
        new Color(0xB98D58), new Color(0xB6A58A), new Color(0x8D603B)
    };
    static final Color[] FEET = {
        new Color(0xE83B32), new Color(0x66391F), new Color(0x4B4F56), new Color(0xD9CFC2),
        new Color(0x45413E), new Color(0x54321F), new Color(0x8D735B), new Color(0x8D3E20),
        new Color(0xD7CCB8), new Color(0x777D84), new Color(0x4D3A2D), new Color(0x674126),
        new Color(0x5B4B3A), new Color(0x9D6B2D), new Color(0x171918), new Color(0x8C7763),
        new Color(0x8A6745), new Color(0x746653), new Color(0x5E3C27)
    };

    // ------------------------------------------------------------ obstacle
    static class Obstacle {
        final int type, lane;         // type: 0 small rock, 1 big rock pile, 2 spaceship; lane: 0 lower road, 1 upper road
        double x, y, w, h, bob;

        Obstacle(int type, double x, int lane) {
            this.lane = lane;
            double base = (lane == 0) ? GROUND : UPPER;
            this.type = type;
            this.x = x;
            switch (type) {
                case 0:  w = 44; h = 34; y = base - h + 4; break;
                case 1:  w = 78; h = 80; y = base - h + 4; break;
                default: w = 86; h = 34; y = base - 70;      break;   // ship: duck under it or jump over it
            }
        }

        Rectangle2D.Double hitbox() {
            double ix = 8, iy = (type == 2) ? 6 : 8;
            return new Rectangle2D.Double(x + ix, y + bob + iy, w - 2 * ix, h - 2 * iy);
        }
    }

    // ------------------------------------------------------------ booster pick-up
    static class Pickup {
        final int kind, lane;          // kind: any booster; lane: 0 lower road, 1 upper road
        double x, y, bob;              // x, y = centre of the orb
        boolean taken;

        Pickup(int kind, double x, int lane) {
            this.kind = kind;
            this.lane = lane;
            this.x = x;
            this.y = ((lane == 0) ? GROUND : UPPER) - 52;
        }

        Rectangle2D.Double box() {
            return new Rectangle2D.Double(x - 20, y + bob - 20, 40, 40);
        }
    }

    // ------------------------------------------------------------ state
    State state = State.MENU;
    Theme theme = Theme.NEON;
    final Random rnd = new Random();
    final List<Obstacle> obstacles = new ArrayList<>();
    final List<GameRecord> performanceHistory = new ArrayList<>();
    Difficulty difficulty = Difficulty.EASY;
    double gameElapsedMs = 0;
    boolean runRecorded = false;
    int trackerOffset = 0;

    int selected = 0, themeChoice = 0, frame = 0, score = 0, best = 0, banner = 0;   // themeChoice 0-22
    double py = GROUND, vy = 0, speed = 6.5, distance = 0, scroll = 0, nextSpawn = 500, nextSpawnUp = 900;
    boolean onGround = true, ducking = false, downHeld = false, jumpQueued = false, paused = false;
    boolean upper = false, dropping = false, upQueued = false, dropQueued = false;
    Point mouse = new Point(-1, -1);

    // boosters
    final List<Pickup> pickups = new ArrayList<>();
    final double[] effectMs = new double[5];          // booster time left, in milliseconds
    int nextPickupScore = 400;                        // common booster drop every 400 score points
    int nextDragonScore = 900;                        // next random Dragon Breath roll
    int nextPortalScore = 3000;                       // first portal appears around 3,000 points
    double portalX = -1;                              // negative means no portal is on screen
    boolean bonusStage = false;                       // temporary booster dimension entered through a portal
    double bonusStageMs = 0;
    int bonusPickupTick = 0;
    int fireAnim = 0;
    boolean themeMenu = false, skipUpper = false;      // theme picker open / boosted jump passes through the upper road
    boolean holdSpace = false, holdUp = false, holdMouse = false;   // "rise" controls while flying
    String popup = "";
    Color popupColor = Color.WHITE;
    int popupFrames = 0;
    long lastNano = System.nanoTime();
    final Path bestFile = Paths.get(System.getProperty("user.home"), ".pixelpaws_best");
    final Path performanceFile = Paths.get(System.getProperty("user.home"), ".pixelpaws_performance");
    final Path settingsFile = Paths.get(System.getProperty("user.home"), ".pixelpaws_settings");

    // ------------------------------------------------------------ buttons
    final Rectangle menuStart    = new Rectangle(W / 2 - 170, 164, 340, 48);
    final Rectangle menuStage    = new Rectangle(W / 2 - 170, 220, 340, 48);
    final Rectangle menuTracker  = new Rectangle(W / 2 - 170, 276, 340, 48);
    final Rectangle menuSettings = new Rectangle(W / 2 - 170, 332, 340, 48);
    final Rectangle menuExit     = new Rectangle(W / 2 - 170, 388, 340, 48);

    final Rectangle settingsDefault = new Rectangle(28, 190, 198, 92);
    final Rectangle settingsEasy    = new Rectangle(246, 190, 198, 92);
    final Rectangle settingsMedium  = new Rectangle(464, 190, 198, 92);
    final Rectangle settingsHard    = new Rectangle(682, 190, 190, 92);
    final Rectangle settingsBack   = new Rectangle(W / 2 - 100, 390, 200, 52);
    final Rectangle trackerPrev    = new Rectangle(235, 452, 120, 38);
    final Rectangle trackerBack    = new Rectangle(W / 2 - 80, 452, 160, 38);
    final Rectangle trackerNext    = new Rectangle(545, 452, 120, 38);
    final Rectangle selBox    = new Rectangle(W / 2 - 190, 310, 380, 58);
    final Rectangle selStart  = new Rectangle(W / 2 - 140, 385, 280, 58);
    final Rectangle selBack   = new Rectangle(W / 2 - 80, 458, 160, 40);
    final Rectangle selLeft   = new Rectangle(115, 175, 70, 70);
    final Rectangle selRight  = new Rectangle(W - 185, 175, 70, 70);
    int selectionDragStartX = -1;                                            // where a drag on the select screen started (-1 = none)
    final Rectangle overTitle = new Rectangle(W / 2 - 120, 120, 240, 50);
    final Rectangle overScore = new Rectangle(W / 2 - 160, 190, 320, 66);
    final Rectangle overAgain = new Rectangle(W / 2 - 120, 330, 240, 52);
    final Rectangle overMenu  = new Rectangle(W / 2 - 120, 395, 240, 52);
    final Rectangle themeBox   = new Rectangle(W / 2 - 190, 365, 380, 40);
    final Rectangle themeStart = new Rectangle(W / 2 - 140, 415, 280, 52);
    final Rectangle themeBack  = new Rectangle(W / 2 - 80, 472, 160, 36);
    final Rectangle changeBtn  = new Rectangle(W / 2 - 95, 18, 190, 40);      // in-game CHANGE THEME button
    final Rectangle menuResume = new Rectangle(W / 2 - 110, 385, 220, 48);

    // ------------------------------------------------------------ setup
    // Builds the panel: registers input listeners, loads saved data, and starts
    // the game timer that runs update() + repaint() about 60 times per second.
    public PixelPaws() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        loadBest();
        loadPerformance();
        loadSettings();
        new javax.swing.Timer(16, this).start();   // ~60 FPS
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("PixelPaws - A Endless Runner Game");
            PixelPaws p = new PixelPaws();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(p);
            f.pack();
            f.setResizable(false);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            p.requestFocusInWindow();
        });
    }

    void loadBest() {
        try {
            if (Files.exists(bestFile)) best = Integer.parseInt(new String(Files.readAllBytes(bestFile)).trim());
        } catch (IOException | NumberFormatException ignored) { }
    }

    void saveBest() {
        try {
            Files.write(bestFile, String.valueOf(best).getBytes());
        } catch (IOException ignored) { }
    }

    void loadSettings() {
        try {
            if (Files.exists(settingsFile)) {
                String saved = new String(Files.readAllBytes(settingsFile), StandardCharsets.UTF_8).trim();
                difficulty = Difficulty.valueOf(saved);
            }
        } catch (IOException | IllegalArgumentException ignored) { }
    }

    void saveSettings() {
        try {
            Files.write(settingsFile, difficulty.name().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) { }
    }

    void setDifficulty(Difficulty next) {
        difficulty = next;
        saveSettings();
    }

    int speedMultiplier() {
        if (difficulty == Difficulty.MEDIUM) return 2;
        if (difficulty == Difficulty.HARD) return gameElapsedMs < 30000 ? 3 : 5;
        // DEFAULT restores the original game's adaptive speed curve; EASY is also 1x.
        return 1;
    }

    void loadPerformance() {
        try {
            if (!Files.exists(performanceFile)) return;
            for (String line : Files.readAllLines(performanceFile, StandardCharsets.UTF_8)) {
                String[] parts = line.split("\\|", -1);
                if (parts.length != 4) continue;
                try {
                    performanceHistory.add(new GameRecord(Integer.parseInt(parts[0]), parts[1], parts[2], parts[3]));
                } catch (NumberFormatException ignored) { }
                if (performanceHistory.size() >= 100) break;
            }
        } catch (IOException ignored) { }
    }

    void savePerformance() {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < Math.min(100, performanceHistory.size()); i++) {
            GameRecord r = performanceHistory.get(i);
            lines.add(r.score + "|" + r.theme.replace("|", "/") + "|"
                    + r.character.replace("|", "/") + "|" + r.difficulty.replace("|", "/"));
        }
        try {
            Files.write(performanceFile, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) { }
    }

    void recordRun() {
        if (runRecorded) return;
        runRecorded = true;
        if (score > best) { best = score; saveBest(); }
        performanceHistory.add(0, new GameRecord(score, theme.label, NAMES[selected], difficulty.label));
        while (performanceHistory.size() > 100) performanceHistory.remove(performanceHistory.size() - 1);
        trackerOffset = 0;
        savePerformance();
    }

    void startGame() {
        obstacles.clear();
        pickups.clear();
        java.util.Arrays.fill(effectMs, 0);
        py = GROUND; vy = 0; onGround = true; ducking = false;
        downHeld = false; jumpQueued = false; paused = false;
        holdSpace = false; holdUp = false; holdMouse = false;
        skipUpper = false; themeMenu = false;
        distance = 0; score = 0; speed = 6.5; scroll = 0;
        gameElapsedMs = 0; runRecorded = false;
        nextSpawn = 500; nextSpawnUp = 900; banner = 0;
        nextPickupScore = 400; popupFrames = 0;
        nextDragonScore = 650 + rnd.nextInt(650);
        nextPortalScore = 3000 + rnd.nextInt(501);
        portalX = -1; bonusStage = false; bonusStageMs = 0; bonusPickupTick = 0; fireAnim = 0;
        upper = false; dropping = false; upQueued = false; dropQueued = false;
        theme = Theme.values()[themeChoice];
        lastNano = System.nanoTime();
        state = State.PLAY;
    }

    void die() {
        recordRun();
        state = State.OVER;
        if (score > best) { best = score; saveBest(); }
    }

    // ------------------------------------------------------------ game logic
    @Override public void actionPerformed(ActionEvent e) {
        update();
        repaint();
    }

    void update() {
        frame++;
        if (banner > 0) banner--;
        if (popupFrames > 0) popupFrames--;
        long now = System.nanoTime();
        double dt = Math.min(50.0, (now - lastNano) / 1e6);   // real milliseconds since the last tick
        lastNano = now;
        if (state != State.PLAY || paused || themeMenu) return;

        gameElapsedMs += dt;
        double normalSpeed = Math.min(14, 6.5 + score / 180.0);
        speed = normalSpeed * speedMultiplier();
        // DOUBLE POINTS doubles score progression on top of the selected game speed.
        distance += speed * (effectMs[DOUBLE] > 0 ? 2.0 : 1.0);
        scroll += speed;
        score = (int) (distance / 12);
        fireAnim++;

        // Booster timers
        for (int i = 0; i < effectMs.length; i++) {
            if (effectMs[i] <= 0) continue;
            effectMs[i] -= dt;
            if (effectMs[i] <= 0) {
                effectMs[i] = 0;
                if (i == WINGS) endWings();
            }
        }
        boolean flying = effectMs[WINGS] > 0, god = effectMs[GOD] > 0,
                boost = effectMs[JUMP] > 0, dragon = effectMs[DRAGON] > 0;

        // player physics
        if (flying) {                                   // WINGS: hold SPACE / UP / click to rise, DOWN to dive
            if (holdSpace || holdUp || holdMouse) vy -= 1.0;
            else if (downHeld) vy += 1.0;
            else vy *= 0.85;
            vy = Math.max(-7, Math.min(7, vy));
            py += vy;
            if (py < CEILING) { py = CEILING; if (vy < 0) vy = 0; }
            if (py > GROUND) { py = GROUND; if (vy > 0) vy = 0; }
            onGround = false; dropping = false; skipUpper = false; ducking = false;
            jumpQueued = false; upQueued = false; dropQueued = false;
        } else {
            if (dropQueued && onGround && upper) {     // DOWN: drop from the upper road to the lower road
                upper = false; dropping = true; onGround = false; vy = 3;
            }
            if (jumpQueued && onGround) {              // JUMP BOOST makes this jump much higher
                vy = boost ? -15.5 * JUMP_MUL : -15.5;
                skipUpper = boost && !upper;           // a boosted jump flies past the upper road instead of landing on it
                onGround = false;
            }
            if (upQueued && onGround) {                // UP: climb to the upper road (hop if already there)
                vy = upper ? (boost ? -15.5 * JUMP_MUL : -15.5) : -18.5;
                skipUpper = false;
                onGround = false;
            }
            jumpQueued = false; upQueued = false; dropQueued = false;
            if (!onGround) {
                double prev = py;
                vy += downHeld ? 2.2 : 0.85;
                py += vy;
                if (py < CEILING) { py = CEILING; if (vy < 0) vy = 0; }
                if (vy > 0 && !dropping && !skipUpper && prev <= UPPER && py >= UPPER) {   // land on the upper road
                    py = UPPER; vy = 0; onGround = true; upper = true;
                } else if (py >= GROUND) {                                                  // land on the lower road
                    py = GROUND; vy = 0; onGround = true; upper = false; dropping = false; skipUpper = false;
                }
                if (dropping && py > UPPER + 24) dropping = false;
            }
            ducking = downHeld && onGround && !upper;
        }

        // Portals appear randomly once the score reaches the 3,000-point range.
        // Missing a portal is okay: another one can appear later in the run.
        if (!bonusStage && portalX < 0 && score >= nextPortalScore) {
            portalX = W + 70;
            nextPortalScore = score + 2500 + rnd.nextInt(2001);
        }
        if (!bonusStage && portalX >= 0) {
            portalX -= speed;
            if (portalX + 76 < -10) {
                portalX = -1;
            } else if (portalX < PX + PW - 5 && portalX + 76 > PX + 8) {
                enterBonusStage();
            }
        }

        Rectangle2D.Double me = playerBox();
        int myLane = upper ? 1 : 0;

        if (!bonusStage) {
            // Normal stage obstacles.
            nextSpawn -= speed;
            if (nextSpawn <= 0) spawn(0);
            nextSpawnUp -= speed;
            if (nextSpawnUp <= 0) spawn(1);

            for (Obstacle o : obstacles) {
                o.x -= (o.type == 2) ? speed + 0.8 : speed;
                if (o.type == 2) o.bob = Math.sin(frame * 0.12) * 3;
                // Dragon Breath rapidly incinerates obstacles in front of the character.
                if (dragon && o.x < PX + 490 && o.x + o.w > PX + 25
                        && (flying || o.lane == myLane)) {
                    o.x = -1000;
                    continue;
                }
                // GOD MODE ignores every hit; while flying you can hit things on either road.
                if (!god && (flying || o.lane == myLane) && o.hitbox().intersects(me)) {
                    die(); return;
                }
            }
            obstacles.removeIf(o -> o.x + o.w < -30);
        } else {
            // The bonus dimension is safe: no obstacles, only lots of booster pickups.
            obstacles.clear();
            bonusStageMs -= dt;
            bonusPickupTick++;
            if (bonusPickupTick >= 18) {
                bonusPickupTick = 0;
                spawnBonusPickups();
            }
            if (bonusStageMs <= 0) leaveBonusStage();
        }

        // The regular pool (including DOUBLE POINTS) is awarded at each 400-point milestone.
        // Use while so boosted scoring never skips a milestone.
        while (score >= nextPickupScore) {
            if (!bonusStage) spawnPickup();
            nextPickupScore += 400;
        }

        // Dragon Breath is a separate random, rare roll; it is never guaranteed by milestones.
        if (score >= nextDragonScore) {
            if (rnd.nextDouble() < 0.24) spawnDragonPickup();
            nextDragonScore = score + 450 + rnd.nextInt(850);
        }

        for (Pickup pk : pickups) {
            pk.x -= speed;
            pk.bob = Math.sin(frame * 0.1 + pk.kind * 2) * 5;
            if (pk.box().intersects(me)) { pk.taken = true; collect(pk); }
        }
        pickups.removeIf(pk -> pk.taken || pk.x < -40);
    }

    Rectangle2D.Double playerBox() {
        double h = ducking ? PH * 0.58 : PH;
        return new Rectangle2D.Double(PX + 12, py - h + (ducking ? 6 : 8), PW - 24, h - (ducking ? 8 : 10));
    }

    void spawn(int lane) {
        double x = W + 40;
        for (Pickup pk : pickups) {                                          // keep rocks away from a booster
            if (pk.lane == lane && Math.abs(pk.x - x) < 150) {
                if (lane == 0) nextSpawn = 70; else nextSpawnUp = 70;
                return;
            }
        }
        double r = rnd.nextDouble();
        if (score >= 100 && r < 0.25) {
            obstacles.add(new Obstacle(2, x, lane));                        // flying spaceship
        } else if (r < 0.65) {
            obstacles.add(new Obstacle(0, x, lane));                        // small rock
            if (rnd.nextDouble() < 0.3) obstacles.add(new Obstacle(0, x + 52, lane));
        } else {
            obstacles.add(new Obstacle(1, x, lane));                        // big rock pile
        }
        if (lane == 0) nextSpawn = speed * 44 + rnd.nextDouble() * speed * 30;     // always a jumpable gap
        else nextSpawnUp = speed * 52 + rnd.nextDouble() * speed * 40;
    }

    // ---- boosters
    void spawnPickup() {
        int lane = rnd.nextInt(2);
        double x = W + 110;
        for (Obstacle o : obstacles) {
            if (o.lane == lane && o.x < x + 190 && o.x + o.w > x - 190) {
                lane = 1 - lane; // use the other road if the first lane is crowded
                break;
            }
        }
        // Common milestones can grant the three original boosters or DOUBLE POINTS.
        pickups.add(new Pickup(rnd.nextInt(4), x, lane));
    }

    void spawnDragonPickup() {
        int lane = rnd.nextInt(2);
        double x = W + 105 + rnd.nextInt(100);
        for (Obstacle o : obstacles) {
            if (o.lane == lane && o.x < x + 200 && o.x + o.w > x - 120) {
                lane = 1 - lane;
                break;
            }
        }
        pickups.add(new Pickup(DRAGON, x, lane));
    }

    void spawnBonusPickups() {
        // Two or three drops per burst makes the temporary stage feel packed with rewards.
        int count = 2 + rnd.nextInt(2);
        for (int i = 0; i < count; i++) {
            int lane = rnd.nextInt(2);
            int kind = rnd.nextInt(100) < 7 ? DRAGON : rnd.nextInt(4);
            double x = W + 35 + i * 70 + rnd.nextInt(65);
            pickups.add(new Pickup(kind, x, lane));
        }
    }

    void enterBonusStage() {
        bonusStage = true;
        bonusStageMs = 12000; // 12 seconds in the booster dimension
        bonusPickupTick = 0;
        portalX = -1;
        obstacles.clear();
        pickups.clear();
        nextSpawn = speed * 45;
        nextSpawnUp = speed * 60;
        popup = "PORTAL! BOOSTER DIMENSION!";
        popupColor = new Color(0xFF6BFF);
        popupFrames = 120;
    }

    void leaveBonusStage() {
        bonusStage = false;
        bonusStageMs = 0;
        pickups.clear();
        nextSpawn = speed * 28;
        nextSpawnUp = speed * 48;
        popup = "BONUS STAGE COMPLETE!";
        popupColor = new Color(0x7CFF5A);
        popupFrames = 100;
    }

    void collect(Pickup pk) {
        effectMs[pk.kind] = PU_SECONDS[pk.kind] * 1000; // collecting the same booster refreshes its timer
        popup = PU_NAMES[pk.kind] + "!  " + (int) PU_SECONDS[pk.kind] + " SECONDS";
        popupColor = PU_COLORS[pk.kind];
        popupFrames = 100;
        if (pk.kind == WINGS) { onGround = false; dropping = false; skipUpper = false; vy = -6; }
        if (pk.kind == DOUBLE) popup = "DOUBLE POINTS!  8 SECONDS";
        if (pk.kind == DRAGON) popup = "LEGENDARY DRAGON BREATH!  10 SECONDS";
    }

    void endWings() {                                   // wings ran out: fall back down onto a road
        onGround = false; vy = 0; dropping = false; skipUpper = false;
        upper = py <= UPPER;
    }

    // ---- in-game theme picker
    void openThemeMenu() {
        themeMenu = true;
        themeChoice = theme.ordinal();
        downHeld = false; holdSpace = false; holdUp = false; holdMouse = false;
    }

    void closeThemeMenu() {
        themeMenu = false;
        themeChoice = theme.ordinal();
        lastNano = System.nanoTime();
    }

    void applyTheme(int i) {
        Theme t = Theme.values()[i];
        if (t != theme) { theme = t; banner = 120; }
        closeThemeMenu();
    }

    void themeMenuKey(int k) {
        if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) themeChoice = (themeChoice + NUM_THEMES - 1) % NUM_THEMES;
        else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) themeChoice = (themeChoice + 1) % NUM_THEMES;
        else if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_9) themeChoice = Math.min(k - KeyEvent.VK_1, NUM_THEMES - 1);
        else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) applyTheme(themeChoice);
        else if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_T) closeThemeMenu();
    }

    // ------------------------------------------------------------ painting
    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        switch (state) {
            case MENU:     drawMenu(g);       break;
            case SELECT:   drawSelect(g);     break;
            case THEME:    drawThemeSelect(g);break;
            case SETTINGS: drawSettings(g);   break;
            case TRACKER:  drawTracker(g);    break;
            default:       drawGame(g);       break;
        }
        Toolkit.getDefaultToolkit().sync();
    }

    // ---- menu screens
    void drawMenuBg(Graphics2D g) {
        g.setPaint(new GradientPaint(0, H, new Color(0x2FC4C9), W, 0, new Color(0x3A1C7A)));
        g.fillRect(0, 0, W, H);
    }

    void drawMenu(Graphics2D g) {
        drawMenuBg(g);
        text(g, "PIXELPAWS", W / 2.0, 76, 68, Color.BLACK);
        text(g, "A ENDLESS RUNNER GAME", W / 2.0, 126, 19, Color.BLACK);
        drawButton(g, menuStart, "START", GLASS, 29);
        drawButton(g, menuStage, "STAGES", YELLOW, 26);
        drawButton(g, menuTracker, "PERFORMANCE TRACKER", BLUE, 23);
        drawButton(g, menuSettings, "SETTINGS", GRAY, 25);
        drawButton(g, menuExit, "EXIT", RED, 27);
        drawPlayer(g, 110, H - 30 - Math.abs(Math.sin(frame * 0.1)) * 14, 1.05, selected, true, false, frame);
        text(g, "SETTINGS: S     PERFORMANCE: P     STAGES: T", W / 2.0, H - 16, 13, new Color(255, 255, 255, 220));
    }

    void drawSettings(Graphics2D g) {
        drawMenuBg(g);
        text(g, "SETTINGS", W / 2.0, 85, 48, Color.BLACK);
        text(g, "CHOOSE YOUR GAME SPEED", W / 2.0, 132, 20, Color.BLACK);

        boolean classic = difficulty == Difficulty.DEFAULT;
        boolean easy = difficulty == Difficulty.EASY;
        boolean medium = difficulty == Difficulty.MEDIUM;
        boolean hard = difficulty == Difficulty.HARD;
        drawButton(g, settingsDefault, "DEFAULT", classic ? GREEN : GLASS, 19);
        drawButton(g, settingsEasy, "EASY - 1X", easy ? GREEN : GLASS, 19);
        drawButton(g, settingsMedium, "MEDIUM - 2X", medium ? GREEN : GLASS, 18);
        drawButton(g, settingsHard, "HARD - 3X / 5X", hard ? GREEN : GLASS, 16);
        text(g, "Original speed curve", settingsDefault.getCenterX(), 306, 12, Color.WHITE);
        text(g, "Normal speed", settingsEasy.getCenterX(), 306, 13, Color.WHITE);
        text(g, "Twice the normal speed", settingsMedium.getCenterX(), 306, 12, Color.WHITE);
        text(g, "3X for 30s, then 5X", settingsHard.getCenterX(), 306, 12, Color.WHITE);
        text(g, "Current: " + difficulty.label, W / 2.0, 352, 19, new Color(255, 255, 255, 235));
        drawButton(g, settingsBack, "BACK", RED, 23);
        text(g, "CLICK A SPEED OR PRESS 1, 2, 3, OR 4", W / 2.0, 476, 13, new Color(255, 255, 255, 220));
    }

    void drawTracker(Graphics2D g) {
        drawMenuBg(g);
        text(g, "PERFORMANCE TRACKER", W / 2.0, 48, 34, Color.BLACK);
        text(g, "RECORDED RUNS: " + performanceHistory.size(), W / 2.0, 82, 16, new Color(255, 255, 255, 235));

        int headerY = 120;
        g.setColor(new Color(0, 0, 0, 115));
        g.fillRoundRect(35, 99, W - 70, 40, 12, 12);
        text(g, "POINTS", 130, headerY, 15, Color.WHITE);
        text(g, "THEME", 330, headerY, 15, Color.WHITE);
        text(g, "CHARACTER", 555, headerY, 15, Color.WHITE);
        text(g, "SPEED", 760, headerY, 15, Color.WHITE);

        if (performanceHistory.isEmpty()) {
            text(g, "No completed runs yet. Play a game to start tracking your performance!", W / 2.0, 245, 18, Color.WHITE);
        } else {
            int visible = 8;
            int start = Math.min(trackerOffset, Math.max(0, performanceHistory.size() - 1));
            int end = Math.min(performanceHistory.size(), start + visible);
            for (int i = start; i < end; i++) {
                GameRecord r = performanceHistory.get(i);
                int y = 162 + (i - start) * 34;
                if ((i - start) % 2 == 0) {
                    g.setColor(new Color(255, 255, 255, 35));
                    g.fillRoundRect(35, y - 15, W - 70, 30, 8, 8);
                }
                text(g, String.valueOf(r.score), 130, y, 15, Color.WHITE);
                text(g, fitLabel(r.theme, 17), 330, y, 14, Color.WHITE);
                text(g, r.character, 555, y, 14, Color.WHITE);
                text(g, r.difficulty, 760, y, 14, Color.WHITE);
            }
            text(g, "Showing runs " + (start + 1) + "-" + end + " (newest first)", W / 2.0, 434, 12, new Color(255, 255, 255, 210));
        }
        drawButton(g, trackerPrev, "PREVIOUS", GLASS, 14);
        drawButton(g, trackerBack, "BACK", RED, 18);
        drawButton(g, trackerNext, "NEXT", GLASS, 14);
        text(g, "UP/DOWN: SCROLL   ESC: BACK", W / 2.0, 504, 12, new Color(255, 255, 255, 220));
    }

    String fitLabel(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) return value == null ? "" : value;
        return value.substring(0, Math.max(1, maxChars - 3)) + "...";
    }

    double charX(int i) { return W / 2.0 + (i - selected) * 130; }

    void drawSelect(Graphics2D g) {
        drawMenuBg(g);
        text(g, "SELECT YOUR PAWS:", W / 2.0, 62, 30, Color.BLACK);

        // A clean single-character carousel keeps the selection screen uncluttered.
        g.setColor(new Color(0, 0, 0, 35));
        g.fillRoundRect(205, 105, W - 410, 175, 26, 26);
        g.setColor(new Color(255, 255, 255, 55));
        g.drawRoundRect(205, 105, W - 410, 175, 26, 26);

        drawButton(g, selLeft, "<", GLASS, 42);
        drawButton(g, selRight, ">", GREEN, 42);
        double bounce = Math.abs(Math.sin(frame * 0.12)) * 7;
        g.setColor(new Color(124, 255, 90, 90));
        g.fill(new Ellipse2D.Double(W / 2.0 - 72, 260, 144, 20));
        drawPlayer(g, W / 2.0, 252 - bounce, 1.65, selected, false, false, frame);
        text(g, (selected + 1) + " / " + NAMES.length, W / 2.0, 292, 13, new Color(255, 255, 255, 225));

        drawButton(g, selBox, "SELECTED: " + NAMES[selected], GLASS, 22);
        drawButton(g, selStart, "STAGE SELECT", GREEN, 27);
        drawButton(g, selBack, "BACK", RED, 21);
        text(g, "TAP < OR > TO BROWSE PAWS  |  USE LEFT / RIGHT KEYS", W / 2.0, 510, 12, new Color(255, 255, 255, 220));
    }

    // ---- theme selection screen
    Rectangle cardRect(int i) {
        int cols = 6;
        int cardW = 135, cardH = 68;
        int gapX = 12, gapY = 10;
        int row = i / cols;
        int col = i % cols;
        int startX = 14;
        int startY = 52;
        return new Rectangle(startX + col * (cardW + gapX), startY + row * (cardH + gapY), cardW, cardH);
    }

    void drawThemeCard(Graphics2D g, int i, boolean sel) {
        Theme t = Theme.values()[i];
        Rectangle r = cardRect(i);
        boolean hover = r.contains(mouse);
        if (sel || hover) {
            for (int k = 5; k >= 1; k--) {
                g.setColor(new Color(124, 255, 90, sel ? 30 : 16));
                g.setStroke(new BasicStroke(k * 2f));
                g.drawRoundRect(r.x, r.y, r.width, r.height, 16, 16);
            }
        }
        Shape oldClip = g.getClip();
        g.setClip(new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 16, 16));
        g.setPaint(new GradientPaint(0, r.y, t.skyTop, 0, r.y + 44, t.skyBottom));
        g.fillRect(r.x, r.y, r.width, 44);
        g.setColor(t.groundTop);
        g.fillRect(r.x, r.y + 44, r.width, 6);
        g.setColor(t.groundBody);
        g.fillRect(r.x, r.y + 50, r.width, 18);
        g.setClip(oldClip);
        drawPlayer(g, r.x + 20, r.y + 48, 0.30, selected, false, false, frame);
        g.setStroke(new BasicStroke(sel ? 3f : 1.5f));
        g.setColor(sel ? new Color(0x7CFF5A) : new Color(255, 255, 255, 170));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 16, 16);
        g.setStroke(new BasicStroke(1f));
        text(g, t.label, r.x + r.width * 0.58, r.y + 56, t.label.length() > 11 ? 9 : 11, Color.WHITE);
        text(g, String.valueOf(i + 1), r.x + 12, r.y + 12, 10, new Color(0, 0, 0, 150));
        text(g, String.valueOf(i + 1), r.x + 11, r.y + 11, 10, Color.WHITE);
    }

    void drawThemeSelect(Graphics2D g) {
        drawMenuBg(g);
        text(g, "SELECT YOUR STAGE:", W / 2.0, 30, 24, Color.BLACK);
        for (int i = 0; i < NUM_THEMES; i++) drawThemeCard(g, i, themeChoice == i);
        drawButton(g, themeBox, "SELECTED: " + Theme.values()[themeChoice].label, GLASS, 20);
        drawButton(g, themeStart, "START", GREEN, 36);
        drawButton(g, themeBack, "BACK", RED, 20);
        text(g, "CLICK A STAGE OR USE ARROW KEYS", W / 2.0, H - 8, 11, new Color(255, 255, 255, 200));
    }

    void drawThemeMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 175));
        g.fillRect(0, 0, W, H);
        text(g, "CHOOSE YOUR STAGE", W / 2.0, 30, 24, Color.WHITE);
        for (int i = 0; i < NUM_THEMES; i++) drawThemeCard(g, i, themeChoice == i);
        drawButton(g, menuResume, "RESUME", GREEN, 26);
        text(g, "CLICK A STAGE OR USE ARROW KEYS   ESC: RESUME", W / 2.0, H - 8, 12, new Color(255, 255, 255, 210));
    }

    // ---- game screen
    void drawGame(Graphics2D g) {
        boolean flying = effectMs[WINGS] > 0;
        double drawY = py + (flying ? Math.sin(frame * 0.15) * 3 : 0);
        if (bonusStage) drawBonusBackground(g); else drawBackground(g);
        drawGround(g);
        drawUpperRoad(g);
        if (portalX >= 0 && !bonusStage) drawPortal(g, portalX);
        for (Obstacle o : obstacles) drawObstacle(g, o);
        for (Pickup pk : pickups) drawPickup(g, pk);
        drawEffects(g, PX + PW / 2.0, drawY);
        drawPlayer(g, PX + PW / 2.0, drawY, 1.0, selected, state == State.PLAY && onGround && !paused, ducking, frame);
        if (effectMs[DRAGON] > 0) drawDragonBreath(g, drawY);

        if (banner > 100) {
            float a = (banner - 100) / 20f * 0.7f;
            g.setColor(new Color(1f, 1f, 1f, Math.min(1f, a)));
            g.fillRect(0, 0, W, H);
        }
        if (banner > 0) {
            float a = Math.min(1f, banner / 30f);
            text(g, "THEME: " + theme.label, W / 2.0 + 2, 112, 34, new Color(0f, 0f, 0f, a * 0.6f));
            text(g, "THEME: " + theme.label, W / 2.0, 110, 34, new Color(1f, 1f, 1f, a));
        }

        if (state == State.PLAY && themeMenu) {
            drawThemeMenu(g);
        } else if (state == State.PLAY) {
            drawButton(g, new Rectangle(24, 18, 170, 40), "PIXELPAWS", GREEN, 20);
            drawButton(g, new Rectangle(W - 194, 18, 170, 40), "SCORE: " + score, RED, 20);
            text(g, "BEST: " + best, W - 109, 74, 14, Color.WHITE);
            drawButton(g, changeBtn, "CHANGE THEME", GLASS, 18);
            text(g, bonusStage ? "BOOSTER DIMENSION" : theme.label, changeBtn.getCenterX(), 74, 14,
                    bonusStage ? new Color(0xFFB7FF) : Color.WHITE);
            text(g, difficulty.label + "  " + speedMultiplier() + "X", 305, 74, 14, Color.WHITE);
            if (bonusStage) {
                text(g, "BONUS STAGE: " + (int) Math.ceil(bonusStageMs / 1000.0) + "s LEFT",
                        W / 2.0, 142, 25, new Color(0xFFB7FF));
            }
            if (!bonusStage && portalX >= 0) {
                text(g, "PORTAL APPROACHING!", W / 2.0, 142, 20, new Color(0xA8F8FF));
            }
            drawEffectHud(g);
            if (popupFrames > 0) {
                float a = Math.min(1f, popupFrames / 25f);
                double rise = (100 - popupFrames) * 0.25;
                text(g, popup, W / 2.0 + 2, 175 - rise + 2, 30, new Color(0f, 0f, 0f, a * 0.6f));
                text(g, popup, W / 2.0, 175 - rise, 30, alpha(popupColor, (int) (255 * a)));
            }
            if (score < 80) text(g, "SPACE: JUMP   UP: CLIMB   DOWN: DROP / DUCK   T: THEME", W / 2.0, H - 20, 14, new Color(255, 255, 255, 230));
            if (paused) {
                g.setColor(new Color(0, 0, 0, 120));
                g.fillRect(0, 0, W, H);
                text(g, "PAUSED", W / 2.0, H / 2.0, 64, Color.WHITE);
            }
        } else {
            g.setColor(new Color(0, 0, 0, 100));
            g.fillRect(0, 0, W, H);
            drawButton(g, overTitle, "GAME OVER", RED, 26);
            drawButton(g, overScore, "SCORE: " + score, GREEN, 36);
            text(g, "BEST: " + best, W / 2.0, 286, 20, Color.WHITE);
            drawButton(g, overAgain, "PLAY AGAIN", GLASS, 24);
            drawButton(g, overMenu, "MAIN MENU", RED, 24);
        }
    }

    // ---- boosters
    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    static Path2D star(double cx, double cy, double ro, double ri) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double r = (i % 2 == 0) ? ro : ri, a = -Math.PI / 2 + i * Math.PI / 5;
            double x = cx + Math.cos(a) * r, y = cy + Math.sin(a) * r;
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.closePath();
        return p;
    }

    void drawPickup(Graphics2D g, Pickup pk) {
        double cx = pk.x, cy = pk.y + pk.bob;
        Color c = PU_COLORS[pk.kind];
        for (int i = 4; i >= 1; i--) {
            double r = 20 + i * 5 + Math.sin(frame * 0.12) * 2;
            g.setColor(alpha(c, 28));
            g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        }
        g.setPaint(new RadialGradientPaint((float) (cx - 6), (float) (cy - 7), 26f,
                new float[]{0f, 1f}, new Color[]{Color.WHITE, c}));
        g.fill(new Ellipse2D.Double(cx - 19, cy - 19, 38, 38));
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Ellipse2D.Double(cx - 19, cy - 19, 38, 38));

        if (pk.kind == JUMP) {
            g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(cx - 9, cy + 1, cx, cy - 8));
            g.draw(new Line2D.Double(cx, cy - 8, cx + 9, cy + 1));
            g.draw(new Line2D.Double(cx - 9, cy + 10, cx, cy + 1));
            g.draw(new Line2D.Double(cx, cy + 1, cx + 9, cy + 10));
        } else if (pk.kind == WINGS) {
            AffineTransform old = g.getTransform();
            g.translate(cx, cy + 3);
            g.scale(0.27, 0.27);
            drawWing(g, 0, 0, 1, 0.2);
            drawWing(g, 0, 0, -1, 0.2);
            g.setTransform(old);
        } else if (pk.kind == GOD) {
            g.setColor(Color.WHITE);
            g.fill(star(cx, cy + 1, 12, 5));
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1.8f));
            g.draw(star(cx, cy + 1, 12, 5));
        } else if (pk.kind == DOUBLE) {
            g.setColor(new Color(0x6A168C));
            g.setFont(new Font("Monospaced", Font.BOLD, 18));
            g.drawString("2X", (float) (cx - 12), (float) (cy + 7));
            g.setColor(Color.WHITE);
            g.drawString("2X", (float) (cx - 13), (float) (cy + 6));
        } else if (pk.kind == DRAGON) {
            Path2D flame = new Path2D.Double();
            flame.moveTo(cx - 9, cy + 10);
            flame.curveTo(cx - 15, cy, cx - 2, cy - 4, cx - 1, cy - 14);
            flame.curveTo(cx + 12, cy - 4, cx + 15, cy + 6, cx + 5, cy + 11);
            flame.curveTo(cx + 2, cy + 3, cx - 2, cy + 3, cx - 2, cy + 10);
            flame.closePath();
            g.setColor(new Color(0xB51F13)); g.fill(flame);
            g.setColor(new Color(0xFFF176));
            g.fill(new Ellipse2D.Double(cx - 4, cy - 2, 8, 13));
        }
        g.setStroke(new BasicStroke(1f));
        text(g, PU_SHORT[pk.kind], cx + 1, cy + 35, 11, new Color(0, 0, 0, 200));
        text(g, PU_SHORT[pk.kind], cx, cy + 34, 11, Color.WHITE);
    }

    void drawPortal(Graphics2D g, double x) {
        double cx = x + 35;
        double cy = GROUND - 105 + Math.sin(frame * 0.12) * 4;
        for (int i = 4; i >= 1; i--) {
            g.setColor(new Color(190, 80, 255, 22 + i * 5));
            g.fill(new Ellipse2D.Double(cx - 27 - i * 7, cy - 43 - i * 5,
                    54 + i * 14, 86 + i * 10));
        }
        AffineTransform old = g.getTransform();
        g.rotate(frame * 0.035, cx, cy);
        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setPaint(new GradientPaint((float) (cx - 30), (float) (cy - 40), new Color(0x7B2CFF),
                (float) (cx + 30), (float) (cy + 40), new Color(0x45F7FF)));
        g.draw(new Ellipse2D.Double(cx - 30, cy - 80, 60, 160));
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(255, 255, 255, 220));
        g.draw(new Arc2D.Double(cx - 22, cy - 70, 44, 140, frame * 4.0, 205, Arc2D.OPEN));
        g.setTransform(old);
        g.setStroke(new BasicStroke(1f));
        text(g, "PORTAL", cx, cy - 96, 12, new Color(0xEEDBFF));
    }

    void drawBonusBackground(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x10052F), 0, H, new Color(0x3B0F60)));
        g.fillRect(0, 0, W, H);
        for (int i = 0; i < 85; i++) {
            double x = mod(hash(i * 13 + 7) % W - scroll * (0.08 + (i % 4) * 0.035), W);
            double y = 24 + hash(i * 37 + 9) % (GROUND - 35);
            int r = 2 + (i % 3);
            int a = 90 + (int) (90 * (0.5 + 0.5 * Math.sin(frame * 0.08 + i)));
            g.setColor(new Color(155 + (i % 2) * 70, 70 + (i % 3) * 45, 255, a));
            g.fill(new Ellipse2D.Double(x, y, r, r));
        }
        for (int i = 0; i < 5; i++) {
            double cx = mod(i * 215 + frame * (i % 2 == 0 ? 0.45 : -0.32), W + 140) - 70;
            double cy = 72 + i * 42 + Math.sin(frame * 0.04 + i) * 16;
            g.setColor(new Color(210, 80, 255, 25));
            g.fill(new Ellipse2D.Double(cx - 45, cy - 45, 90, 90));
            g.setColor(new Color(120, 220, 255, 125));
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(cx - 28, cy - 28, 56, 56));
        }
        g.setStroke(new BasicStroke(1f));
        text(g, "BOOSTER DIMENSION", W / 2.0, 35, 19, new Color(0xFFC8FF));
    }

    void drawDragonBreath(Graphics2D g, double feetY) {
        double mouthX = PX + PW - 2;
        double mouthY = feetY - 41;
        // Fast-moving flame tongues make it look like the character is continuously breathing fire.
        for (int i = 0; i < 10; i++) {
            double t = ((fireAnim * 0.105 + i * 0.103) % 1.0);
            double x = mouthX + 22 + t * 430;
            double y = mouthY + Math.sin(t * 19 + fireAnim * 0.25 + i) * (7 + t * 8);
            double size = 7 + (1 - t) * 13;
            Path2D flame = new Path2D.Double();
            flame.moveTo(x - size, y - size * 0.45);
            flame.curveTo(x - size * 0.2, y - size * 1.45, x + size * 0.45, y - size * 0.55, x + size * 1.6, y);
            flame.curveTo(x + size * 0.2, y + size * 0.8, x - size * 0.6, y + size * 0.45, x - size, y - size * 0.45);
            flame.closePath();
            g.setColor(new Color(255, 60 + i * 8, 15, (int) (220 * (1 - t * 0.35))));
            g.fill(flame);
            g.setColor(new Color(255, 235, 90, 210));
            g.fill(new Ellipse2D.Double(x - size * 0.25, y - size * 0.22, size * 0.9, size * 0.45));
        }
        g.setColor(new Color(255, 80, 15, 150));
        g.fill(new Ellipse2D.Double(mouthX - 1, mouthY - 8, 20, 16));
    }

    void drawWing(Graphics2D g, double sx, double sy, double dir, double flap) {
        AffineTransform old = g.getTransform();
        g.translate(sx, sy);
        g.scale(dir, 1);
        g.rotate(-0.55 + flap * 0.45);
        Path2D w = new Path2D.Double();
        w.moveTo(0, 0);
        w.curveTo(18, -22, 48, -30, 66, -14);
        w.lineTo(58, -8); w.lineTo(64, -2); w.lineTo(53, 2); w.lineTo(57, 10); w.lineTo(42, 11);
        w.curveTo(28, 12, 12, 9, 0, 0);
        w.closePath();
        g.setPaint(new GradientPaint(0, -20, Color.WHITE, 60, 10, new Color(0xBFE6FF)));
        g.fill(w);
        g.setColor(new Color(0x3A6FA0));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(w);
        g.draw(new Line2D.Double(10, -2, 50, -9));
        g.draw(new Line2D.Double(10, 3, 46, 4));
        g.setStroke(new BasicStroke(1f));
        g.setTransform(old);
    }

    void drawEffects(Graphics2D g, double cx, double feetY) {
        double cy = feetY - 36;
        boolean blinkOff = (frame / 6) % 2 == 0;

        if (effectMs[WINGS] > 0 && !(effectMs[WINGS] < 2500 && blinkOff)) {
            double flap = Math.sin(frame * 0.35);
            drawWing(g, cx + 22, cy - 2, 1, flap);
            drawWing(g, cx - 22, cy - 2, -1, flap);
        }
        if (effectMs[GOD] > 0 && !(effectMs[GOD] < 3000 && blinkOff)) {
            double pulse = 0.5 + 0.5 * Math.sin(frame * 0.2);
            for (int i = 4; i >= 1; i--) {
                double r = 32 + i * 7 + pulse * 4;
                g.setColor(new Color(255, 215, 60, 26));
                g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
            }
            g.setStroke(new BasicStroke(2.5f));
            g.setColor(new Color(255, 236, 130, 210));
            g.draw(new Ellipse2D.Double(cx - 40, cy - 40, 80, 80));
            g.setStroke(new BasicStroke(1f));
            for (int k = 0; k < 6; k++) {
                double a = frame * 0.07 + k * Math.PI / 3;
                g.setColor(new Color(255, 245, 170));
                g.fill(star(cx + Math.cos(a) * 46, cy + Math.sin(a) * 46, 5, 2));
            }
        }
        if (effectMs[JUMP] > 0 && !(effectMs[JUMP] < 2500 && blinkOff)) {
            g.setColor(new Color(92, 255, 122, 80));
            g.fill(new Ellipse2D.Double(cx - 34, feetY - 7, 68, 14));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int k = 0; k < 3; k++) {
                double t = ((frame * 0.9 + k * 8) % 24) / 24.0;
                double x = cx - 16 + k * 16, y = feetY + 6 - t * 24;
                g.setColor(new Color(92, 255, 122, (int) (220 * (1 - t))));
                g.draw(new Line2D.Double(x - 6, y + 4, x, y - 2));
                g.draw(new Line2D.Double(x, y - 2, x + 6, y + 4));
            }
            g.setStroke(new BasicStroke(1f));
        }
    }

    void drawEffectHud(Graphics2D g) {
        int row = 0;
        for (int i = 0; i < effectMs.length; i++) {
            if (effectMs[i] <= 0) continue;
            int x = W - 194, y = 90 + row * 32, w = 170, h = 26;
            boolean low = effectMs[i] < 3000 && (frame / 8) % 2 == 0;
            double frac = effectMs[i] / (PU_SECONDS[i] * 1000);
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRoundRect(x, y, w, h, 18, 18);
            g.setColor(low ? new Color(255, 255, 255, 210) : alpha(PU_COLORS[i], 210));
            g.fillRoundRect(x + 2, y + 2, Math.max(8, (int) ((w - 4) * frac)), h - 4, 14, 14);
            g.setStroke(new BasicStroke(1.5f));
            g.setColor(new Color(255, 255, 255, 170));
            g.drawRoundRect(x, y, w, h, 18, 18);
            g.setStroke(new BasicStroke(1f));
            String label = PU_NAMES[i] + "  " + (int) Math.ceil(effectMs[i] / 1000.0) + "s";
            text(g, label, x + w / 2.0 + 1, y + h / 2.0 + 1, 13, new Color(0, 0, 0, 200));
            text(g, label, x + w / 2.0, y + h / 2.0, 13, Color.WHITE);
            row++;
        }
    }

    // ---- background themes
    static double mod(double a, double m) { return ((a % m) + m) % m; }

    static int hash(int n) {
        n = (n ^ 61) ^ (n >>> 16);
        n *= 9;
        n ^= n >>> 4;
        n *= 0x27d4eb2d;
        n ^= n >>> 15;
        return n & 0x7fffffff;
    }

    void drawBackground(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, theme.skyTop, 0, GROUND, theme.skyBottom));
        g.fillRect(0, 0, W, H);
        switch (theme) {
            case NEON:
                nebula(g); stars(g); drawCity(g, new Color(0x15133B), new Color(0x42F5F2));
                break;
            case DESSERT:
                sun(g, 720, 105, 48, new Color(255, 236, 140));
                clouds(g, 0.08, 150);
                hills(g, 0.18, new Color(0xDFA34E), 125, 35, 0.006);
                hills(g, 0.35, new Color(0xC77B3D), 75, 24, 0.009);
                break;
            case MOON:
                nebula(g); stars(g); planet(g);
                g.setColor(new Color(190, 190, 210, 90));
                for (int i = 0; i < 12; i++) {
                    double x = mod(i * 113 - scroll * 0.12, W + 80) - 40;
                    double y = 45 + hash(i + 500) % 260;
                    g.fill(new Ellipse2D.Double(x, y, 20 + i % 3 * 9, 12 + i % 4 * 4));
                }
                break;
            case RAGNAROK:
                sun(g, 690, 105, 54, new Color(255, 135, 80));
                mountains(g, 0.10, new Color(0x65446F), new Color(0xF2B17A), 260, 230, true);
                mountains(g, 0.25, new Color(0x342E55), new Color(0xD77B61), 190, 160, true);
                hills(g, 0.4, new Color(0x43543C), 50, 18, 0.012);
                break;
            case VOLCANO:
                mountains(g, 0.12, new Color(0x301F2A), new Color(0xFF6A35), 250, 245, false);
                mountains(g, 0.25, new Color(0x55232A), new Color(0xFF3D24), 180, 180, false);
                for (int i = 0; i < 24; i++) {
                    double x = mod(i * 61 - scroll * 0.3, W);
                    double y = 50 + hash(i + 20) % 230;
                    g.setColor(new Color(255, 95 + hash(i) % 100, 30, 150));
                    g.fill(new Ellipse2D.Double(x, y, 3 + i % 4, 3 + i % 4));
                }
                break;
            case ROOFTOPS:
                sun(g, 710, 100, 40, new Color(255, 210, 150));
                clouds(g, 0.08, 120);
                drawCity(g, new Color(0x28334D), new Color(0xFFB17A));
                break;
            case COUNTRYSIDE:
                sun(g, 720, 95, 42, new Color(255, 235, 130));
                clouds(g, 0.06, 170);
                hills(g, 0.12, new Color(0x9BD36A), 145, 35, 0.006);
                hills(g, 0.28, new Color(0x5BAA50), 95, 28, 0.009);
                drawCountrysideTrees(g);
                break;
            case WINTER:
                sun(g, 720, 95, 32, new Color(230, 245, 255));
                mountains(g, 0.12, new Color(0x8AAFC7), Color.WHITE, 230, 220, true);
                mountains(g, 0.25, new Color(0x668BAA), Color.WHITE, 170, 150, true);
                snowfall(g);
                break;
            case SPRING:
                sun(g, 720, 95, 38, new Color(255, 224, 130));
                clouds(g, 0.08, 150);
                hills(g, 0.15, new Color(0xA6E58A), 130, 30, 0.006);
                hills(g, 0.30, new Color(0x63C66D), 75, 22, 0.010);
                drawSpringFlowers(g);
                break;
            case SEASONS:
                drawSeasonMix(g);
                break;
            case CAVES:
                drawCaveBackground(g);
                break;
            case ALIEN_PLANET:
                nebula(g); stars(g); planet(g);
                drawAlienLandscape(g);
                break;
            case JUNKYARD:
                drawJunkyard(g);
                break;
            case RAINBOW:
                drawRainbow(g);
                break;
            case HAUNTED:
                drawHaunted(g);
                break;
            case NORTHPOLE:
                drawNorthPole(g);
                break;
            case NUCLEAR_PLANT:
                drawNuclearPlant(g);
                break;
            case ARENA:
                drawArena(g);
                break;
            case JUNGLE:
                drawJungle(g);
                break;
            case PROGNOSIN:
                drawPrognosin(g);
                break;
            case BOGLAND:
                drawBogland(g);
                break;
            case SUPER_HIGHWAY:
                drawSuperHighway(g);
                break;
            case SUPER_DESERT:
                drawSuperDesert(g);
                break;
        }
    }

    // ---- New Stages Graphic Drawers ----
    void drawJunkyard(Graphics2D g) {
        hills(g, 0.12, new Color(0x3B2E24), 140, 40, 0.01);
        hills(g, 0.25, new Color(0x5E4331), 90, 30, 0.015);
        for (int i = 0; i < 10; i++) {
            double x = mod(i * 120 - scroll * 0.3, W + 100) - 50;
            g.setColor(new Color(0x8B4513));
            g.fillRect((int)x, GROUND - 45, 25, 45);
            g.setColor(new Color(0x4A3525));
            g.fill(new Ellipse2D.Double(x - 10, GROUND - 70, 45, 30));
        }
    }

    void drawRainbow(Graphics2D g) {
        clouds(g, 0.05, 200);
        int cx = W / 2, cy = GROUND + 100;
        Color[] rColors = {new Color(255,0,0,120), new Color(255,127,0,120), new Color(255,255,0,120),
                           new Color(0,255,0,120), new Color(0,0,255,120), new Color(139,0,255,120)};
        g.setStroke(new BasicStroke(12f));
        for (int i = 0; i < rColors.length; i++) {
            g.setColor(rColors[i]);
            g.draw(new Arc2D.Double(cx - 350 + i * 12, cy - 350 + i * 12, 700 - i * 24, 700 - i * 24, 0, 180, Arc2D.OPEN));
        }
        g.setStroke(new BasicStroke(1f));
        hills(g, 0.15, new Color(0xA8F0C6), 110, 25, 0.008);
    }

    void drawHaunted(Graphics2D g) {
        sun(g, 700, 100, 60, new Color(240, 240, 255, 200));
        stars(g);
        mountains(g, 0.15, new Color(0x201235), new Color(0x44266D), 220, 200, false);
        for (int i = 0; i < 8; i++) {
            double x = mod(i * 150 - scroll * 0.25, W + 100) - 50;
            g.setColor(new Color(0x10081C));
            g.fillRect((int)x + 10, GROUND - 50, 8, 50);
            g.fillRect((int)x, GROUND - 70, 28, 25);
            // floating ghosts
            double gy = 100 + Math.sin(frame * 0.08 + i) * 20;
            double gx = mod(i * 130 - scroll * 0.1, W);
            g.setColor(new Color(255, 255, 255, 140));
            g.fill(new Ellipse2D.Double(gx, gy, 20, 30));
        }
    }

    void drawNorthPole(Graphics2D g) {
        // Aurora Borealis
        for (int i = 0; i < 3; i++) {
            Path2D a = new Path2D.Double();
            a.moveTo(0, 50 + i * 30);
            for (int x = 0; x <= W; x += 30) {
                a.lineTo(x, 50 + i * 30 + Math.sin((x + frame * 4) * 0.01 + i) * 25);
            }
            a.lineTo(W, 0); a.lineTo(0, 0); a.closePath();
            g.setColor(new Color(50, 255, 180, 50));
            g.fill(a);
        }
        mountains(g, 0.12, new Color(0x3B6D8C), Color.WHITE, 220, 210, true);
        snowfall(g);
        // Candy Canes
        for (int i = 0; i < 6; i++) {
            double x = mod(i * 180 - scroll * 0.22, W + 100) - 50;
            g.setColor(Color.WHITE);
            g.fillRect((int)x, GROUND - 60, 10, 60);
            g.setColor(Color.RED);
            for(int c=0; c<60; c+=15) g.fillRect((int)x, GROUND - 60 + c, 10, 6);
        }
    }

    void drawNuclearPlant(Graphics2D g) {
        for (int i = 0; i < 4; i++) {
            double x = mod(i * 260 - scroll * 0.2, W + 200) - 100;
            Path2D tower = new Path2D.Double();
            tower.moveTo(x, GROUND);
            tower.lineTo(x + 15, GROUND - 140);
            tower.lineTo(x + 65, GROUND - 140);
            tower.lineTo(x + 80, GROUND);
            tower.closePath();
            g.setColor(new Color(0x1B301B));
            g.fill(tower);
            g.setColor(new Color(0x39FF14, true));
            g.setStroke(new BasicStroke(2f));
            g.draw(tower);
            g.setStroke(new BasicStroke(1f));
        }
        // Green glow particles
        for (int i = 0; i < 30; i++) {
            double x = mod(hash(i) % W - scroll * 0.2, W);
            double y = mod(hash(i + 33) % GROUND - frame * 0.5, GROUND);
            g.setColor(new Color(0x39FF14));
            g.fill(new Ellipse2D.Double(x, y, 4, 4));
        }
    }

    void drawArena(Graphics2D g) {
        // Colosseum Pillars Background
        g.setColor(new Color(0x5C4632));
        for (int i = 0; i < 12; i++) {
            double x = mod(i * 90 - scroll * 0.2, W + 100) - 50;
            g.fillRect((int)x, GROUND - 160, 24, 160);
            g.fill(new Arc2D.Double(x - 23, GROUND - 180, 70, 40, 0, 180, Arc2D.OPEN));
        }
        // Stadium spotlight beams
        for (int i = 0; i < 3; i++) {
            double x = 150 + i * 300;
            Path2D beam = new Path2D.Double();
            beam.moveTo(x, 0);
            beam.lineTo(x - 80 + Math.sin(frame * 0.03 + i) * 60, GROUND);
            beam.lineTo(x + 80 + Math.sin(frame * 0.03 + i) * 60, GROUND);
            beam.closePath();
            g.setColor(new Color(255, 235, 170, 35));
            g.fill(beam);
        }
    }

    void drawJungle(Graphics2D g) {
        hills(g, 0.10, new Color(0x0F3816), 160, 40, 0.01);
        hills(g, 0.22, new Color(0x185422), 100, 30, 0.012);
        // Vines and big canopy leaves
        for (int i = 0; i < 10; i++) {
            double x = mod(i * 110 - scroll * 0.35, W + 100) - 50;
            g.setColor(new Color(0x237831));
            g.fill(new Ellipse2D.Double(x - 30, GROUND - 110, 80, 120));
            g.setColor(new Color(0x0C3011));
            g.fillRect((int)x + 5, GROUND - 50, 10, 50);
        }
    }

    void drawPrognosin(Graphics2D g) {
        // Cyber digital grid background
        g.setColor(new Color(0, 229, 255, 40));
        g.setStroke(new BasicStroke(1.5f));
        for (int x = 0; x < W; x += 40) {
            double gx = mod(x - scroll * 0.5, W);
            g.draw(new Line2D.Double(gx, 0, gx, GROUND));
        }
        for (int y = 0; y < GROUND; y += 30) {
            g.draw(new Line2D.Double(0, y, W, y));
        }
        // Digital data pillars
        for (int i = 0; i < 7; i++) {
            double x = mod(i * 140 - scroll * 0.25, W + 100) - 50;
            g.setColor(new Color(0, 229, 255, 80));
            g.fillRect((int)x, GROUND - 180, 35, 180);
            g.setColor(Color.WHITE);
            g.fillRect((int)x + 5, GROUND - 180 + (frame * 3 + i * 20) % 160, 25, 6);
        }
        g.setStroke(new BasicStroke(1f));
    }

    void drawBogland(Graphics2D g) {
        hills(g, 0.15, new Color(0x232D21), 120, 20, 0.008);
        clouds(g, 0.04, 60); // swamp mist
        for (int i = 0; i < 8; i++) {
            double x = mod(i * 130 - scroll * 0.2, W + 100) - 50;
            g.setColor(new Color(0x171C15));
            g.fillRect((int)x + 12, GROUND - 80, 12, 80);
            g.fill(new Ellipse2D.Double(x, GROUND - 110, 36, 40));
        }
        // Will-o'-the-wisps / Fireflies
        for (int i = 0; i < 20; i++) {
            double x = mod(hash(i) % W - scroll * 0.1, W);
            double y = 150 + Math.sin(frame * 0.05 + i) * 30 + (hash(i + 5) % 150);
            g.setColor(new Color(0x9DFF00));
            g.fill(new Ellipse2D.Double(x, y, 5, 5));
        }
    }

    void drawSuperHighway(Graphics2D g) {
        drawCity(g, new Color(0x0F172A), new Color(0xF59E0B));
        // Multi-lane background roads
        for (int lane = 0; lane < 3; lane++) {
            int ly = GROUND - 110 + lane * 30;
            g.setColor(new Color(0x1E293B));
            g.fillRect(0, ly, W, 18);
            g.setColor(new Color(0xF59E0B, true));
            // Distant vehicle lights on background roads
            for (int i = 0; i < 5; i++) {
                double vx = mod(i * 220 + scroll * (0.4 + lane * 0.2), W);
                g.fillRect((int)vx, ly + 4, 16, 6);
            }
        }
    }

    void drawSuperDesert(Graphics2D g) {
        sun(g, 700, 110, 55, new Color(0xFFD000));
        // Oasis in the background
        double oasisX = mod(500 - scroll * 0.1, W + 300) - 150;
        g.setColor(new Color(0x38BDF8));
        g.fill(new Ellipse2D.Double(oasisX, GROUND - 50, 140, 30));
        g.setColor(new Color(0x15803D));
        for (int p = 0; p < 3; p++) {
            g.fillRect((int)oasisX + 30 + p * 30, GROUND - 90, 8, 45);
            g.fill(new Ellipse2D.Double(oasisX + 15 + p * 30, GROUND - 110, 38, 25));
        }
        hills(g, 0.20, new Color(0xB45309), 110, 35, 0.007);
        hills(g, 0.35, new Color(0x78350F), 70, 20, 0.01);
        // Sandstorm particle effects
        g.setColor(new Color(253, 230, 138, 110));
        for (int i = 0; i < 45; i++) {
            double x = mod(hash(i) % W - scroll * 1.5, W);
            double y = hash(i + 17) % GROUND;
            g.fill(new Rectangle2D.Double(x, y, 12 + i % 8, 2));
        }
    }

    void drawCountrysideTrees(Graphics2D g) {
        for (int i = 0; i < 9; i++) {
            double x = mod(i * 125 - scroll * 0.22, W + 100) - 50;
            double y = GROUND - 95 - (i % 3) * 12;
            g.setColor(new Color(0x79512F));
            g.fillRect((int)x - 4, (int)y + 18, 8, 38);
            g.setColor(new Color(0x368A45));
            g.fill(new Ellipse2D.Double(x - 25, y - 8, 50, 45));
        }
    }

    void drawSpringFlowers(Graphics2D g) {
        for (int i = 0; i < 28; i++) {
            double x = mod(i * 43 - scroll * 0.35, W);
            double y = 90 + hash(i + 91) % 220;
            g.setColor(new Color(i % 2 == 0 ? 0xFF8CCB : 0xFFF18A));
            g.fill(new Ellipse2D.Double(x, y, 6, 6));
        }
    }

    void drawSeasonMix(Graphics2D g) {
        int phase = (int)(distance / 1800) % 4;
        switch (phase) {
            case 0:
                sun(g, 720, 95, 38, new Color(255, 235, 130));
                clouds(g, 0.08, 150);
                hills(g, 0.18, new Color(0x63B84A), 100, 28, 0.008);
                drawSpringFlowers(g);
                break;
            case 1:
                sun(g, 720, 95, 42, new Color(255, 220, 130));
                hills(g, 0.18, new Color(0xDFA34E), 125, 35, 0.006);
                hills(g, 0.35, new Color(0xC77B3D), 75, 24, 0.009);
                break;
            case 2:
                mountains(g, 0.15, new Color(0x8AAFC7), Color.WHITE, 220, 200, true);
                snowfall(g);
                break;
            default:
                clouds(g, 0.08, 150);
                hills(g, 0.15, new Color(0xA6E58A), 130, 30, 0.006);
                drawSpringFlowers(g);
                break;
        }
    }

    void drawCaveBackground(Graphics2D g) {
        g.setColor(new Color(20, 16, 32, 150));
        g.fillRect(0, 0, W, GROUND);
        for (int i = 0; i < 8; i++) {
            double x = mod(i * 145 - scroll * 0.18, W + 150) - 75;
            int h = 90 + hash(i + 12) % 130;
            Path2D stalactite = new Path2D.Double();
            stalactite.moveTo(x, 0); stalactite.lineTo(x + 45, 0);
            stalactite.lineTo(x + 22, h); stalactite.closePath();
            g.setColor(new Color(0x3B3449)); g.fill(stalactite);
            Path2D stalagmite = new Path2D.Double();
            stalagmite.moveTo(x, GROUND); stalagmite.lineTo(x + 50, GROUND);
            stalagmite.lineTo(x + 25, GROUND - h * 0.55); stalagmite.closePath();
            g.setColor(new Color(0x62566F)); g.fill(stalagmite);
        }
    }

    void drawAlienLandscape(Graphics2D g) {
        for (int i = 0; i < 7; i++) {
            double x = mod(i * 150 - scroll * 0.2, W + 180) - 80;
            g.setColor(new Color(0x5C2C91));
            g.fill(new Ellipse2D.Double(x, GROUND - 155 - (i % 3) * 20, 65, 85));
            g.setColor(new Color(0x6AF0C2));
            g.fill(new Ellipse2D.Double(x + 18, GROUND - 145 - (i % 3) * 20, 28, 28));
        }
    }

    void drawCity(Graphics2D g, Color buildings, Color lights) {
        for (int i = 0; i < 15; i++) {
            double bw = 38 + hash(i + 40) % 38;
            double bh = 70 + hash(i + 90) % 150;
            double x = mod(i * 83 - scroll * 0.22, W + 90) - 45;
            double y = GROUND - bh;
            g.setColor(buildings);
            g.fillRect((int)x, (int)y, (int)bw, (int)bh);
            for (int wy = (int)y + 12; wy < GROUND - 10; wy += 18) {
                for (int wx = (int)x + 8; wx < x + bw - 5; wx += 13) {
                    if (hash(wx + wy + i) % 3 != 0) {
                        g.setColor(lights);
                        g.fillRect(wx, wy, 4, 6);
                    }
                }
            }
        }
    }

    void clouds(Graphics2D g, double par, int alpha) {
        g.setColor(new Color(255, 255, 255, alpha));
        for (int i = 0; i < 6; i++) {
            double x = mod(i * 210 - scroll * par, W + 260) - 130;
            double y = 45 + (i * 53) % 110;
            g.fill(new Ellipse2D.Double(x, y, 90, 30));
            g.fill(new Ellipse2D.Double(x + 20, y - 14, 56, 36));
            g.fill(new Ellipse2D.Double(x + 48, y - 6, 60, 30));
        }
    }

    void hills(Graphics2D g, double par, Color c, double base, double amp, double freq) {
        Path2D p = new Path2D.Double();
        p.moveTo(0, GROUND);
        for (int x = 0; x <= W; x += 10) p.lineTo(x, GROUND - base + Math.sin((x + scroll * par) * freq) * amp);
        p.lineTo(W, GROUND);
        p.closePath();
        g.setColor(c);
        g.fill(p);
    }

    void mountains(Graphics2D g, double par, Color body, Color cap, double spacing, double maxH, boolean caps) {
        int off = (int) Math.floor(scroll * par / spacing);
        double sh = mod(scroll * par, spacing);
        for (int i = -1; i <= W / spacing + 1; i++) {
            int hs = hash(i + off + (int) maxH);
            double hh = maxH * (0.55 + 0.45 * (hs % 100) / 100.0);
            double bx = i * spacing - sh;
            double half = spacing * 0.75;
            Path2D m = new Path2D.Double();
            m.moveTo(bx - half, GROUND);
            m.lineTo(bx, GROUND - hh);
            m.lineTo(bx + half, GROUND);
            m.closePath();
            g.setColor(body);
            g.fill(m);
            if (caps) {
                double d = hh * 0.28, hw = half * 0.28, top = GROUND - hh;
                Path2D c = new Path2D.Double();
                c.moveTo(bx - hw, top + d);
                c.lineTo(bx, top);
                c.lineTo(bx + hw, top + d);
                c.lineTo(bx + hw * 0.35, top + d * 0.72);
                c.lineTo(bx - hw * 0.25, top + d * 0.85);
                c.closePath();
                g.setColor(cap);
                g.fill(c);
            }
        }
    }

    void snowfall(Graphics2D g) {
        g.setColor(new Color(255, 255, 255, 220));
        for (int i = 0; i < 70; i++) {
            double x = mod(hash(i) % W - scroll * 0.5 * (0.4 + (i % 3) * 0.3) + Math.sin(frame * 0.03 + i) * 10, W);
            double y = mod(hash(i + 500) % GROUND + frame * (0.8 + (i % 3) * 0.6), GROUND);
            double s = 2 + i % 3;
            g.fill(new Ellipse2D.Double(x, y, s, s));
        }
    }

    void sun(Graphics2D g, double cx, double cy, double r, Color c) {
        for (int i = 3; i >= 1; i--) {
            double rr = r + i * 14;
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 30));
            g.fill(new Ellipse2D.Double(cx - rr, cy - rr, rr * 2, rr * 2));
        }
        g.setColor(c);
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    void nebula(Graphics2D g) {
        g.setPaint(new RadialGradientPaint(280, 180, 260, new float[]{0f, 1f},
                new Color[]{new Color(140, 60, 200, 90), new Color(0, 0, 0, 0)}));
        g.fillRect(0, 0, W, H);
        g.setPaint(new RadialGradientPaint(700, 300, 220, new float[]{0f, 1f},
                new Color[]{new Color(40, 120, 220, 70), new Color(0, 0, 0, 0)}));
        g.fillRect(0, 0, W, H);
    }

    void stars(Graphics2D g) {
        for (int i = 0; i < 100; i++) {
            double x = mod(hash(i) % W - scroll * (0.05 + (i % 3) * 0.06), W);
            double y = hash(i + 99) % GROUND;
            int a = (int) (140 + 110 * Math.sin(frame * 0.05 + i));
            g.setColor(new Color(255, 255, 255, Math.max(0, Math.min(255, a))));
            double s = 1 + i % 3;
            g.fill(new Ellipse2D.Double(x, y, s, s));
        }
    }

    void planet(Graphics2D g) {
        double plx = mod(720 - scroll * 0.03, W + 300) - 100, ply = 130;
        g.setPaint(new GradientPaint((float) (plx - 55), (float) (ply - 55), new Color(0xFFB36B),
                (float) (plx + 55), (float) (ply + 55), new Color(0xB5472A)));
        g.fill(new Ellipse2D.Double(plx - 55, ply - 55, 110, 110));
        g.setStroke(new BasicStroke(6f));
        g.setColor(new Color(255, 220, 160, 150));
        g.draw(new Ellipse2D.Double(plx - 90, ply - 18, 180, 36));
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(0xBBBBCC));
        g.fill(new Ellipse2D.Double(plx - 170, ply + 60, 28, 28));
    }

    // ---- ground
    void drawGround(Graphics2D g) {
        g.setColor(theme.groundBody);
        g.fillRect(0, GROUND + 4, W, H - GROUND - 4);

        double sp = 70;
        int off = (int) Math.floor(scroll / sp);
        double sh = mod(scroll, sp);
        for (int i = -1; i <= W / sp + 1; i++) {
            int hs = hash(i + off);
            double x = i * sp - sh + hs % 40;
            double y = GROUND + 36 + (hs / 7) % (H - GROUND - 60);
            if (theme == Theme.MOON || theme == Theme.NEON || theme == Theme.PROGNOSIN) {
                g.setColor(new Color(0, 0, 0, 60));
                g.fill(new Ellipse2D.Double(x, y, 34, 9));
                g.setColor(new Color(255, 255, 255, 30));
                g.draw(new Ellipse2D.Double(x, y, 34, 9));
            } else {
                g.setColor(new Color(0, 0, 0, 40));
                g.fill(new Ellipse2D.Double(x, y, 6 + hs % 8, 4 + hs % 4));
            }
        }

        Path2D p = new Path2D.Double();
        p.moveTo(0, GROUND + 18);
        for (int x = 0; x <= W; x += 10) p.lineTo(x, GROUND - 4 + Math.sin((x + scroll) * 0.03) * 2.5);
        p.lineTo(W, GROUND + 18);
        p.closePath();
        g.setColor(theme.groundTop);
        g.fill(p);
        g.setColor(new Color(0, 0, 0, 50));
        g.fillRect(0, GROUND + 18, W, 3);

        if (theme == Theme.WINTER || theme == Theme.NORTHPOLE) snowfall(g);
        if (theme == Theme.DESSERT || theme == Theme.RAGNAROK || theme == Theme.COUNTRYSIDE || theme == Theme.SPRING || theme == Theme.SEASONS || theme == Theme.JUNGLE || theme == Theme.BOGLAND) {
            g.setStroke(new BasicStroke(2f));
            g.setColor(theme == Theme.JUNGLE ? new Color(0x1B5E20) : new Color(0x5FAE2A));
            for (int i = -1; i <= W / sp + 1; i++) {
                int hs = hash(i + off + 33);
                if (hs % 3 != 0) continue;
                double x = i * sp - sh + hs % 50;
                g.draw(new Line2D.Double(x, GROUND - 3, x - 5, GROUND - 15));
                g.draw(new Line2D.Double(x, GROUND - 3, x, GROUND - 18));
                g.draw(new Line2D.Double(x, GROUND - 3, x + 5, GROUND - 14));
            }
            g.setStroke(new BasicStroke(1f));
        }
    }

    // ---- upper road
    void drawUpperRoad(Graphics2D g) {
        Color gb = theme.groundBody;
        double sp = 220, sh = mod(scroll, sp);
        g.setColor(new Color(gb.getRed(), gb.getGreen(), gb.getBlue(), 170));
        for (int i = -1; i <= W / sp + 1; i++)
            g.fill(new Rectangle2D.Double(i * sp - sh, UPPER + 34, 14, GROUND - UPPER - 30));
        g.setColor(gb);
        g.fillRect(0, UPPER + 14, W, 20);
        Path2D p = new Path2D.Double();
        p.moveTo(0, UPPER + 18);
        for (int x = 0; x <= W; x += 10) p.lineTo(x, UPPER - 4 + Math.sin((x + scroll) * 0.03) * 2.5);
        p.lineTo(W, UPPER + 18);
        p.closePath();
        g.setColor(theme.groundTop);
        g.fill(p);
        g.setColor(new Color(0, 0, 0, 50));
        g.fillRect(0, UPPER + 18, W, 3);
        double s2 = 70, sh2 = mod(scroll, s2);
        int off = (int) Math.floor(scroll / s2);
        for (int i = -1; i <= W / s2 + 1; i++) {
            int hs = hash(i + off + 91);
            g.setColor(new Color(0, 0, 0, 40));
            g.fill(new Ellipse2D.Double(i * s2 - sh2 + hs % 40, UPPER + 24 + hs % 6, 6 + hs % 8, 4));
        }
    }

    // ---- stage-specific obstacles
    // Obstacle type 0 is a small ground hazard, type 1 is a larger hazard,
    // and type 2 is an airborne hazard. Each stage gives these silhouettes a theme.
    void drawObstacle(Graphics2D g, Obstacle o) {
        double x = o.x, y = o.y + (o.type == 2 ? o.bob : 0), w = o.w, h = o.h;
        switch (theme) {
            case NEON:
                if (o.type == 2) drawCyberDrone(g, x, y, w, h, new Color(0x39F5FF));
                else drawEnergyBlock(g, x, y, w, h, new Color(0x39F5FF));
                break;
            case DESSERT:
                if (o.type == 2) drawFlyingHazard(g, x, y, w, h, new Color(0x6B4B2A));
                else drawCactus(g, x, y, w, h, new Color(0x398348));
                break;
            case MOON:
                if (o.type == 2) drawShip(g, x, y, w, h);
                else if (o.type == 0) drawCrater(g, x, y, w, h);
                else drawAlien(g, x, y, w, h);
                break;
            case RAGNAROK:
                if (o.type == 2) drawFlyingHazard(g, x, y, w, h, new Color(0xE7A15E));
                else drawRuneStone(g, x, y, w, h);
                break;
            case VOLCANO:
                if (o.type == 2) drawFireOrb(g, x, y, w, h);
                else drawMagmaRock(g, x, y, w, h);
                break;
            case ROOFTOPS:
                if (o.type == 2) drawCyberDrone(g, x, y, w, h, new Color(0xFF9D78));
                else drawRooftopVent(g, x, y, w, h);
                break;
            case COUNTRYSIDE:
                if (o.type == 2) drawFlyingHazard(g, x, y, w, h, new Color(0x7C5A32));
                else if (o.type == 0) drawHayBale(g, x, y, w, h);
                else drawFence(g, x, y, w, h, new Color(0x8C5B31));
                break;
            case WINTER:
                if (o.type == 2) drawIcicle(g, x, y, w, h);
                else if (o.type == 0) drawSnowMound(g, x, y, w, h);
                else drawSnowman(g, x, y, w, h);
                break;
            case SPRING:
                if (o.type == 2) drawButterfly(g, x, y, w, h);
                else drawThornBush(g, x, y, w, h);
                break;
            case SEASONS:
                if (o.type == 2) drawFlyingLeaves(g, x, y, w, h);
                else if (o.type == 0) drawLeafPile(g, x, y, w, h);
                else drawSeasonTree(g, x, y, w, h);
                break;
            case CAVES:
                if (o.type == 2) drawBat(g, x, y, w, h);
                else drawStalagmite(g, x, y, w, h);
                break;
            case ALIEN_PLANET:
                if (o.type == 2) drawShip(g, x, y, w, h);
                else if (o.type == 0) drawAlienPod(g, x, y, w, h);
                else drawAlien(g, x, y, w, h);
                break;
            case JUNKYARD:
                if (o.type == 2) drawCyberDrone(g, x, y, w, h, new Color(0xE6B56A));
                else if (o.type == 0) drawTire(g, x, y, w, h);
                else drawScrapPile(g, x, y, w, h);
                break;
            case RAINBOW:
                if (o.type == 2) drawRainbowCloud(g, x, y, w, h);
                else drawCrystal(g, x, y, w, h, new Color(0xCF78F3));
                break;
            case HAUNTED:
                if (o.type == 2) drawBat(g, x, y, w, h);
                else if (o.type == 0) drawTombstone(g, x, y, w, h);
                else drawGhost(g, x, y, w, h);
                break;
            case NORTHPOLE:
                if (o.type == 2) drawOrnament(g, x, y, w, h);
                else if (o.type == 0) drawCandyCane(g, x, y, w, h);
                else drawSnowMound(g, x, y, w, h);
                break;
            case NUCLEAR_PLANT:
                if (o.type == 2) drawToxicDrone(g, x, y, w, h);
                else drawToxicBarrel(g, x, y, w, h);
                break;
            case ARENA:
                if (o.type == 2) drawFlyingHazard(g, x, y, w, h, new Color(0xD8B56C));
                else if (o.type == 0) drawArenaBlock(g, x, y, w, h);
                else drawArenaPillar(g, x, y, w, h);
                break;
            case JUNGLE:
                if (o.type == 2) drawFlyingHazard(g, x, y, w, h, new Color(0xE1D46B));
                else if (o.type == 0) drawFallenLog(g, x, y, w, h);
                else drawThornBush(g, x, y, w, h);
                break;
            case PROGNOSIN:
                if (o.type == 2) drawCyberDrone(g, x, y, w, h, new Color(0x00E5FF));
                else drawLaserBarrier(g, x, y, w, h);
                break;
            case BOGLAND:
                if (o.type == 2) drawDragonfly(g, x, y, w, h);
                else if (o.type == 0) drawSwampStump(g, x, y, w, h);
                else drawSlime(g, x, y, w, h);
                break;
            case SUPER_HIGHWAY:
                if (o.type == 2) drawFlyingCar(g, x, y, w, h);
                else if (o.type == 0) drawTrafficCone(g, x, y, w, h);
                else drawRoadBarrier(g, x, y, w, h);
                break;
            case SUPER_DESERT:
                if (o.type == 2) drawFlyingHazard(g, x, y, w, h, new Color(0x5E4831));
                else if (o.type == 0) drawCactus(g, x, y, w, h, new Color(0x3D8451));
                else drawMagmaRock(g, x, y, w, h);
                break;
            default:
                if (o.type == 2) drawShip(g, x, y, w, h);
                else drawRock(g, x, y, w, h);
                break;
        }
    }

    void outline(Graphics2D g, Shape s, Color c) {
        g.setColor(c);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
        g.setStroke(new BasicStroke(1f));
    }

    void drawEnergyBlock(Graphics2D g, double x, double y, double w, double h, Color c) {
        Path2D p = new Path2D.Double();
        p.moveTo(x + w * .12, y + h * .18); p.lineTo(x + w * .75, y);
        p.lineTo(x + w, y + h * .25); p.lineTo(x + w * .86, y + h);
        p.lineTo(x + w * .12, y + h * .88); p.closePath();
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 70));
        g.fill(new Ellipse2D.Double(x - 6, y - 5, w + 12, h + 10));
        g.setPaint(new GradientPaint((float)x, (float)y, Color.WHITE, (float)(x+w), (float)(y+h), c.darker()));
        g.fill(p); outline(g, p, c.brighter());
        g.setColor(Color.WHITE); g.drawLine((int)(x+w*.27), (int)(y+h*.26), (int)(x+w*.68), (int)(y+h*.68));
    }

    void drawCyberDrone(Graphics2D g, double x, double y, double w, double h, Color c) {
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 55));
        g.fill(new Ellipse2D.Double(x - 3, y - 3, w + 6, h + 6));
        g.setColor(new Color(35, 39, 61)); g.fillRoundRect((int)(x+w*.15), (int)(y+h*.2), (int)(w*.7), (int)(h*.6), 12, 12);
        g.setColor(c); g.setStroke(new BasicStroke(3f));
        g.drawLine((int)(x+w*.08), (int)(y+h*.5), (int)(x+w*.92), (int)(y+h*.5));
        g.fillOval((int)(x+w*.32), (int)(y+h*.34), (int)(w*.16), (int)(h*.28));
        g.fillOval((int)(x+w*.58), (int)(y+h*.34), (int)(w*.16), (int)(h*.28));
        g.setStroke(new BasicStroke(1f));
    }

    void drawCactus(Graphics2D g, double x, double y, double w, double h, Color c) {
        g.setColor(c.darker());
        g.fillRoundRect((int)(x+w*.36), (int)(y+h*.12), (int)(w*.30), (int)(h*.88), 12, 12);
        if (w > 50) {
            g.fillRoundRect((int)(x+w*.05), (int)(y+h*.42), (int)(w*.34), (int)(h*.17), 9, 9);
            g.fillRoundRect((int)(x+w*.08), (int)(y+h*.25), (int)(w*.15), (int)(h*.35), 9, 9);
            g.fillRoundRect((int)(x+w*.62), (int)(y+h*.55), (int)(w*.31), (int)(h*.16), 9, 9);
            g.fillRoundRect((int)(x+w*.76), (int)(y+h*.36), (int)(w*.15), (int)(h*.35), 9, 9);
        }
        g.setColor(c.brighter()); g.fillRoundRect((int)(x+w*.42), (int)(y+h*.16), (int)(w*.08), (int)(h*.75), 5, 5);
        g.setColor(new Color(255, 255, 220, 190));
        for (int i=1;i<5;i++) g.drawLine((int)(x+w*.3), (int)(y+h*i/5.0), (int)(x+w*.36), (int)(y+h*i/5.0));
    }

    void drawCrater(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0xB8BBD2));
        g.fill(new Ellipse2D.Double(x, y + h*.35, w, h*.62));
        g.setColor(new Color(0x777B96));
        g.fill(new Ellipse2D.Double(x+w*.10, y+h*.48, w*.80, h*.37));
        g.setColor(new Color(0x35384D));
        g.fill(new Ellipse2D.Double(x+w*.22, y+h*.57, w*.56, h*.21));
        g.setColor(new Color(255,255,255,105));
        g.drawArc((int)(x+w*.12), (int)(y+h*.43), (int)(w*.72), (int)(h*.32), 10, 155);
    }

    void drawAlien(Graphics2D g, double x, double y, double w, double h) {
        Color green = new Color(0x79D65D);
        g.setColor(new Color(0,0,0,45)); g.fillOval((int)(x+w*.13), (int)(y+h*.82), (int)(w*.75), Math.max(5,(int)(h*.15)));
        g.setColor(green.darker());
        g.fillRoundRect((int)(x+w*.23), (int)(y+h*.52), (int)(w*.54), (int)(h*.44), 14, 14);
        g.setColor(green); g.fillOval((int)(x+w*.12), (int)(y+h*.05), (int)(w*.76), (int)(h*.65));
        g.setColor(new Color(0x102719));
        g.fillOval((int)(x+w*.26), (int)(y+h*.29), (int)(w*.18), (int)(h*.22));
        g.fillOval((int)(x+w*.57), (int)(y+h*.29), (int)(w*.18), (int)(h*.22));
        g.setColor(Color.WHITE); g.fillOval((int)(x+w*.30), (int)(y+h*.32), (int)(w*.06), (int)(h*.07));
        g.fillOval((int)(x+w*.61), (int)(y+h*.32), (int)(w*.06), (int)(h*.07));
        g.setColor(green.darker()); g.drawLine((int)(x+w*.34), (int)(y+h*.03), (int)(x+w*.27), (int)(y-h*.04));
        g.drawLine((int)(x+w*.66), (int)(y+h*.03), (int)(x+w*.73), (int)(y-h*.04));
        g.setColor(new Color(255,100,140)); g.fillOval((int)(x+w*.15), (int)(y+h*.34), 5, 5); g.fillOval((int)(x+w*.78), (int)(y+h*.34), 5, 5);
    }

    void drawRuneStone(Graphics2D g, double x, double y, double w, double h) {
        drawRock(g, x, y, w, h);
        g.setColor(new Color(0xFFE0A0)); g.setStroke(new BasicStroke(2f));
        g.drawLine((int)(x+w*.5), (int)(y+h*.18), (int)(x+w*.5), (int)(y+h*.78));
        g.drawLine((int)(x+w*.3), (int)(y+h*.42), (int)(x+w*.72), (int)(y+h*.42));
        g.drawLine((int)(x+w*.34), (int)(y+h*.65), (int)(x+w*.68), (int)(y+h*.25));
        g.setStroke(new BasicStroke(1f));
    }

    void drawMagmaRock(Graphics2D g, double x, double y, double w, double h) {
        drawRock(g, x, y, w, h);
        g.setColor(new Color(0xFF4A24)); g.setStroke(new BasicStroke(Math.max(2f, (float)(w/24))));
        g.drawLine((int)(x+w*.28), (int)(y+h*.7), (int)(x+w*.47), (int)(y+h*.42));
        g.drawLine((int)(x+w*.47), (int)(y+h*.42), (int)(x+w*.63), (int)(y+h*.62));
        g.drawLine((int)(x+w*.63), (int)(y+h*.62), (int)(x+w*.78), (int)(y+h*.28));
        g.setStroke(new BasicStroke(1f));
    }

    void drawRooftopVent(Graphics2D g, double x, double y, double w, double h) {
        int ix=(int)x, iy=(int)y, iw=(int)w, ih=(int)h;
        g.setColor(new Color(0x414A5D)); g.fillRoundRect(ix, iy+(int)(h*.3), iw, (int)(h*.7), 6, 6);
        g.setColor(new Color(0x8795A9)); g.fillRoundRect(ix+(int)(w*.08), iy+(int)(h*.15), (int)(w*.84), (int)(h*.22), 5, 5);
        g.setColor(new Color(0x252B3A));
        for(int i=1;i<5;i++) g.drawLine(ix+(int)(w*.12), iy+(int)(h*(.35+i*.11)), ix+(int)(w*.88), iy+(int)(h*(.35+i*.11)));
    }

    void drawFlyingHazard(Graphics2D g, double x, double y, double w, double h, Color c) {
        Path2D wings = new Path2D.Double();
        wings.moveTo(x+w*.5,y+h*.48); wings.lineTo(x+w*.06,y+h*.06); wings.lineTo(x+w*.18,y+h*.72); wings.lineTo(x+w*.5,y+h*.82);
        wings.lineTo(x+w*.82,y+h*.72); wings.lineTo(x+w*.94,y+h*.06); wings.closePath();
        g.setColor(c.darker()); g.fill(wings);
        g.setColor(c); g.fillOval((int)(x+w*.37),(int)(y+h*.22),(int)(w*.26),(int)(h*.55));
        g.setColor(Color.BLACK); g.fillOval((int)(x+w*.43),(int)(y+h*.36),4,4); g.fillOval((int)(x+w*.55),(int)(y+h*.36),4,4);
    }

    void drawHayBale(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0xE0B74D)); g.fillRoundRect((int)x,(int)(y+h*.30),(int)w,(int)(h*.70),8,8);
        g.setColor(new Color(0x9D752B)); g.setStroke(new BasicStroke(2f));
        for(int i=1;i<4;i++) g.drawLine((int)(x+w*i/4.0),(int)(y+h*.32),(int)(x+w*i/4.0),(int)(y+h*.97));
        g.setStroke(new BasicStroke(1f));
    }

    void drawFence(Graphics2D g, double x, double y, double w, double h, Color c) {
        g.setColor(c); g.fillRect((int)(x+w*.08),(int)(y+h*.15),(int)(w*.13),(int)(h*.85));
        g.fillRect((int)(x+w*.79),(int)(y+h*.15),(int)(w*.13),(int)(h*.85));
        g.fillRoundRect((int)x,(int)(y+h*.32),(int)w,(int)(h*.17),4,4);
        g.fillRoundRect((int)x,(int)(y+h*.65),(int)w,(int)(h*.17),4,4);
    }

    void drawSnowMound(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0xC9E7F8));
        g.fill(new Ellipse2D.Double(x,y+h*.30,w,h*.70));
        g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(x+w*.12,y+h*.35,w*.58,h*.27));
        g.setColor(new Color(0x80AFCB)); g.drawArc((int)x,(int)(y+h*.35),(int)w,(int)(h*.6),180,160);
    }

    void drawSnowman(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(Color.WHITE);
        g.fillOval((int)(x+w*.12),(int)(y+h*.48),(int)(w*.76),(int)(h*.52));
        g.fillOval((int)(x+w*.22),(int)(y+h*.10),(int)(w*.56),(int)(h*.48));
        g.setColor(new Color(0x3B5367)); g.fillRect((int)(x+w*.2),(int)(y+h*.12),(int)(w*.6),(int)(h*.1));
        g.fillRect((int)(x+w*.32),(int)(y-h*.02),(int)(w*.36),(int)(h*.2));
        g.setColor(new Color(0xE87B35));
        Polygon nose = new Polygon(new int[]{(int)(x+w*.49),(int)(x+w*.80),(int)(x+w*.49)},new int[]{(int)(y+h*.29),(int)(y+h*.34),(int)(y+h*.38)},3); g.fill(nose);
        g.setColor(Color.BLACK); g.fillOval((int)(x+w*.36),(int)(y+h*.25),4,4); g.fillOval((int)(x+w*.59),(int)(y+h*.25),4,4);
    }

    void drawIcicle(Graphics2D g, double x, double y, double w, double h) {
        Path2D p = new Path2D.Double(); p.moveTo(x+w*.05,y); p.lineTo(x+w*.95,y); p.lineTo(x+w*.66,y+h*.58); p.lineTo(x+w*.55,y+h); p.lineTo(x+w*.34,y+h*.53); p.closePath();
        g.setColor(new Color(0xAEEBFF)); g.fill(p); outline(g,p,new Color(0x397FA0));
        g.setColor(new Color(255,255,255,180)); g.drawLine((int)(x+w*.27),(int)(y+h*.12),(int)(x+w*.48),(int)(y+h*.47));
    }

    void drawThornBush(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0x287E42));
        for(int i=0;i<4;i++) g.fillOval((int)(x+w*i*.21),(int)(y+h*.38),(int)(w*.42),(int)(h*.62));
        g.setColor(new Color(0x88E56D));
        for(int i=0;i<4;i++) { int xx=(int)(x+w*(.13+i*.23)); Polygon th=new Polygon(new int[]{xx-5,xx+5,xx},new int[]{(int)(y+h*.48),(int)(y+h*.48),(int)(y+h*.12)},3);g.fill(th); }
        g.setColor(new Color(0xFFB9E6)); g.fillOval((int)(x+w*.36),(int)(y+h*.22),9,9); g.fillOval((int)(x+w*.7),(int)(y+h*.3),7,7);
    }

    void drawButterfly(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0xF28CCD)); g.fillOval((int)(x+w*.12),(int)(y+h*.05),(int)(w*.38),(int)(h*.7));
        g.setColor(new Color(0x8B78FF)); g.fillOval((int)(x+w*.5),(int)(y+h*.05),(int)(w*.38),(int)(h*.7));
        g.setColor(new Color(0x6B3B74)); g.fillRoundRect((int)(x+w*.45),(int)(y+h*.18),(int)(w*.12),(int)(h*.65),6,6);
        g.drawArc((int)(x+w*.42),(int)(y-h*.08),(int)(w*.18),(int)(h*.4),25,120);
    }

    void drawLeafPile(Graphics2D g, double x, double y, double w, double h) {
        Color[] cs={new Color(0xC95E32),new Color(0xE0A638),new Color(0x7AAB49)};
        for(int i=0;i<7;i++){ double xx=x+(i%4)*w/4.5, yy=y+h*(.4+(i%3)*.08); g.setColor(cs[i%cs.length]); g.fill(new Ellipse2D.Double(xx,yy,w*.27,h*.45)); }
    }

    void drawSeasonTree(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0x76502F)); g.fillRect((int)(x+w*.42),(int)(y+h*.38),(int)(w*.18),(int)(h*.62));
        g.setColor(new Color(0xC66C3E)); g.fillOval((int)(x+w*.10),(int)(y+h*.05),(int)(w*.8),(int)(h*.55));
        g.setColor(new Color(0xEACB5A)); g.fillOval((int)(x+w*.22),(int)(y+h*.18),(int)(w*.48),(int)(h*.3));
    }

    void drawStalagmite(Graphics2D g, double x, double y, double w, double h) {
        Path2D p=new Path2D.Double(); p.moveTo(x,y+h);p.lineTo(x+w*.18,y+h*.48);p.lineTo(x+w*.42,y+h*.22);p.lineTo(x+w*.54,y);p.lineTo(x+w*.7,y+h*.43);p.lineTo(x+w,y+h);p.closePath();
        g.setPaint(new GradientPaint((float)x,(float)y,new Color(0xAAA1AE),(float)(x+w),(float)(y+h),new Color(0x514B5A)));g.fill(p);outline(g,p,new Color(0x302B36));
    }

    void drawBat(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0x342448));
        Polygon wings=new Polygon(new int[]{(int)(x+w*.5),(int)(x+w*.27),(int)(x+w*.08),(int)(x+w*.22),(int)(x+w*.4),(int)(x+w*.5),(int)(x+w*.6),(int)(x+w*.78),(int)(x+w*.92),(int)(x+w*.73)},new int[]{(int)(y+h*.52),(int)(y+h*.18),(int)(y+h*.3),(int)(y+h*.83),(int)(y+h*.62),(int)(y+h*.78),(int)(y+h*.62),(int)(y+h*.83),(int)(y+h*.3),(int)(y+h*.18)},10);g.fillPolygon(wings);
        g.setColor(new Color(0xFF5050));g.fillOval((int)(x+w*.4),(int)(y+h*.35),5,5);g.fillOval((int)(x+w*.55),(int)(y+h*.35),5,5);
    }

    void drawAlienPod(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0x7546A8)); g.fillOval((int)(x+w*.14),(int)(y+h*.30),(int)(w*.72),(int)(h*.7));
        g.setColor(new Color(0x55E7BB)); g.fillOval((int)(x+w*.25),(int)(y+h*.45),(int)(w*.5),(int)(h*.24));
        g.setColor(new Color(0xC5FF79)); g.fillOval((int)(x+w*.35),(int)(y+h*.12),(int)(w*.3),(int)(h*.45));
        g.setColor(new Color(0x321B4A)); g.drawOval((int)(x+w*.14),(int)(y+h*.3),(int)(w*.72),(int)(h*.7));
    }

    void drawTire(Graphics2D g, double x, double y, double w, double h) {
        double d=Math.min(w,h);
        g.setColor(new Color(0x303238));g.fillOval((int)(x+(w-d)/2),(int)(y+h-d),(int)d,(int)d);
        g.setColor(new Color(0x777B80));g.fillOval((int)(x+(w-d)/2+d*.27),(int)(y+h-d+d*.27),(int)(d*.46),(int)(d*.46));
        g.setColor(new Color(0x151719));g.drawOval((int)(x+(w-d)/2),(int)(y+h-d),(int)d,(int)d);
        g.setColor(new Color(0xA0A4A8));g.drawOval((int)(x+(w-d)/2+d*.38),(int)(y+h-d+d*.38),(int)(d*.24),(int)(d*.24));
    }

    void drawScrapPile(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0x78563A));g.fillRoundRect((int)x,(int)(y+h*.5),(int)w,(int)(h*.5),7,7);
        g.setColor(new Color(0xA5A9AA));g.fillRect((int)(x+w*.08),(int)(y+h*.32),(int)(w*.34),(int)(h*.24));
        g.setColor(new Color(0x6F8790));g.fillRect((int)(x+w*.46),(int)(y+h*.18),(int)(w*.28),(int)(h*.32));
        g.setColor(new Color(0xD18D48));g.fillOval((int)(x+w*.62),(int)(y+h*.04),(int)(w*.2),(int)(h*.25));
        g.setColor(Color.DARK_GRAY);g.drawRect((int)(x+w*.08),(int)(y+h*.32),(int)(w*.34),(int)(h*.24));
    }

    void drawCrystal(Graphics2D g, double x, double y, double w, double h, Color c) {
        Path2D p=new Path2D.Double();p.moveTo(x+w*.5,y);p.lineTo(x+w*.9,y+h*.27);p.lineTo(x+w*.76,y+h*.85);p.lineTo(x+w*.5,y+h);p.lineTo(x+w*.18,y+h*.79);p.lineTo(x+w*.08,y+h*.28);p.closePath();
        g.setPaint(new GradientPaint((float)x,(float)y,Color.WHITE,(float)(x+w),(float)(y+h),c));g.fill(p);outline(g,p,c.darker());
        g.setColor(new Color(255,255,255,150));g.drawLine((int)(x+w*.5),(int)y,(int)(x+w*.48),(int)(y+h*.82));
    }

    void drawRainbowCloud(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(255,255,255,225));g.fillOval((int)(x+w*.05),(int)(y+h*.34),(int)(w*.48),(int)(h*.56));g.fillOval((int)(x+w*.28),(int)(y+h*.1),(int)(w*.45),(int)(h*.8));g.fillOval((int)(x+w*.55),(int)(y+h*.3),(int)(w*.4),(int)(h*.6));
        Color[] cs={Color.RED,new Color(255,165,0),Color.YELLOW,new Color(0,200,100),new Color(50,130,255),new Color(160,80,220)};
        for(int i=0;i<cs.length;i++){g.setColor(cs[i]);g.fillOval((int)(x+w*(.13+i*.12)),(int)(y+h*.62),(int)(w*.13),(int)(h*.22));}
    }

    void drawTombstone(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0x777087));g.fillRoundRect((int)(x+w*.12),(int)(y+h*.15),(int)(w*.76),(int)(h*.85),12,12);
        g.setColor(new Color(0xB9B1C9));g.drawRoundRect((int)(x+w*.12),(int)(y+h*.15),(int)(w*.76),(int)(h*.85),12,12);
        g.setColor(new Color(0x33263C));g.setFont(new Font("Monospaced",Font.BOLD,Math.max(9,(int)(w*.28))));g.drawString("RIP",(int)(x+w*.28),(int)(y+h*.62));
    }

    void drawGhost(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0xDAD7FF));
        g.fillOval((int)(x+w*.2),(int)(y+h*.04),(int)(w*.6),(int)(h*.56));
        Path2D body=new Path2D.Double();body.moveTo(x+w*.2,y+h*.34);body.lineTo(x+w*.2,y+h*.82);body.lineTo(x+w*.34,y+h*.72);body.lineTo(x+w*.48,y+h*.84);body.lineTo(x+w*.64,y+h*.72);body.lineTo(x+w*.8,y+h*.82);body.lineTo(x+w*.8,y+h*.34);body.closePath();g.fill(body);
        g.setColor(new Color(0x392344));g.fillOval((int)(x+w*.35),(int)(y+h*.25),5,7);g.fillOval((int)(x+w*.6),(int)(y+h*.25),5,7);
    }

    void drawCandyCane(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(Color.WHITE);g.setStroke(new BasicStroke(Math.max(5f,(float)(w*.2)),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        Path2D p=new Path2D.Double();p.moveTo(x+w*.28,y+h);p.lineTo(x+w*.28,y+h*.25);p.curveTo(x+w*.28,y-h*.02,x+w*.82,y-h*.02,x+w*.78,y+h*.28);g.draw(p);
        g.setColor(new Color(0xE34C5D));g.setStroke(new BasicStroke(Math.max(3f,(float)(w*.11)),BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND));
        for(int i=0;i<4;i++)g.drawLine((int)(x+w*.18),(int)(y+h*(.35+i*.14)),(int)(x+w*.39),(int)(y+h*(.26+i*.14)));
        g.setStroke(new BasicStroke(1f));
    }

    void drawOrnament(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0xC9E8F7));g.fillOval((int)(x+w*.28),(int)(y+h*.17),(int)(w*.45),(int)(h*.75));
        g.setColor(new Color(0xD94A55));g.fillRect((int)(x+w*.4),(int)(y+h*.03),(int)(w*.2),(int)(h*.2));
        g.setColor(new Color(0xF7D76B));g.drawOval((int)(x+w*.37),(int)(y+h*.28),(int)(w*.27),(int)(h*.4));
    }

    void drawToxicBarrel(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0x38613A));g.fillRoundRect((int)(x+w*.14),(int)(y+h*.12),(int)(w*.72),(int)(h*.88),7,7);
        g.setColor(new Color(0x7FFF32));g.fillRect((int)(x+w*.16),(int)(y+h*.38),(int)(w*.68),(int)(h*.18));
        g.setColor(new Color(0x1A2B1A));g.setStroke(new BasicStroke(2f));g.drawRoundRect((int)(x+w*.14),(int)(y+h*.12),(int)(w*.72),(int)(h*.88),7,7);
        text(g,"TOX",x+w*.5,y+h*.69,Math.max(8,(int)(w*.17)),Color.WHITE);g.setStroke(new BasicStroke(1f));
    }

    void drawToxicDrone(Graphics2D g, double x, double y, double w, double h) {
        drawCyberDrone(g,x,y,w,h,new Color(0x7FFF32));
        g.setColor(new Color(0x7FFF32));
        g.fillOval((int)(x+w*.43),(int)(y+h*.15), (int)(w*.14),(int)(h*.7));
    }

    void drawArenaBlock(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0x8B6A3E));g.fillRect((int)(x+w*.08),(int)(y+h*.44),(int)(w*.84),(int)(h*.56));
        g.setColor(new Color(0xE0C47A));g.fillRect((int)(x+w*.02),(int)(y+h*.32),(int)(w*.96),(int)(h*.18));
        g.setColor(new Color(0x4B3626));g.drawRect((int)(x+w*.08),(int)(y+h*.44),(int)(w*.84),(int)(h*.56));
    }

    void drawArenaPillar(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0x92744D));g.fillRect((int)(x+w*.18),(int)(y+h*.12),(int)(w*.64),(int)(h*.88));
        g.setColor(new Color(0xD5B77A));g.fillRect((int)(x+w*.08),(int)(y+h*.08),(int)(w*.84),(int)(h*.15));
        g.fillRect((int)(x+w*.08),(int)(y+h*.83),(int)(w*.84),(int)(h*.15));
        g.setColor(new Color(0x60462C));g.drawRect((int)(x+w*.18),(int)(y+h*.12),(int)(w*.64),(int)(h*.88));
    }

    void drawFallenLog(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0x795033));g.fillRoundRect((int)x,(int)(y+h*.42),(int)w,(int)(h*.58),12,12);
        g.setColor(new Color(0xB3834D));g.fillOval((int)(x+w*.72),(int)(y+h*.45),(int)(w*.26),(int)(h*.5));
        g.setColor(new Color(0x47301E));g.drawOval((int)(x+w*.77),(int)(y+h*.51),(int)(w*.16),(int)(h*.34));
        g.setColor(new Color(0x3E7D35));g.drawArc((int)(x+w*.12),(int)(y+h*.37),(int)(w*.22),(int)(h*.5),15,140);
    }

    void drawLaserBarrier(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0x133E53));g.fillRoundRect((int)x,(int)(y+h*.72),(int)w,(int)(h*.28),6,6);
        g.setColor(new Color(0x00E5FF));g.setStroke(new BasicStroke(3f));
        g.drawLine((int)(x+w*.1),(int)(y+h*.16),(int)(x+w*.9),(int)(y+h*.16));
        g.drawLine((int)(x+w*.22),(int)(y+h*.3),(int)(x+w*.78),(int)(y+h*.3));
        g.drawLine((int)(x+w*.1),(int)(y+h*.45),(int)(x+w*.9),(int)(y+h*.45));
        g.setStroke(new BasicStroke(1f));
    }

    void drawSwampStump(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0x57452C));g.fillRoundRect((int)(x+w*.1),(int)(y+h*.3),(int)(w*.8),(int)(h*.7),7,7);
        g.setColor(new Color(0x9C8D53));g.fillOval((int)(x+w*.12),(int)(y+h*.2),(int)(w*.76),(int)(h*.28));
        g.setColor(new Color(0x4B3923));g.drawOval((int)(x+w*.28),(int)(y+h*.24),(int)(w*.43),(int)(h*.17));
        g.setColor(new Color(0x6B8A42));g.fillOval((int)(x+w*.02),(int)(y+h*.58),(int)(w*.25),(int)(h*.23));
    }

    void drawSlime(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0x6BAA49));g.fillOval((int)(x+w*.08),(int)(y+h*.3),(int)(w*.84),(int)(h*.68));
        g.setColor(new Color(0x9EE65D));g.fillOval((int)(x+w*.22),(int)(y+h*.2),(int)(w*.52),(int)(h*.42));
        g.setColor(new Color(0x20331E));g.fillOval((int)(x+w*.3),(int)(y+h*.42),5,7);g.fillOval((int)(x+w*.61),(int)(y+h*.42),5,7);
        g.setColor(new Color(0xD6F8A2));g.fillOval((int)(x+w*.27),(int)(y+h*.28),7,4);
    }

    void drawDragonfly(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(185,232,205,160));g.fillOval((int)(x+w*.10),(int)(y+h*.10),(int)(w*.34),(int)(h*.3));g.fillOval((int)(x+w*.55),(int)(y+h*.10),(int)(w*.34),(int)(h*.3));
        g.setColor(new Color(0x357C63));g.fillRoundRect((int)(x+w*.4),(int)(y+h*.35),(int)(w*.48),(int)(h*.24),8,8);g.fillOval((int)(x+w*.3),(int)(y+h*.3),(int)(w*.23),(int)(h*.34));
        g.setColor(new Color(0xD0E75B));g.fillOval((int)(x+w*.34),(int)(y+h*.37),4,4);
    }

    void drawTrafficCone(Graphics2D g, double x, double y, double w, double h) {
        Polygon p=new Polygon(new int[]{(int)(x+w*.5),(int)(x+w*.9),(int)(x+w*.1)},new int[]{(int)y,(int)(y+h*.9),(int)(y+h*.9)},3);
        g.setColor(new Color(0xF07824));g.fillPolygon(p);g.setColor(Color.WHITE);g.fillRect((int)(x+w*.23),(int)(y+h*.55),(int)(w*.54),(int)(h*.13));
        g.setColor(new Color(0x373A40));g.fillRoundRect((int)(x+w*.04),(int)(y+h*.86),(int)(w*.92),(int)(h*.14),5,5);
    }

    void drawRoadBarrier(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0xF5F2EA));g.fillRoundRect((int)x,(int)(y+h*.28),(int)w,(int)(h*.65),5,5);
        g.setColor(new Color(0xEA4A38));
        for(int i=-1;i<5;i++) { Polygon stripe=new Polygon(new int[]{(int)(x+i*w*.28),(int)(x+(i+.5)*w*.28),(int)(x+(i+.5)*w*.28),(int)(x+i*w*.28)},new int[]{(int)(y+h*.3),(int)(y+h*.3),(int)(y+h*.9),(int)(y+h*.9)},4);g.fillPolygon(stripe); }
        g.setColor(new Color(0x42464D));g.fillRect((int)(x+w*.08),(int)(y+h*.88),(int)(w*.84),(int)(h*.12));
    }

    void drawFlyingCar(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(0xE5E7EB));g.fillRoundRect((int)(x+w*.08),(int)(y+h*.34),(int)(w*.84),(int)(h*.48),11,11);
        g.setColor(new Color(0x3BA5D9));g.fillRoundRect((int)(x+w*.28),(int)(y+h*.1),(int)(w*.43),(int)(h*.37),9,9);
        g.setColor(new Color(0x202938));g.fillOval((int)(x+w*.16),(int)(y+h*.68),(int)(w*.2),(int)(h*.3));g.fillOval((int)(x+w*.65),(int)(y+h*.68),(int)(w*.2),(int)(h*.3));
        g.setColor(new Color(0xFFDF5D));g.fillOval((int)(x+w*.78),(int)(y+h*.46),5,5);
    }

    void drawFireOrb(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(255,62,12,65));g.fillOval((int)(x-5),(int)(y-5),(int)(w+10),(int)(h+10));
        g.setPaint(new GradientPaint((float)x,(float)y,new Color(0xFFF17A),(float)(x+w),(float)(y+h),new Color(0xDF230D)));
        g.fillOval((int)(x+w*.15),(int)(y+h*.02),(int)(w*.7),(int)(h*.9));
        g.setColor(new Color(0xFFFAE0));g.fillOval((int)(x+w*.4),(int)(y+h*.3),(int)(w*.2),(int)(h*.35));
    }

    void drawBatPlaceholder(Graphics2D g, double x, double y, double w, double h) { drawBat(g,x,y,w,h); }

    void drawFlowerPlaceholder(Graphics2D g, double x, double y, double w, double h) { drawThornBush(g,x,y,w,h); }

    void drawFlyingLeaves(Graphics2D g, double x, double y, double w, double h) {
        drawLeafPile(g,x,y,w,h);
    }

    Color rockColor() {
        switch (theme) {
            case NEON:          return new Color(0x38BFCB);
            case DESSERT:       return new Color(0xB87942);
            case MOON:          return new Color(0x8A8CA8);
            case RAGNAROK:      return new Color(0x7D6A5A);
            case VOLCANO:       return new Color(0x65333A);
            case ROOFTOPS:      return new Color(0x62667F);
            case JUNKYARD:      return new Color(0x78563A);
            case HAUNTED:       return new Color(0x4F3B63);
            case NUCLEAR_PLANT: return new Color(0x28592B);
            case ARENA:         return new Color(0x8C7443);
            case JUNGLE:        return new Color(0x27592B);
            case PROGNOSIN:     return new Color(0x0E5166);
            case BOGLAND:       return new Color(0x3D4F38);
            default:            return new Color(0x8C8C94);
        }
    }

    void drawRock(Graphics2D g, double x, double y, double w, double h) {
        Color base = rockColor();
        Path2D p = new Path2D.Double();
        p.moveTo(x, y + h);
        p.lineTo(x + w * 0.04, y + h * 0.55);
        p.lineTo(x + w * 0.22, y + h * 0.15);
        p.lineTo(x + w * 0.55, y);
        p.lineTo(x + w * 0.82, y + h * 0.22);
        p.lineTo(x + w, y + h * 0.62);
        p.lineTo(x + w * 0.97, y + h);
        p.closePath();
        g.setPaint(new GradientPaint((float) x, (float) y, base.brighter(), (float) (x + w), (float) (y + h), base.darker()));
        g.fill(p);

        Path2D hi = new Path2D.Double();
        hi.moveTo(x + w * 0.22, y + h * 0.15);
        hi.lineTo(x + w * 0.55, y);
        hi.lineTo(x + w * 0.5, y + h * 0.35);
        hi.lineTo(x + w * 0.18, y + h * 0.5);
        hi.closePath();
        g.setColor(new Color(255, 255, 255, 55));
        g.fill(hi);

        if (theme == Theme.MOON || theme == Theme.NORTHPOLE) {
            Path2D s = new Path2D.Double();
            s.moveTo(x + w * 0.22, y + h * 0.15);
            s.lineTo(x + w * 0.55, y);
            s.lineTo(x + w * 0.82, y + h * 0.22);
            s.lineTo(x + w * 0.65, y + h * 0.32);
            s.lineTo(x + w * 0.5, y + h * 0.22);
            s.lineTo(x + w * 0.35, y + h * 0.34);
            s.closePath();
            g.setColor(Color.WHITE);
            g.fill(s);
        }
        g.setStroke(new BasicStroke(1.8f));
        g.setColor(base.darker().darker());
        g.draw(p);
        g.setStroke(new BasicStroke(1f));
    }

    void drawShip(Graphics2D g, double x, double y, double w, double h) {
        double fl = 12 + (frame % 6) * 3;
        Path2D f = new Path2D.Double();
        f.moveTo(x + w * 0.9, y + h * 0.55);
        f.lineTo(x + w + fl, y + h * 0.7);
        f.lineTo(x + w * 0.9, y + h * 0.88);
        f.closePath();
        g.setColor(new Color(255, 150, 30, 220));
        g.fill(f);
        Path2D f2 = new Path2D.Double();
        f2.moveTo(x + w * 0.9, y + h * 0.62);
        f2.lineTo(x + w + fl * 0.5, y + h * 0.7);
        f2.lineTo(x + w * 0.9, y + h * 0.8);
        f2.closePath();
        g.setColor(new Color(255, 240, 120, 230));
        g.fill(f2);

        Arc2D dome = new Arc2D.Double(x + w * 0.28, y, w * 0.44, h * 0.8, 0, 180, Arc2D.CHORD);
        g.setColor(new Color(150, 230, 255, 230));
        g.fill(dome);
        g.setColor(new Color(0x2A5A7A));
        g.setStroke(new BasicStroke(1.8f));
        g.draw(dome);

        Ellipse2D saucer = new Ellipse2D.Double(x, y + h * 0.4, w, h * 0.6);
        g.setPaint(new GradientPaint((float) x, (float) (y + h * 0.4), new Color(0xE0E0EA),
                (float) x, (float) (y + h), new Color(0x6B6B85)));
        g.fill(saucer);
        g.setColor(new Color(0x2B2B3F));
        g.draw(saucer);
        g.setStroke(new BasicStroke(1f));

        for (int k = 0; k < 3; k++) {
            boolean on = ((frame / 8) + k) % 3 == 0;
            g.setColor(on ? new Color(0xFFE94D) : new Color(0x7A3B3B));
            g.fill(new Ellipse2D.Double(x + w * (0.2 + 0.3 * k) - 4, y + h * 0.62, 8, 8));
        }
    }

    // ---- Paw characters: each index has its own palette and recognizable animal details.
    void drawPlayer(Graphics2D g, double cx, double feetY, double s, int idx,
                    boolean running, boolean duck, int fr) {
        AffineTransform old = g.getTransform();
        Stroke oldStroke = g.getStroke();
        idx = Math.floorMod(idx, NAMES.length);
        g.translate(cx, feetY);
        g.scale(s, duck ? s * 0.58 : s);
        int step = running ? (fr / 6) % 2 : 0;
        int liftL = (running && step == 0) ? 5 : 0, liftR = (running && step == 1) ? 5 : 0;
        Color outline = new Color(0x302A35), body = BODY[idx], feet = FEET[idx];
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Tails / ears / feathers vary by character.
        if (idx == 0) { // Sonic quills
            int[] qx = {-17,-40,-35,-53,-24,-29,-8,-5}; int[] qy = {-48,-67,-48,-51,-32,-18,-25,-45};
            g.setColor(body); g.fillPolygon(qx,qy,qx.length); g.setColor(outline); g.drawPolygon(qx,qy,qx.length);
        } else if (idx == 1) { // Kurama's flowing fox tail
            Path2D tail = new Path2D.Double(); tail.moveTo(-20,-31); tail.curveTo(-54,-31,-60,-62,-48,-78); tail.curveTo(-43,-57,-26,-57,-14,-47); tail.closePath();
            g.setColor(body); g.fill(tail); g.setColor(outline); g.draw(tail);
            g.setColor(new Color(0xFFE8CB)); g.fillOval(-48,-76,8,11); g.fillOval(-36,-62,8,10);
        } else if (idx == 2 || idx == 7 || idx == 12 || idx == 18) { // canine / deer-like ears
            int[] a={-20,-23,-10,-4}, b={-48,-79,-68,-50};
            g.setColor(body); g.fillPolygon(a,b,4); g.setColor(outline); g.drawPolygon(a,b,4);
            int[] c={1,5,12,16}, d={-51,-82,-68,-48}; g.setColor(body); g.fillPolygon(c,d,4); g.setColor(outline); g.drawPolygon(c,d,4);
        } else if (idx == 3) { // Cotton rabbit ears
            g.setColor(body); g.fillRoundRect(-13,-91,12,38,8,8); g.fillRoundRect(2,-94,12,41,8,8);
            g.setColor(new Color(0xEBA5B5)); g.fillRoundRect(-9,-86,4,27,4,4); g.fillRoundRect(6,-88,4,29,4,4);
        } else if (idx == 4 || idx == 14) { // Raccoon / skunk striped tail
            g.setColor(body); g.fillOval(-54,-42,36,18); g.setColor(new Color(0xD8D3C9)); g.fillOval(-49,-41,8,15); g.fillOval(-36,-40,7,13);
        } else if (idx == 5) { // Bruno bear round ears
            g.setColor(body); g.fillOval(-22,-67,17,17); g.fillOval(8,-67,17,17);
        } else if (idx == 6 || idx == 15) { // squirrel / mouse tail
            g.setColor(body); g.fillOval(-49,-57,28,35); g.setColor(outline); g.drawOval(-49,-57,28,35);
        } else if (idx == 9) { // Shiva snow leopard ears and spots
            g.setColor(body); g.fillOval(-20,-67,15,15); g.fillOval(8,-67,15,15);
        } else if (idx == 10 || idx == 17) { // bird / owl feather tufts
            int[] a={-17,-13,-7,-2}, b={-55,-76,-62,-53}; g.setColor(body); g.fillPolygon(a,b,4); g.setColor(outline); g.drawPolygon(a,b,4);
        } else if (idx == 13 || idx == 16) { // cat / lynx ears
            int[] a={-19,-21,-7,-4}, b={-48,-76,-65,-49}; g.setColor(body); g.fillPolygon(a,b,4); g.setColor(outline); g.drawPolygon(a,b,4);
            int[] c={4,10,17,17}, d={-49,-73,-62,-46}; g.setColor(body); g.fillPolygon(c,d,4); g.setColor(outline); g.drawPolygon(c,d,4);
        } else {
            g.setColor(body); g.fillOval(-48,-43,26,13); g.setColor(outline); g.drawOval(-48,-43,26,13);
            g.setColor(body); g.fillOval(-20,-66,14,14); g.fillOval(8,-66,14,14);
        }

        // Legs and body
        g.setColor(feet); g.fillRoundRect(-24,-10-liftL,22,11,6,6); g.fillRoundRect(7,-10-liftR,22,11,6,6);
        g.setColor(outline); g.drawRoundRect(-24,-10-liftL,22,11,6,6); g.drawRoundRect(7,-10-liftR,22,11,6,6);
        g.setColor(body); g.fillOval(-27,-62,55,49); g.setColor(outline); g.drawOval(-27,-62,55,49);

        // Chest / muzzle and facial features
        Color muzzle = (idx == 3 || idx == 8) ? new Color(0xFFF9F1) : new Color(0xE9D0B4);
        g.setColor(muzzle); g.fillOval(6,-42,25,17);
        if (idx == 1 || idx == 5 || idx == 9) { g.setColor(new Color(0xFFF0D9)); g.fillOval(-9,-31,28,15); }
        if (idx == 10 || idx == 17) { // simple beak for Helios and Atlas
            g.setColor(new Color(0xE8B83F)); int[] bx={25,39,26}, by={-43,-38,-34}; g.fillPolygon(bx,by,3);
        }
        g.setColor(Color.WHITE); g.fillOval(8,-55,13,17);
        g.setColor(new Color(0x26334A)); g.fillOval(15,-50,6,9); g.setColor(Color.WHITE); g.fillOval(17,-49,2,3);
        g.setColor(outline); g.fillOval(29,-36,5,4);
        if (idx == 0) { g.setColor(new Color(0xF7D6B5)); g.fillOval(2,-43,24,17); g.setColor(new Color(0xE83B32)); g.fillRoundRect(5,-7,23,7,5,5); }
        if (idx == 2) { g.setColor(new Color(0x686E75)); g.fillOval(-23,-64,43,11); g.setColor(new Color(0xE6C75B)); g.fillOval(11,-49,4,4); }
        if (idx == 3) { g.setColor(Color.WHITE); g.fillOval(-38,-34,18,18); g.setColor(outline); g.drawOval(-38,-34,18,18); g.setColor(new Color(0xE99AAF)); g.fillOval(29,-36,5,4); }
        if (idx == 4 || idx == 14) { g.setColor(new Color(0x272727)); g.fillOval(5,-52,20,8); g.setColor(Color.WHITE); g.fillOval(12,-53,5,5); }
        if (idx == 9) { g.setColor(new Color(0x74777B)); for(int i=0;i<5;i++) g.fillOval(-20+i*8,-56+(i%2)*7,4,4); }
        g.setStroke(oldStroke); g.setTransform(old);
    }

    // ---- UI helpers
    void text(Graphics2D g, String s, double cx, double cy, int size, Color c) {
        g.setFont(new Font("Monospaced", Font.BOLD, size));
        FontMetrics fm = g.getFontMetrics();
        float x = (float) (cx - fm.stringWidth(s) / 2.0);
        float y = (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0);
        g.setColor(c);
        g.drawString(s, x, y);
    }

    void drawButton(Graphics2D g, Rectangle r, String label, int style, int fontSize) {
        boolean hover = r.contains(mouse);
        Color main, light, glow;
        if (style == GREEN) {
            main = new Color(0x3DBE2A); light = new Color(0xA6F55A); glow = new Color(0x7CFF5A);
        } else if (style == RED) {
            main = new Color(0xC40000); light = new Color(0xFF3030); glow = new Color(0xFF2A2A);
        } else if (style == YELLOW) {
            main = new Color(0xE6B800); light = new Color(0xFFF176); glow = new Color(0xFFD740);
        } else if (style == BLUE) {
            main = new Color(0x1565C0); light = new Color(0x64B5F6); glow = new Color(0x42A5F5);
        } else if (style == GRAY) {
            main = new Color(0x616161); light = new Color(0xBDBDBD); glow = new Color(0x9E9E9E);
        } else {
            main = new Color(255, 255, 255, hover ? 60 : 35);
            light = new Color(255, 255, 255, hover ? 90 : 60);
            glow = new Color(0x7CFF5A);
        }
        int arc = (style == GREEN) ? r.height : (int) (r.height * 0.45);

        for (int i = 7; i >= 1; i--) {
            g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), hover ? 30 : 18));
            g.setStroke(new BasicStroke(i * 2.4f));
            g.drawRoundRect(r.x, r.y, r.width, r.height, arc, arc);
        }
        g.setPaint(new GradientPaint(0, r.y, light, 0, r.y + r.height, main));
        g.fillRoundRect(r.x, r.y, r.width, r.height, arc, arc);
        g.setColor(new Color(255, 255, 255, 70));
        g.fillRoundRect(r.x + 6, r.y + 3, r.width - 12, r.height / 3, arc, arc);
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(255, 255, 255, 170));
        g.drawRoundRect(r.x, r.y, r.width, r.height, arc, arc);
        g.setStroke(new BasicStroke(1f));
        text(g, label, r.getCenterX(), r.getCenterY(), fontSize, Color.BLACK);
    }

    // ------------------------------------------------------------ input
    @Override public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        boolean jump = k == KeyEvent.VK_SPACE || k == KeyEvent.VK_UP || k == KeyEvent.VK_W;
        boolean down = k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S;
        switch (state) {
            case MENU:
                if (k == KeyEvent.VK_ENTER || jump) state = State.SELECT;
                else if (k == KeyEvent.VK_S) state = State.SETTINGS;
                else if (k == KeyEvent.VK_P) { trackerOffset = 0; state = State.TRACKER; }
                else if (k == KeyEvent.VK_T) state = State.THEME;
                else if (k == KeyEvent.VK_ESCAPE) System.exit(0);
                break;
            case SETTINGS:
                if (k == KeyEvent.VK_1) setDifficulty(Difficulty.DEFAULT);
                else if (k == KeyEvent.VK_2) setDifficulty(Difficulty.EASY);
                else if (k == KeyEvent.VK_3) setDifficulty(Difficulty.MEDIUM);
                else if (k == KeyEvent.VK_4) setDifficulty(Difficulty.HARD);
                else if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) {
                    setDifficulty(Difficulty.values()[Math.floorMod(difficulty.ordinal() - 1, Difficulty.values().length)]);
                } else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) {
                    setDifficulty(Difficulty.values()[(difficulty.ordinal() + 1) % Difficulty.values().length]);
                } else if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_ENTER || k == KeyEvent.VK_BACK_SPACE) state = State.MENU;
                break;
            case TRACKER:
                if (k == KeyEvent.VK_UP) trackerOffset = Math.max(0, trackerOffset - 1);
                else if (k == KeyEvent.VK_DOWN) trackerOffset = Math.min(Math.max(0, performanceHistory.size() - 8), trackerOffset + 1);
                else if (k == KeyEvent.VK_PAGE_UP) trackerOffset = Math.max(0, trackerOffset - 8);
                else if (k == KeyEvent.VK_PAGE_DOWN) trackerOffset = Math.min(Math.max(0, performanceHistory.size() - 8), trackerOffset + 8);
                else if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_ENTER || k == KeyEvent.VK_BACK_SPACE) state = State.MENU;
                break;
            case SELECT:
                if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) selected = (selected + NAMES.length - 1) % NAMES.length;
                else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) selected = (selected + 1) % NAMES.length;
                else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) state = State.THEME;
                else if (k == KeyEvent.VK_ESCAPE) state = State.MENU;
                break;
            case THEME:
                if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) themeChoice = (themeChoice + NUM_THEMES - 1) % NUM_THEMES;
                else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) themeChoice = (themeChoice + 1) % NUM_THEMES;
                else if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W) themeChoice = (themeChoice + NUM_THEMES - 6) % NUM_THEMES;
                else if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) themeChoice = (themeChoice + 6) % NUM_THEMES;
                else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) startGame();
                else if (k == KeyEvent.VK_ESCAPE) state = State.SELECT;
                break;
            case PLAY:
                if (themeMenu) { themeMenuKey(k); break; }
                if (k == KeyEvent.VK_SPACE) { jumpQueued = true; holdSpace = true; }
                if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W) { upQueued = true; holdUp = true; }
                if (down) { downHeld = true; dropQueued = true; }
                if (k == KeyEvent.VK_P) paused = !paused;
                if (k == KeyEvent.VK_T) openThemeMenu();
                if (k == KeyEvent.VK_ESCAPE) { recordRun(); state = State.MENU; }
                break;
            case OVER:
                if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) startGame();
                else if (k == KeyEvent.VK_ESCAPE) state = State.MENU;
                break;
        }
    }

    @Override public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) downHeld = false;
        if (k == KeyEvent.VK_SPACE) holdSpace = false;
        if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W) holdUp = false;
    }

    @Override public void keyTyped(KeyEvent e) { }

    @Override public void mousePressed(MouseEvent e) {
        Point p = e.getPoint();
        if (state == State.SELECT) selectionDragStartX = e.getX();
        switch (state) {
            case MENU:
                if (menuStart.contains(p)) state = State.SELECT;
                else if (menuStage.contains(p)) state = State.THEME;
                else if (menuTracker.contains(p)) { trackerOffset = 0; state = State.TRACKER; }
                else if (menuSettings.contains(p)) state = State.SETTINGS;
                else if (menuExit.contains(p)) System.exit(0);
                break;
            case SETTINGS:
                if (settingsDefault.contains(p)) setDifficulty(Difficulty.DEFAULT);
                else if (settingsEasy.contains(p)) setDifficulty(Difficulty.EASY);
                else if (settingsMedium.contains(p)) setDifficulty(Difficulty.MEDIUM);
                else if (settingsHard.contains(p)) setDifficulty(Difficulty.HARD);
                else if (settingsBack.contains(p)) state = State.MENU;
                break;
            case TRACKER:
                if (trackerBack.contains(p)) state = State.MENU;
                else if (trackerPrev.contains(p)) trackerOffset = Math.max(0, trackerOffset - 8);
                else if (trackerNext.contains(p)) trackerOffset = Math.min(Math.max(0, performanceHistory.size() - 8), trackerOffset + 8);
                break;
            case SELECT:
                if (selLeft.contains(p)) selected = (selected + NAMES.length - 1) % NAMES.length;
                else if (selRight.contains(p)) selected = (selected + 1) % NAMES.length;
                else if (selStart.contains(p)) state = State.THEME;
                else if (selBack.contains(p)) state = State.MENU;
                break;
            case THEME:
                for (int i = 0; i < NUM_THEMES; i++) if (cardRect(i).contains(p)) themeChoice = i;
                if (themeStart.contains(p)) startGame();
                else if (themeBack.contains(p)) state = State.SELECT;
                break;
            case PLAY:
                if (themeMenu) {
                    for (int i = 0; i < NUM_THEMES; i++) if (cardRect(i).contains(p)) { applyTheme(i); return; }
                    if (menuResume.contains(p)) closeThemeMenu();
                } else if (changeBtn.contains(p)) {
                    openThemeMenu();
                } else {
                    jumpQueued = true;
                    holdMouse = true;
                }
                break;
            case OVER:
                if (overAgain.contains(p)) startGame();
                else if (overMenu.contains(p)) state = State.MENU;
                break;
        }
    }

    @Override public void mouseReleased(MouseEvent e) {
        holdMouse = false;
        if (state == State.SELECT && selectionDragStartX >= 0) {
            int delta = e.getX() - selectionDragStartX;
            if (Math.abs(delta) > 45) {
                if (delta < 0) selected = (selected + 1) % NAMES.length;
                else selected = (selected + NAMES.length - 1) % NAMES.length;
            }
            selectionDragStartX = -1;
        }
    }
    @Override public void mouseMoved(MouseEvent e) { mouse = e.getPoint(); }
    @Override public void mouseDragged(MouseEvent e) { mouse = e.getPoint(); }
    @Override public void mouseClicked(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { mouse = new Point(-1, -1); }
}
