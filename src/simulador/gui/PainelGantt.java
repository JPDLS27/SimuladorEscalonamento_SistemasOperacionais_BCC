package simulador.gui;

import simulador.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Gráfico de Gantt: primeira linha = CPU; depois uma linha por processo mostrando
 * Execução (cor cheia), Pronto (cor clara) e Espera/E/S (listrado cinza).
 */
class PainelGantt extends JPanel {
    private static final int ROTULO = 56, CEL = 24, LIN = 24, TOPO = 8;
    private Simulador sim;

    PainelGantt() { setBackground(Color.WHITE); }

    void setSimulador(Simulador s) {
        this.sim = s;
        revalidate();
        repaint();
        SwingUtilities.invokeLater(() -> scrollRectToVisible(new Rectangle(getPreferredSize().width - 1, 0, 1, 1)));
    }

    @Override public Dimension getPreferredSize() {
        int ticks = sim == null ? 0 : sim.getGantt().size();
        int n = sim == null ? 0 : sim.getProcessos().size();
        return new Dimension(ROTULO + Math.max(ticks, 20) * CEL + 20, TOPO + (n + 1) * LIN + 62);
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (sim == null) return;
        List<Processo> cpu = sim.getGantt();
        List<Estado[]> hist = sim.getHistorico();
        List<Processo> ps = sim.getProcessos();
        int ticks = cpu.size();

        g.setFont(getFont().deriveFont(Font.BOLD, 12f));
        // linha da CPU
        int y = TOPO;
        g.setColor(Cores.BORDA);
        g.drawString("CPU", 10, y + 16);
        for (int t = 0; t < ticks; t++) {
            Processo p = cpu.get(t);
            int x = ROTULO + t * CEL;
            if (p == null) {
                g.setColor(new Color(0xEEEEEE));
                g.fillRect(x, y, CEL, LIN - 4);
            } else {
                g.setColor(Cores.doProcesso(p.pid));
                g.fillRect(x, y, CEL, LIN - 4);
                // nome só no início de cada bloco contínuo
                if (t == 0 || cpu.get(t - 1) != p) {
                    g.setColor(Color.WHITE);
                    g.setFont(getFont().deriveFont(Font.BOLD, 10f));
                    g.drawString("P" + p.pid, x + 3, y + 14);
                }
            }
            g.setColor(Color.WHITE);
            if (t > 0 && cpu.get(t - 1) != p) { g.setColor(Cores.BORDA); g.drawLine(x, y, x, y + LIN - 4); }
        }

        // uma linha por processo
        Stroke fino = new BasicStroke(1f);
        for (int i = 0; i < ps.size(); i++) {
            Processo p = ps.get(i);
            y = TOPO + (i + 1) * LIN + 6;
            Color c = Cores.doProcesso(p.pid);
            g.setColor(c);
            g.setFont(getFont().deriveFont(Font.BOLD, 12f));
            g.drawString("P" + p.pid, 14, y + 15);
            for (int t = 0; t < ticks; t++) {
                Estado e = hist.get(t)[i];
                int x = ROTULO + t * CEL;
                if (e == Estado.EXECUCAO) { g.setColor(c); g.fillRect(x, y + 2, CEL, LIN - 8); }
                else if (e == Estado.PRONTO) { g.setColor(Cores.clara(c)); g.fillRect(x, y + 2, CEL, LIN - 8); }
                else if (e == Estado.ESPERA) {
                    g.setColor(new Color(0xD5D8DC));
                    g.fillRect(x, y + 2, CEL, LIN - 8);
                    g.setColor(new Color(0x99A3A4));
                    g.setStroke(fino);
                    Shape clipAntigo = g.getClip();
                    g.clipRect(x, y + 2, CEL, LIN - 8);
                    for (int k = -LIN; k < CEL; k += 6) g.drawLine(x + k, y + LIN - 6, x + k + LIN, y + 2 - 4);
                    g.setClip(clipAntigo);
                }
            }
            g.setColor(new Color(0xEAECEE));
            g.drawLine(ROTULO, y + LIN - 3, ROTULO + Math.max(ticks, 20) * CEL, y + LIN - 3);
        }

        // eixo do tempo
        int yEixo = TOPO + (ps.size() + 1) * LIN + 12;
        g.setColor(Cores.BORDA);
        g.drawLine(ROTULO, yEixo, ROTULO + Math.max(ticks, 20) * CEL, yEixo);
        g.setFont(getFont().deriveFont(10f));
        int passo = ticks > 60 ? 5 : 1;
        for (int t = 0; t <= Math.max(ticks, 20); t += passo) {
            int x = ROTULO + t * CEL;
            g.drawLine(x, yEixo, x, yEixo + 4);
            String s = String.valueOf(t);
            g.drawString(s, x - g.getFontMetrics().stringWidth(s) / 2, yEixo + 16);
        }

        // legenda
        int lx = ROTULO, ly = yEixo + 38;
        g.setFont(getFont().deriveFont(11f));
        legenda(g, lx, ly, new Color(0x2E86DE), "Execução");
        legenda(g, lx + 100, ly, Cores.clara(new Color(0x2E86DE)), "Pronto (esperando CPU)");
        legenda(g, lx + 270, ly, new Color(0xD5D8DC), "Espera (E/S)");
    }

    private void legenda(Graphics2D g, int x, int y, Color c, String s) {
        g.setColor(c);
        g.fillRect(x, y - 9, 14, 10);
        g.setColor(Cores.TEXTO_SUAVE);
        g.drawString(s, x + 18, y);
    }
}
