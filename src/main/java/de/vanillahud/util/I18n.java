package de.vanillahud.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Eigene kleine Uebersetzung, unabhaengig von der Minecraft-Sprache. Dateien: assets/vanillahud/vh_lang/<code>.json */
public final class I18n {
    public static final String[] LANGS = {"en", "de"};
    private static final Map<String, String> EN = load("en");
    private static Map<String, String> cur = EN;
    public static String lang = "en";

    private I18n() {}

    private static Map<String, String> load(String code) {
        Map<String, String> m = new HashMap<>();
        try (InputStream in = I18n.class.getResourceAsStream("/assets/vanillahud/vh_lang/" + code + ".json")) {
            if (in != null) {
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                o.entrySet().forEach(e -> m.put(e.getKey(), e.getValue().getAsString()));
            }
        } catch (Exception ignored) {
        }
        return m;
    }

    public static void setLang(String code) {
        lang = code;
        cur = code.equals("en") ? EN : load(code);
    }

    public static String tr(String key) {
        String v = cur.get(key);
        if (v == null) v = EN.get(key);
        return v == null ? key : v;
    }

    public static String tr(String key, Object... args) {
        try {
            return String.format(tr(key), args);
        } catch (Exception e) {
            return tr(key);
        }
    }

    public static String langName(String code) {
        return code.equals("en") ? EN.getOrDefault("lang.name", "English") : load(code).getOrDefault("lang.name", code);
    }
}
