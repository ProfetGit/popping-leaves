package io.github.profetgit.snappyleafdecay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@code config/snappy_leaf_decay.json}, re-read within about 2 s when the file changes (PRD 5.4). Unknown keys are kept,
 * missing keys take their defaults. Every number marked as a guess in the PRD is a key here. {@code -Dsnappy_leaf_decay.<key>}
 * overrides a value in tests (see {@link #prop}).
 */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Platform.configDir().resolve(SnappyLeafDecay.ID + ".json");

    public int version = 1;
    public Server server = new Server();
    public Client client = new Client();

    /** The server half: when and how fast decaying leaves go. */
    public static final class Server {
        public boolean enabled = true;
        /** Ticks from a leaf becoming decaying to its removal, at the cut itself (guess, tune at the feel check). */
        public int delay = 2;
        /** Extra random ticks on top (guess). */
        public int jitter = 2;
        /** Ticks per block of distance from the cut: the ripple's speed (guess). */
        public int stagger = 9;
        /** The ripple never adds more than this many ticks, so a giant still falls in a few seconds (guess). */
        public int maxRipple = 60;
        /** Decays per level per tick at most; the rest keep their order and go next tick. */
        public int maxPerTick = 40;
        /** With randomTickSpeed 0 (admins use it to freeze growth and decay) the server half stands aside. */
        public boolean respectRandomTickSpeed = true;
    }

    /** The client half: what a decaying leaf looks like. */
    public static final class Client {
        public boolean enabled = true;
        /** 50-150 (%), scales the squash and the chips' speed. */
        public int intensity = 100;
        /** Reduced motion: amplitudes x0.3, no hold, one chip. */
        public boolean calm = false;
        public boolean sound = true;
        /** A leaf that has just become decaying sheds a chip (the anticipation). */
        public boolean shed = true;
        public int chips = 4;
        /** Ticks the leaf holds (with a short squash) before it shrinks away (guess). */
        public double hold = 1;
        public double shrink = 3;
        public double squash = 0.12;
        public double chipSize = 0.55;
        public double chipSpeed = 1.0;
    }

    private static Config current = new Config();
    private static long stamp = -1;
    private static int poll;

    public static Config get() {
        return current;
    }

    public static Server server() {
        return current.server;
    }

    public static Client client() {
        return current.client;
    }

    /** A test override, {@code -Dsnappy_leaf_decay.<key>=value}, or {@code def}. */
    public static double prop(String key, double def) {
        String v = System.getProperty(SnappyLeafDecay.ID + "." + key);
        return v == null || v.isEmpty() ? def : Double.parseDouble(v);
    }

    public static void load() {
        JsonObject raw = read();
        Config c;
        try {
            c = GSON.fromJson(raw, Config.class);
            if (c == null) c = new Config();
        } catch (RuntimeException e) {
            SnappyLeafDecay.LOG.warn("Snappy Leaf Decay: config has a bad value, using defaults ({})", e.toString());
            c = new Config();
        }
        if (c.server == null) c.server = new Server();
        if (c.client == null) c.client = new Client();
        c.server.delay = Math.max(0, Math.min(200, c.server.delay));
        c.server.jitter = Math.max(0, Math.min(200, c.server.jitter));
        c.server.stagger = Math.max(0, Math.min(20, c.server.stagger));
        c.server.maxRipple = Math.max(0, Math.min(400, c.server.maxRipple));
        c.server.maxPerTick = Math.max(1, Math.min(4096, c.server.maxPerTick));
        c.client.intensity = Math.max(50, Math.min(150, c.client.intensity));
        c.client.chips = Math.max(0, Math.min(16, c.client.chips));
        c.client.hold = Math.max(0, Math.min(10, c.client.hold));
        c.client.shrink = Math.max(1, Math.min(20, c.client.shrink));
        c.client.squash = Math.max(0, Math.min(0.5, c.client.squash));
        current = c;
        write(c, raw);
    }

    private static JsonObject read() {
        JsonObject raw = new JsonObject();
        try {
            if (Files.exists(FILE)) {
                JsonElement e = JsonParser.parseString(Files.readString(FILE));
                if (e.isJsonObject()) raw = e.getAsJsonObject();
            }
        } catch (IOException | RuntimeException e) {
            SnappyLeafDecay.LOG.warn("Snappy Leaf Decay: config unreadable, using defaults ({})", e.toString());
        }
        return raw;
    }

    /** Writes the settings screen's changes; unknown keys in the file stay. */
    public static synchronized void save() {
        write(current, read());
    }

    private static void write(Config c, JsonObject raw) {
        JsonObject merged = GSON.toJsonTree(c).getAsJsonObject();
        for (String k : raw.keySet()) if (!merged.has(k)) merged.add(k, raw.get(k));
        try {
            Files.createDirectories(FILE.getParent());
            String text = GSON.toJson(merged);
            if (!Files.exists(FILE) || !Files.readString(FILE).equals(text)) Files.writeString(FILE, text);
            stamp = Files.getLastModifiedTime(FILE).toMillis();
        } catch (IOException e) {
            SnappyLeafDecay.LOG.warn("Snappy Leaf Decay: could not write the config ({})", e.toString());
        }
    }

    /** Called every tick, on either side: reloads when the file's modification time changed (checked every 40 calls). */
    public static void pollReload() {
        if (++poll < 40) return;
        poll = 0;
        try {
            if (Files.exists(FILE) && Files.getLastModifiedTime(FILE).toMillis() != stamp) load();
        } catch (IOException ignored) {
        }
    }

    /** The intensity as a factor, with Calm applied. */
    public double amplitude() {
        return client.intensity / 100.0 * (client.calm ? 0.3 : 1.0);
    }
}
