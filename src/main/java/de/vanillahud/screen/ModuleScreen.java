package de.vanillahud.screen;

import de.vanillahud.module.HudModule;
import de.vanillahud.module.Modules;
import de.vanillahud.util.I18n;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Hauptmenue: Karten mit Icon, Name, AN/AUS und Einstellungs-Button. Mehrere Seiten. */
public class ModuleScreen extends Screen {
    private static final int COLS = 3;
    private static final int CARD_W = 140;
    private static final int CARD_H = 44;
    private static final int GAP = 6;
    private static int lastPage = 0;

    private final List<Card> cards = new ArrayList<>();
    private int page;

    private record Card(HudModule module, int x, int y) {}

    public ModuleScreen() {
        super(Component.literal("VanillaHud"));
        this.page = lastPage;
    }

    @Override
    protected void init() {
        cards.clear();
        int top = 32;
        int rows = Math.max(1, Math.min(4, (height - top - 62) / (CARD_H + GAP)));
        int perPage = rows * COLS;
        int pages = Math.max(1, (Modules.ALL.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        lastPage = page;

        int totalW = COLS * CARD_W + (COLS - 1) * GAP;
        int left = (width - totalW) / 2;
        int from = page * perPage;
        int to = Math.min(Modules.ALL.size(), from + perPage);

        for (int i = from; i < to; i++) {
            HudModule m = Modules.ALL.get(i);
            int idx = i - from;
            int cx = left + (idx % COLS) * (CARD_W + GAP);
            int cy = top + (idx / COLS) * (CARD_H + GAP);
            cards.add(new Card(m, cx, cy));

            addRenderableWidget(Button.builder(label(m), b -> {
                m.setEnabled(!m.enabled);
                b.setMessage(label(m));
                Modules.save();
            }).bounds(cx + 6, cy + 22, 46, 16).build());
            addRenderableWidget(Button.builder(Component.literal(I18n.tr("ui.settings")),
                    b -> minecraft.setScreen(new ModuleSettingsScreen(this, m)))
                    .bounds(cx + 56, cy + 22, CARD_W - 62, 16).build());
        }

        int row1 = height - 52;
        int row2 = height - 28;
        Button prev = Button.builder(Component.literal("<"), b -> { lastPage = page - 1; minecraft.setScreen(new ModuleScreen()); })
                .bounds(width / 2 - 154, row1, 40, 20).build();
        prev.active = page > 0;
        addRenderableWidget(prev);
        Button next = Button.builder(Component.literal(">"), b -> { lastPage = page + 1; minecraft.setScreen(new ModuleScreen()); })
                .bounds(width / 2 + 114, row1, 40, 20).build();
        next.active = page < pages - 1;
        addRenderableWidget(next);
        pageLabel = I18n.tr("ui.page", page + 1, pages);
        pageY = row1 + 6;

        addRenderableWidget(Button.builder(Component.literal(I18n.tr("ui.arrange")),
                b -> minecraft.setScreen(new HudEditScreen(this)))
                .bounds(width / 2 - 154, row2, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal(I18n.tr("ui.language") + ": " + I18n.langName(I18n.lang)), b -> {
            int i = 0;
            for (int k = 0; k < I18n.LANGS.length; k++) if (I18n.LANGS[k].equals(I18n.lang)) i = k;
            I18n.setLang(I18n.LANGS[(i + 1) % I18n.LANGS.length]);
            Modules.save();
            lastPage = page;
            minecraft.setScreen(new ModuleScreen());
        }).bounds(width / 2 - 50, row2, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal(I18n.tr("ui.done")), b -> onClose())
                .bounds(width / 2 + 54, row2, 100, 20).build());
    }

    private String pageLabel = "";
    private int pageY;

    private static Component label(HudModule m) {
        return Component.literal(I18n.tr(m.enabled ? "ui.on" : "ui.off"))
                .withStyle(m.enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(font, I18n.tr("ui.title"), width / 2, 14, 0xFFFFFFFF);
        g.drawCenteredString(font, pageLabel, width / 2, pageY, 0xFFAAAAAA);

        for (Card c : cards) {
            g.fill(c.x, c.y, c.x + CARD_W, c.y + CARD_H, 0xFF373737);
            g.fill(c.x, c.y, c.x + CARD_W, c.y + 1, 0xFFFFFFFF);
            g.fill(c.x, c.y, c.x + 1, c.y + CARD_H, 0xFFFFFFFF);
            g.fill(c.x, c.y + CARD_H - 1, c.x + CARD_W, c.y + CARD_H, 0xFF555555);
            g.fill(c.x + CARD_W - 1, c.y, c.x + CARD_W, c.y + CARD_H, 0xFF555555);
            g.renderItem(c.module.iconStack(), c.x + 6, c.y + 3);
            g.drawString(font, c.module.name(), c.x + 28, c.y + 7, 0xFFFFFFFF, true);
        }
        for (var child : children()) {
            if (child instanceof Renderable r) r.render(g, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
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
