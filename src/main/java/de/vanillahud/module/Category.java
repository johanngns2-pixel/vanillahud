package de.vanillahud.module;

public enum Category {
    INFO("Info"),
    PVP("PvP"),
    VISUAL("Visual"),
    UTILITY("Utility");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
