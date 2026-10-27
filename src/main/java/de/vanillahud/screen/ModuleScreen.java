package de.vanillahud.screen;

import de.vanillahud.module.HudModule;
import de.vanillahud.module.Modules;
import de.vanillahud.util.I18n;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Hauptmenue: Suchfeld oben, darunter scrollbare Karten mit Icon, Name, AN/AUS und Einstellungen. */
public class ModuleScreen extends ScrollScreen {
    private static final int CARD_W = 140;
    private static final int CARD_H = 44;
    private static final int GAP = 6;
    private static String query = "";
    private static double savedScroll = 0;

    private EditBox search;
    private final List<Card> cards = new ArrayList<>();
    private int cols = 3, left;

    private record Card(HudModule module, int x, int y) {}

    public ModuleScreen() {
        super(Component.literal("VanillaHud"));
    }

    @Override
    protected void init() {
        listTop = 34;
        listBottom = height - 34;

        search = new EditBox(font, width / 2 - 100, 8, 200, 18, Component.literal("search"));
        search.setHint(Component.literal(I18n.tr("ui.search")));
        search.setValue(query);
        search.setResponder(s -> {
            query = s;
            scroll = 0;
            buildList();
        });
        addRenderableWidget(search);

        int bw = Math.min(100, (width - 24) / 4);
        int by = height - 28;
        int bx = width / 2 - (4 * bw + 3 * 4) / 2;
        addRenderableWidget(Button.builder(Component.literal(I18n.tr("ui.arrange")),
                b -> minecraft.setScreen(new HudEditScreen(this))).bounds(bx, by, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal(I18n.tr("ui.order")),
                b -> minecraft.setScreen(new OrderScreen(this))).bounds(bx + bw + 4, by, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal(I18n.tr("ui.language") + ": " + I18n.langName(I18n.lang)), b -> {
            int i = 0;
            for (int k = 0; k < I18n.LANGS.length; k++) if (I18n.LANGS[k].equals(I18n.lang)) i = k;
            I18n.setLang(I18n.LANGS[(i + 1) % I18n.LANGS.length]);
            Modules.save();
            minecraft.setScreen(new ModuleScreen());
        }).bounds(bx + 2 * (bw + 4), by, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal(I18n.tr("ui.done")), b -> onClose())
                .bounds(bx + 3 * (bw + 4), by, bw, 20).build());

        buildList();
        scroll = savedScroll;
        clampScroll();
    }

    private void buildList() {
        clearItems();
        cards.clear();
        cols = Math.max(1, Math.min(3, (width - 24) / (CARD_W + GAP)));
        left = (width - (cols * CARD_W + (cols - 1) * GAP)) / 2;
        String q = query.toLowerCase(Locale.ROOT).trim();

        int idx = 0;
        for (HudModule m : Modules.ALL) {
            if (!q.isEmpty() && !m.name().toLowerCase(Locale.ROOT).contains(q) && !m.id.contains(q)) continue;
            int cx = left + (idx % cols) * (CARD_W + GAP);
            int cy = (idx / cols) * (CARD_H + GAP);
            cards.add(new Card(m, cx, cy));
            addItem(Button.builder(label(m), b -> {
                m.setEnabled(!m.enabled);
                b.setMessage(label(m));
                Modules.save();
            }).bounds(cx + 6, 0, 46, 16).build(), cy + 22);
            addItem(Button.builder(Component.literal(I18n.tr("ui.settings")),
                    b -> minecraft.setScreen(m.screen != null ? m.screen.apply(this) : new ModuleSettingsScreen(this, m)))
                    .bounds(cx + 56, 0, CARD_W - 62, 16).build(), cy + 22);
            idx++;
        }
        contentH = ((idx + cols - 1) / cols) * (CARD_H + GAP);
    }

    private static Component label(HudModule m) {
        return Component.literal(I18n.tr(m.enabled ? "ui.on" : "ui.off"))
                .withStyle(m.enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    @Override
    protected void renderRows(GuiGraphics g, int mouseX, int mouseY, float delta, int top) {
        savedScroll = scroll;
        if (cards.isEmpty()) {
            g.drawCenteredString(font, I18n.tr("ui.noResults"), width / 2, listTop + 20, 0xFFAAAAAA);
        }
        for (Card c : cards) {
            int y = top + c.y;
            if (y + CARD_H < listTop || y > listBottom) continue;
            g.fill(c.x, y, c.x + CARD_W, y + CARD_H, 0xFF373737);
            g.fill(c.x, y, c.x + CARD_W, y + 1, 0xFFFFFFFF);
            g.fill(c.x, y, c.x + 1, y + CARD_H, 0xFFFFFFFF);
            g.fill(c.x, y + CARD_H - 1, c.x + CARD_W, y + CARD_H, 0xFF555555);
            g.fill(c.x + CARD_W - 1, y, c.x + CARD_W, y + CARD_H, 0xFF555555);
            g.renderItem(c.module.iconStack(), c.x + 6, y + 3);
            g.drawString(font, c.module.name(), c.x + 28, y + 7, 0xFFFFFFFF, true);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT && !search.isFocused()) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        Modules.save();
        super.onClose();
    }
}
