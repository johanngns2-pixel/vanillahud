package de.vanillahud.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Auswahl aus fester Liste. Anzeige ueber Sprachschluessel "opt.<name>". */
public class EnumSetting extends Setting {
    public final String[] options;
    public int index;
    private final int def;

    public EnumSetting(String id, int def, String... options) {
        super(id);
        this.options = options;
        this.def = def;
        this.index = def;
    }

    public String get() { return options[index]; }
    public void next() { index = (index + 1) % options.length; }

    @Override public JsonElement save() { return new JsonPrimitive(get()); }
    @Override public void load(JsonElement e) {
        for (int i = 0; i < options.length; i++) if (options[i].equals(e.getAsString())) index = i;
    }
    @Override public void reset() { index = def; }
}
