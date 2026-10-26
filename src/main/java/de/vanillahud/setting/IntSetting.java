package de.vanillahud.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class IntSetting extends Setting {
    public final int min, max;
    public final String unit;
    public int value;
    private final int def;

    public IntSetting(String id, int min, int max, int def, String unit) {
        super(id);
        this.min = min;
        this.max = max;
        this.def = def;
        this.unit = unit;
        this.value = def;
    }

    public void set(int v) { value = Math.max(min, Math.min(max, v)); }

    @Override public JsonElement save() { return new JsonPrimitive(value); }
    @Override public void load(JsonElement e) { set(e.getAsInt()); }
    @Override public void reset() { value = def; }
}
