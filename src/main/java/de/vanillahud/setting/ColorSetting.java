package de.vanillahud.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** RGB-Farbe (ohne Alpha), im Menue als Hex-Feld bearbeitbar. */
public class ColorSetting extends Setting {
    public int value;
    private final int def;

    public ColorSetting(String id, int def) {
        super(id);
        this.def = def & 0xFFFFFF;
        this.value = this.def;
    }

    public String hex() { return String.format("%06X", value & 0xFFFFFF); }

    @Override public JsonElement save() { return new JsonPrimitive(hex()); }
    @Override public void load(JsonElement e) { value = Integer.parseInt(e.getAsString(), 16) & 0xFFFFFF; }
    @Override public void reset() { value = def; }
}
