package de.vanillahud.util;

public final class Colors {
    private Colors() {}

    /** h,s,v in 0..1 -> 0xRRGGBB */
    public static int hsv(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        float r, g, b;
        int i = (int) (h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public static float[] toHsv(int rgb) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float d = max - min, h;
        if (d == 0) h = 0;
        else if (max == r) h = ((g - b) / d) % 6f;
        else if (max == g) h = (b - r) / d + 2;
        else h = (r - g) / d + 4;
        h /= 6f;
        if (h < 0) h += 1f;
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }

    public static String hex(int argb, boolean alpha) {
        return alpha ? String.format("#%08X", argb) : String.format("#%06X", argb & 0xFFFFFF);
    }

    /** Accepts #RRGGBB, RRGGBB, #AARRGGBB. Returns null when invalid. */
    public static Integer parse(String s, boolean alpha, int currentAlpha) {
        try {
            String t = s.startsWith("#") ? s.substring(1) : s;
            if (t.length() == 6) return (currentAlpha << 24) | Integer.parseInt(t, 16);
            if (t.length() == 8 && alpha) return (int) Long.parseLong(t, 16);
        } catch (Exception ignored) {
        }
        return null;
    }

    public static int chroma(float speedMs, float offset) {
        float h = ((System.currentTimeMillis() % (long) speedMs) / speedMs + offset);
        return hsv(h, 0.85f, 1f);
    }
}
