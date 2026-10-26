package de.vanillahud.setting;

import com.google.gson.JsonElement;

/** Basis fuer alle einstellbaren Werte eines Moduls. Das Label kommt aus der Sprachdatei: "setting.<id>". */
public abstract class Setting {
    public final String id;

    protected Setting(String id) {
        this.id = id;
    }

    public abstract JsonElement save();

    public abstract void load(JsonElement e);

    public abstract void reset();
}
