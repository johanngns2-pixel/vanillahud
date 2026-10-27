package de.vanillahud.screen;

import de.vanillahud.module.Category;
import de.vanillahud.module.Module;
import de.vanillahud.setting.Setting;
import net.minecraft.world.item.Items;

/** Colors of the mod's own menus. Opened with the Theme button in the main menu. */
public class Theme extends Module {
    public static final Theme I = new Theme();

    public final Setting.Choice style = add(new Setting.Choice("button_style", "Button Style", 0, "Vanilla", "Flat"));
    public final Setting.Color btn = add(new Setting.Color("btn", "Button Color (Flat)", 0xFF3A3A3A, true, false));
    public final Setting.Color btnHover = add(new Setting.Color("btn_hover", "Button Hover Color (Flat)", 0xFF5A5A5A, true, false));
    public final Setting.Color btnOff = add(new Setting.Color("btn_disabled", "Button Disabled Color (Flat)", 0xFF222222, true, false));
    public final Setting.Color btnBorder = add(new Setting.Color("btn_border", "Button Border Color (Flat)", 0xFF000000, true, false));
    public final Setting.Color btnText = add(new Setting.Color("btn_text", "Button Text Color", 0xFFFFFFFF));
    public final Setting.Color btnTextOff = add(new Setting.Color("btn_text_off", "Disabled Text Color", 0xFF888888));
    public final Setting.Color primary = add(new Setting.Color("primary", "Done Button Color (Flat)", 0xFF2E6B33, true, false));
    public final Setting.Color toggleOn = add(new Setting.Color("toggle_on", "ON Toggle Button Color (Flat)", 0xFF2E6B33, true, false));
    public final Setting.Color toggleOff = add(new Setting.Color("toggle_off", "OFF Toggle Button Color (Flat)", 0xFF6B2E2E, true, false));
    public final Setting.Color onText = add(new Setting.Color("on_text", "ON Text Color", 0xFF55FF55));
    public final Setting.Color offText = add(new Setting.Color("off_text", "OFF Text Color", 0xFFFF5555));
    public final Setting.Color accent = add(new Setting.Color("accent", "Accent Color", 0xFF55FF55));
    public final Setting.Color cardBg = add(new Setting.Color("card_bg", "Card Background", 0xFF373737, true, false));
    public final Setting.Color cardLight = add(new Setting.Color("card_light", "Card Light Edge", 0xFFFFFFFF, true, false));
    public final Setting.Color cardDark = add(new Setting.Color("card_dark", "Card Dark Edge", 0xFF555555, true, false));
    public final Setting.Color title = add(new Setting.Color("title", "Title Color", 0xFFFFFFFF));
    public final Setting.Color sliderFill = add(new Setting.Color("slider_fill", "Slider Fill Color (Flat)", 0xFF3F76E4, true, false));
    public final Setting.Color sliderHandle = add(new Setting.Color("slider_handle", "Slider Handle Color (Flat)", 0xFFCCCCCC, true, false));
    public final Setting.Color tipBg = add(new Setting.Color("tip_bg", "Tooltip Background", 0xF0100010, true, false));
    public final Setting.Color tipBorder = add(new Setting.Color("tip_border", "Tooltip Border", 0xFF5000B0, true, false));
    public final Setting.Color tipText = add(new Setting.Color("tip_text", "Tooltip Text", 0xFFFFFFFF));
    public final Setting.Color scroll = add(new Setting.Color("scroll", "Scrollbar Color", 0xFFAAAAAA, true, false));
    public final Setting.Color bgTint = add(new Setting.Color("bg_tint", "Menu Background Tint", 0x00000000, true, false));

    private Theme() {
        super("theme", "Theme", "Colors of the VanillaHud menus.", Items.PAINTING, Category.UTILITY, false);
        add(new Setting.Action("Reset All Theme Colors", this::resetSettings));
    }

    @Override
    public boolean toggleable() {
        return false;
    }

    public static boolean flat() {
        return I.style.is("Flat");
    }
}
