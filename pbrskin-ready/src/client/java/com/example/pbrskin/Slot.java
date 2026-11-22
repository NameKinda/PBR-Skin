package com.example.pbrskin;

public enum Slot {
    COLOR("skin.png", "Colormap", true),
    NORMAL("skin_n.png", "Normal map", false),
    SPECULAR("skin_s.png", "Specular map", false);

    public final String file;
    public final String label;
    public final boolean required;

    Slot(String file, String label, boolean required) {
        this.file = file;
        this.label = label;
        this.required = required;
    }
}
