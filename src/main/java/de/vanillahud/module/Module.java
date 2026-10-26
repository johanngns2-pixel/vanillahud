package de.vanillahud.module;

import de.vanillahud.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/** Base class of every feature. HUD modules extend HudModule. */
public abstract class Module {
    public final String id;
    public final String name;
    public final String description;
    public final Item icon;
    public final Category category;
    public boolean enabled;
    public final List<Setting> settings = new ArrayList<>();

    protected Module(String id, String name, String description, Item icon, Category category, boolean enabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.category = category;
        this.enabled = enabled;
    }

    public <T extends Setting> T add(T s) {
        settings.add(s);
        return s;
    }

    public boolean isHud() {
        return false;
    }

    public void onTick(Minecraft mc) {}

    public void onEnable() {}

    public void onDisable() {}

    public void setEnabled(boolean on) {
        if (on == enabled) return;
        enabled = on;
        if (on) onEnable();
        else onDisable();
    }

    public void resetSettings() {
        for (Setting s : settings) s.reset();
    }
}
