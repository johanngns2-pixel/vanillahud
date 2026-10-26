package de.vanillahud.screen;

import de.vanillahud.module.HudModule;
import de.vanillahud.module.Modules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Das Hauptmenue: Karten mit Minecraft-Item-Icon, Name und AN/AUS-Button. */
public class ModuleScreen extends Screen {
    private static final int COLS = 3;
    private static final int CARD_W = 124;
    private static final int CARD_H = 44;
    private static final int GAP = 6;

    private final List<Card> cards = new ArrayList<>();

    private record Card(HudModule module, int x, int y) {}

    public ModuleScreen() {
        super(Component.translatable("vanillahud.title"));
    }

    @Override
    protected void init() {
        cards.clear();
        int rows = (Modules.ALL.size() + COLS - 1) / COLS;
        int totalW = COLS * CARD_W + (COLS - 1) * GAP;
        int left = (width - totalW) / 2;
        int top = 40;

        for (int i = 0; i < Modules.ALL.size(); i++) {
            HudModule m = Modules.ALL.get(i);
            int cx = left + (i % COLS) * (CARD_W + GAP);
            int cy = top + (i / COLS) * (CARD_H + GAP);
            cards.add(new Card(m, cx, cy));

            Button toggle = Button.builder(label(m), b -> {
                m.enabled = !m.enabled;
                b.setMessage(label(m));
                Modules.save();
            }).bounds(cx + 6, cy + 21, CARD_W - 12, 16).build();
            addRenderableWidget(toggle);
        }

        int bottom = top + rows * (CARD_H + GAP) + 6;
        addRenderableWidget(Button.builder(Component.literal("HUD anordnen"),
                b -> minecraft.setScreen(new HudEditScreen(this)))
                .bounds(width / 2 - 154, bottom, 150, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 + 4, bottom, 150, 20).build());
    }

    private static Component label(HudModule m) {
        return Component.literal(m.enabled ? "AN" : "AUS")
                .withStyle(m.enabled ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta); // Hintergrund + Buttons (Vanilla)
        g.drawCenteredString(font, title, width / 2, 16, 0xFFFFFFFF);

        for (Card c : cards) {
            // Karte im Vanilla-Inventar-Look: dunkle Flaeche mit hellem/dunklem Rand
            g.fill(c.x, c.y, c.x + CARD_W, c.y + CARD_H, 0xFF373737);
            g.fill(c.x, c.y, c.x + CARD_W, c.y + 1, 0xFFFFFFFF);
            g.fill(c.x, c.y, c.x + 1, c.y + CARD_H, 0xFFFFFFFF);
            g.fill(c.x, c.y + CARD_H - 1, c.x + CARD_W, c.y + CARD_H, 0xFF555555);
            g.fill(c.x + CARD_W - 1, c.y, c.x + CARD_W, c.y + CARD_H, 0xFF555555);

            g.renderItem(c.module.iconStack(), c.x + 6, c.y + 3);
            g.drawString(font, c.module.name, c.x + 28, c.y + 7, 0xFFFFFFFF, true);
        }
        // Buttons nochmal ueber die Karten zeichnen
        for (var child : children()) {
            if (child instanceof net.minecraft.client.gui.components.Renderable r) r.render(g, mouseX, mouseY, delta);
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
