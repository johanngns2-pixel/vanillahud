package de.vanillahud.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.vanillahud.util.Colors;

/** Base class for all module settings. */
public abstract class Setting {
    public final String id;
    public final String name;
    public Bool dep;

    protected Setting(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /** Only show this setting while the given toggle is on. */
    public Setting dependsOn(Bool b) {
        this.dep = b;
        return this;
    }

    public boolean visible() {
        return dep == null || dep.value;
    }

    public abstract JsonElement save();

    public abstract void load(JsonElement e);

    public abstract void reset();

    // ------------------------------------------------------------------

    public static class Bool extends Setting {
        public boolean value;
        private final boolean def;

        public Bool(String id, String name, boolean def) {
            super(id, name);
            this.value = def;
            this.def = def;
        }

        public JsonElement save() { return new JsonPrimitive(value); }
        public void load(JsonElement e) { value = e.getAsBoolean(); }
        public void reset() { value = def; }
    }

    public static class Num extends Setting {
        public double value;
        public final double min, max, step, def;
        public final String suffix;

        public Num(String id, String name, double min, double max, double step, double def, String suffix) {
            super(id, name);
            this.min = min;
            this.max = max;
            this.step = step;
            this.def = def;
            this.value = def;
            this.suffix = suffix;
        }

        public int i() { return (int) Math.round(value); }
        public float f() { return (float) value; }

        public void set(double v) {
            v = Math.max(min, Math.min(max, v));
            if (step > 0) v = Math.round((v - min) / step) * step + min;
            value = Math.max(min, Math.min(max, v));
        }

        public String text() {
            String n = step >= 1 ? Integer.toString((int) Math.round(value)) : String.format("%.2f", value);
            if (step < 1 && n.endsWith("0")) n = n.substring(0, n.length() - 1);
            return n + suffix;
        }

        public JsonElement save() { return new JsonPrimitive(value); }
        public void load(JsonElement e) { set(e.getAsDouble()); }
        public void reset() { value = def; }
    }

    public static class Choice extends Setting {
        public final String[] options;
        public int index;
        private final int def;

        public Choice(String id, String name, int def, String... options) {
            super(id, name);
            this.options = options;
            this.index = def;
            this.def = def;
        }

        public String get() { return options[index]; }
        public boolean is(String s) { return options[index].equals(s); }

        public void cycle(int dir) {
            index = (index + dir + options.length) % options.length;
        }

        public JsonElement save() { return new JsonPrimitive(get()); }

        public void load(JsonElement e) {
            String s = e.getAsString();
            for (int i = 0; i < options.length; i++) if (options[i].equals(s)) index = i;
        }

        public void reset() { index = def; }
    }

    public static class Color extends Setting {
        public int argb;
        public boolean chroma;
        public final boolean alpha;
        private final int def;
        private final boolean defChroma;

        public Color(String id, String name, int argb, boolean alpha, boolean chroma) {
            super(id, name);
            this.argb = argb;
            this.alpha = alpha;
            this.chroma = chroma;
            this.def = argb;
            this.defChroma = chroma;
        }

        public Color(String id, String name, int argb) {
            this(id, name, argb, false, false);
        }

        /** Final ARGB color, rainbow-animated when chroma is on. */
        public int get() {
            int a = alpha ? (argb >>> 24) : 0xFF;
            int rgb = chroma ? Colors.chroma(4000f, 0f) : (argb & 0xFFFFFF);
            return (a << 24) | rgb;
        }

        /** Same as get() but with a per-element rainbow offset. */
        public int get(float offset) {
            if (!chroma) return get();
            int a = alpha ? (argb >>> 24) : 0xFF;
            return (a << 24) | Colors.chroma(4000f, offset);
        }

        public JsonElement save() {
            JsonObject o = new JsonObject();
            o.addProperty("color", Colors.hex(argb, true));
            o.addProperty("chroma", chroma);
            return o;
        }

        public void load(JsonElement e) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("color")) argb = (int) Long.parseLong(o.get("color").getAsString().substring(1), 16);
            if (o.has("chroma")) chroma = o.get("chroma").getAsBoolean();
        }

        public void reset() {
            argb = def;
            chroma = defChroma;
        }
    }

    /** A button row in the settings screen (not saved). */
    public static class Action extends Setting {
        public final Runnable run;

        public Action(String name, Runnable run) {
            super("action_" + name, name);
            this.run = run;
        }

        public JsonElement save() { return JsonNull.INSTANCE; }
        public void load(JsonElement e) {}
        public void reset() {}
    }
}
