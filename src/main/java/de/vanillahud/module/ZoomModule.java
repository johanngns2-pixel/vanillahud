package de.vanillahud.module;

import com.mojang.blaze3d.platform.InputConstants;
import de.vanillahud.setting.Setting;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/** Hold a key to zoom in. The FOV change itself is done in GameRendererMixin. */
public class ZoomModule extends Module {
    public static ZoomModule INSTANCE;

    public final Setting.Num factor = add(new Setting.Num("factor", "Zoom Factor", 1.5, 12, 0.5, 4, "x"));
    public final Setting.Bool smooth = add(new Setting.Bool("smooth", "Smooth Zoom", true));
    public final Setting.Bool cinematic = add(new Setting.Bool("cinematic", "Cinematic Camera While Zooming", false));
    public final Setting.Bool scroll = add(new Setting.Bool("scroll", "Scroll To Change Zoom", true));

    private final KeyMapping key;
    private double current = 1.0;
    private double userFactor = -1;
    private long lastNanos = System.nanoTime();
    private boolean zooming;
    private boolean oldSmooth;

    public ZoomModule() {
        super("zoom", "Zoom", "Hold a key (default C) to zoom in.", Items.SPYGLASS, Category.UTILITY, false);
        INSTANCE = this;
        key = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.vanillahud.zoom", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C, KeyMapping.Category.MISC));
    }

    private double target() {
        return userFactor > 0 ? userFactor : factor.value;
    }

    @Override
    public void onTick(Minecraft mc) {
        boolean want = mc.screen == null && key.isDown();
        if (want && !zooming) {
            oldSmooth = mc.options.smoothCamera;
            userFactor = -1;
        }
        if (want && cinematic.value) mc.options.smoothCamera = true;
        if (!want && zooming && cinematic.value) mc.options.smoothCamera = oldSmooth;
        zooming = want;
    }

    @Override
    public void onDisable() {
        if (zooming && cinematic.value) Minecraft.getInstance().options.smoothCamera = oldSmooth;
        zooming = false;
        current = 1.0;
    }

    /** Mouse wheel while zooming. Returns true if the scroll was used. */
    public boolean scrolled(double amount) {
        if (!enabled || !zooming || !scroll.value) return false;
        userFactor = Math.max(1.2, Math.min(20, target() + amount * 0.5));
        return true;
    }

    /** Current zoom divisor, smoothed per frame. */
    public double current() {
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - lastNanos) / 1e9);
        lastNanos = now;
        double goal = enabled && zooming ? target() : 1.0;
        if (smooth.value) current += (goal - current) * Math.min(1.0, dt * 14);
        else current = goal;
        if (Math.abs(current - goal) < 0.001) current = goal;
        return current;
    }

    public static float apply(float fov) {
        ZoomModule z = INSTANCE;
        if (z == null) return fov;
        double c = z.current();
        return c <= 1.0001 ? fov : (float) (fov / c);
    }

    public static boolean onScroll(double amount) {
        return INSTANCE != null && INSTANCE.scrolled(amount);
    }
}
