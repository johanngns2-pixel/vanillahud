package de.vanillahud.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BoolSetting extends Setting {
    public boolean value;
    private final boolean def;

    public BoolSetting(String id, boolean def) {
        super(id);
        this.def = def;
        this.value = def;
    }

    @Override public JsonElement save() { return new JsonPrimitive(value); }
    @Override public void load(JsonElement e) { value = e.getAsBoolean(); }
    @Override public void reset() { value = def; }
}
