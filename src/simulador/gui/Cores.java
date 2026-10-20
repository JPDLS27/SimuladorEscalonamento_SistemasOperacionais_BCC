package simulador.gui;

import java.awt.Color;

/** Paleta: uma cor fixa por processo, para o diagrama e o Gantt combinarem. */
final class Cores {
    private static final Color[] PALETA = {
        new Color(0x2E86DE), new Color(0xE67E22), new Color(0x27AE60), new Color(0xC0392B),
        new Color(0x8E44AD), new Color(0x16A085), new Color(0xD4AC0D), new Color(0xE84393),
        new Color(0x34495E), new Color(0x7F8C8D), new Color(0x6C5CE7), new Color(0xA0522D)
    };
    static final Color FUNDO = new Color(0xF4F6F8);
    static final Color ESTADO = new Color(0xDDEBF5);
    static final Color BORDA = new Color(0x2C3E50);
    static final Color TEXTO_SUAVE = new Color(0x5D6D7E);

    static Color doProcesso(int pid) { return PALETA[Math.floorMod(pid - 1, PALETA.length)]; }

    static Color clara(Color c) {
        return new Color((c.getRed() + 3 * 255) / 4, (c.getGreen() + 3 * 255) / 4, (c.getBlue() + 3 * 255) / 4);
    }

    private Cores() {}
}
