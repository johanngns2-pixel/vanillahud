package de.vanillahud.screen;

import de.vanillahud.module.Modules;
import de.vanillahud.setting.StringSetSetting;
import de.vanillahud.util.I18n;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Liste aller Partikel-Arten, einzeln sichtbar/ausgeblendet schaltbar. Mit Suchfeld. */
public class ParticleScreen extends ScrollScreen {
    private static final int ROW_H = 22;
    private static String query = "";

    private final Screen parent;
    private final StringSetSetting hidden;
    private final List<String> names = new ArrayList<>();
    private final List<String> shown = new ArrayList<>();

    public ParticleScreen(Screen parent, StringSetSetting hidden) {
        super(Component.literal("Particles"));
        this.parent = parent;
        this.hidden = hidden;
        for (var id : BuiltInRegistries.PARTICLE_TYPE.keySet()) names.add(id.getPath());
        names.sort(String::compareTo);
    }

    @Override
    protected void init() {
        listTop = 56;
        listBottom = height - 34;

        EditBox search = new EditBox(font, width / 2 - 100, 28, 200, 18, Component.literal("search"));
        search.setHint(Component.literal(I18n.tr("ui.search")));
        search.setValue(query);
        search.setResponder(s -> {
            query = s;
            scroll = 0;
            buildList();
        });
        addRenderableWidget(search);

        int by = height - 28;
        addRenderableWidget(Button.builder(Component.literal(I18n.tr("ui.hideAll")), b -> {
            hidden.values.addAll(shown);
            buildList();
        }).bounds(width / 2 - 154, by, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal(I18n.tr("ui.showAll")), b -> {
            hidden.values.removeAll(shown);
            buildList();
        }).bounds(width / 2 - 50, by, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal(I18n.tr("ui.back")), b -> onClose())
                .bounds(width / 2 + 54, by, 100, 20).build());
        buildList();
    }

    private Component label(String n) {
        return Component.literal(I18n.tr(hidden.values.contains(n) ? "ui.hidden" : "ui.shown"));
    }

    private void buildList() {
        clearItems();
        shown.clear();
        String q = query.toLowerCase(Locale.ROOT).trim();
        for (String n : names) if (q.isEmpty() || n.contains(q)) shown.add(n);
        int left = width / 2 - 130;
        for (int i = 0; i < shown.size(); i++) {
            String n = shown.get(i);
            addItem(Button.builder(label(n), b -> {
                if (!hidden.values.add(n)) hidden.values.remove(n);
                b.setMessage(label(n));
            }).bounds(left + 190, 0, 70, 18).build(), i * ROW_H + 2);
        }
        contentH = shown.size() * ROW_H;
    }

    @Override
    protected void renderRows(GuiGraphics g, int mouseX, int mouseY, float delta, int top) {
        int left = width / 2 - 130;
        for (int i = 0; i < shown.size(); i++) {
            int y = top + i * ROW_H;
            if (y + ROW_H < listTop || y > listBottom) continue;
            g.fill(left, y + 1, left + 260, y + ROW_H - 1, 0xFF373737);
            String n = shown.get(i);
            g.drawString(font, n.replace('_', ' '), left + 6, y + 7,
                    hidden.values.contains(n) ? 0xFF888888 : 0xFFFFFFFF, true);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(font, I18n.tr("ui.particlesTitle"), width / 2, 12, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        Modules.save();
        minecraft.setScreen(parent);
    }
}
