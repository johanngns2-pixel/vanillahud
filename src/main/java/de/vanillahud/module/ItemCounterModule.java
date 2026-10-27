package de.vanillahud.module;

import de.vanillahud.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Counts up to three item types in the inventory. Pick the items in the settings. */
public class ItemCounterModule extends HudModule {
    private record Entry(String name, Item shown, Set<Item> items) {}

    private static final List<Entry> ENTRIES = List.of(
            new Entry("Totem of Undying", Items.TOTEM_OF_UNDYING, Set.of(Items.TOTEM_OF_UNDYING)),
            new Entry("Golden Apple", Items.GOLDEN_APPLE, Set.of(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE)),
            new Entry("Ender Pearl", Items.ENDER_PEARL, Set.of(Items.ENDER_PEARL)),
            new Entry("Arrow", Items.ARROW, Set.of(Items.ARROW, Items.SPECTRAL_ARROW, Items.TIPPED_ARROW)),
            new Entry("End Crystal", Items.END_CRYSTAL, Set.of(Items.END_CRYSTAL)),
            new Entry("Splash Potion", Items.SPLASH_POTION, Set.of(Items.SPLASH_POTION, Items.LINGERING_POTION)),
            new Entry("Experience Bottle", Items.EXPERIENCE_BOTTLE, Set.of(Items.EXPERIENCE_BOTTLE)),
            new Entry("Obsidian", Items.OBSIDIAN, Set.of(Items.OBSIDIAN)),
            new Entry("Cobweb", Items.COBWEB, Set.of(Items.COBWEB)),
            new Entry("Water Bucket", Items.WATER_BUCKET, Set.of(Items.WATER_BUCKET)),
            new Entry("Firework Rocket", Items.FIREWORK_ROCKET, Set.of(Items.FIREWORK_ROCKET)),
            new Entry("Golden Carrot", Items.GOLDEN_CARROT, Set.of(Items.GOLDEN_CARROT)),
            new Entry("TNT", Items.TNT, Set.of(Items.TNT)),
            new Entry("Respawn Anchor", Items.RESPAWN_ANCHOR, Set.of(Items.RESPAWN_ANCHOR)),
            new Entry("Glowstone", Items.GLOWSTONE, Set.of(Items.GLOWSTONE)));

    private static String[] names(boolean none) {
        List<String> l = new ArrayList<>();
        if (none) l.add("None");
        for (Entry e : ENTRIES) l.add(e.name);
        return l.toArray(new String[0]);
    }

    public final Setting.Choice item1 = add(new Setting.Choice("item1", "Item 1", 0, names(false)));
    public final Setting.Choice item2 = add(new Setting.Choice("item2", "Item 2", 0, names(true)));
    public final Setting.Choice item3 = add(new Setting.Choice("item3", "Item 3", 0, names(true)));
    public final Setting.Choice layout = add(new Setting.Choice("layout", "Layout", 0, "Vertical", "Horizontal"));
    public final Setting.Bool icon = add(new Setting.Bool("icon", "Show Icon", true));
    public final Setting.Bool label = add(new Setting.Bool("label", "Show Name", false));
    public final Setting.Bool hideZero = add(new Setting.Bool("hide_zero", "Hide When Zero", false));

    public ItemCounterModule() {
        super("item_counter", "Item Counter", "Counts items like totems, pearls or arrows. Choose the items in the settings.", Items.TOTEM_OF_UNDYING, Category.PVP, false);
    }

    private Entry entry(Setting.Choice c) {
        if (c.is("None")) return null;
        for (Entry e : ENTRIES) if (e.name.equals(c.get())) return e;
        return null;
    }

    private int count(Minecraft mc, Entry e) {
        if (mc.player == null) return 0;
        Inventory inv = mc.player.getInventory();
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && e.items.contains(s.getItem())) n += s.getCount();
        }
        return n;
    }

    private List<Entry> active(Minecraft mc) {
        List<Entry> l = new ArrayList<>();
        for (Setting.Choice c : new Setting.Choice[]{item1, item2, item3}) {
            Entry e = entry(c);
            if (e == null || l.contains(e)) continue;
            if (hideZero.value && count(mc, e) == 0) continue;
            l.add(e);
        }
        return l;
    }

    private String text(Minecraft mc, Entry e) {
        return label.value ? e.name + ": " + count(mc, e) : Integer.toString(count(mc, e));
    }

    private int cellW(Minecraft mc, Entry e) {
        return (icon.value ? 19 : 0) + mc.font.width(text(mc, e));
    }

    private int cellH() {
        return icon.value ? 16 : 9;
    }

    private boolean vertical() {
        return layout.is("Vertical");
    }

    @Override
    protected int contentWidth(Minecraft mc) {
        List<Entry> l = active(mc);
        if (l.isEmpty()) return 10;
        int w = 0;
        for (Entry e : l) w = vertical() ? Math.max(w, cellW(mc, e)) : w + cellW(mc, e) + 6;
        return vertical() ? w : w - 6;
    }

    @Override
    protected int contentHeight(Minecraft mc) {
        List<Entry> l = active(mc);
        if (l.isEmpty()) return 9;
        return vertical() ? l.size() * (cellH() + 2) - 2 : cellH();
    }

    @Override
    protected void drawContent(GuiGraphics g, Minecraft mc, int cx, int cy, int color) {
        int x = cx, y = cy;
        for (Entry e : active(mc)) {
            int tx = x;
            if (icon.value) {
                g.renderItem(new ItemStack(e.shown), x, y);
                tx += 19;
            }
            g.drawString(mc.font, text(mc, e), tx, y + (icon.value ? 4 : 0), color, shadow.value);
            if (vertical()) y += cellH() + 2;
            else x += cellW(mc, e) + 6;
        }
    }
}
