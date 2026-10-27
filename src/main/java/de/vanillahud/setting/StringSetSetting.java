package de.vanillahud.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.HashSet;
import java.util.Set;

/** Menge von Namen (z.B. ausgeblendete Partikel). Hat kein Widget im Standard-Einstellungsmenue. */
public class StringSetSetting extends Setting {
    public final Set<String> values = new HashSet<>();

    public StringSetSetting(String id) {
        super(id);
    }

    @Override
    public JsonElement save() {
        JsonArray a = new JsonArray();
        for (String s : values) a.add(new JsonPrimitive(s));
        return a;
    }

    @Override
    public void load(JsonElement e) {
        values.clear();
        for (JsonElement x : e.getAsJsonArray()) values.add(x.getAsString());
    }

    @Override
    public void reset() {
        values.clear();
    }
}
