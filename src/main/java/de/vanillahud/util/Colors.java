package de.vanillahud.util;

public final class Colors {
    /** Schnellauswahl-Farben im Farbwaehler. */
    public static final int[] PRESETS = {
            0xFFFFFF, 0xC0C0C0, 0x808080, 0x000000, 0xFF5555, 0xFF8800, 0xFFFF55, 0x88FF00,
            0x55FF55, 0x00AA44, 0x55FFFF, 0x00AAAA, 0x5555FF, 0x8844FF, 0xFF55FF, 0xFF88AA};

    private Colors() {}

    /** h, s, v jeweils 0..1 -> RGB (0xRRGGBB). */
    public static int hsvToRgb(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        float f = h * 6f;
        int i = (int) f;
        f -= i;
        float p = v * (1f - s), q = v * (1f - s * f), t = v * (1f - s * (1f - f));
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return ((int) (r * 255 + 0.5f) << 16) | ((int) (g * 255 + 0.5f) << 8) | (int) (b * 255 + 0.5f);
    }

    /** RGB -> {h, s, v}, alles 0..1. */
    public static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h;
        if (d == 0) h = 0;
        else if (max == r) h = (((g - b) / d) % 6f) / 6f;
        else if (max == g) h = (((b - r) / d) + 2f) / 6f;
        else h = (((r - g) / d) + 4f) / 6f;
        if (h < 0) h += 1f;
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }
}
