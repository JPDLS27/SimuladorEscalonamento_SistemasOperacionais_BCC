package simulador.gui;

import simulador.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

/** Desenha o diagrama da aula (Execução / Pronto / Espera) com os processos dentro de cada estado. */
class PainelEstados extends JPanel {
    private Simulador sim;

    PainelEstados() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(640, 330));
    }

    void setSimulador(Simulador s) { this.sim = s; repaint(); }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        // posições do triângulo, como no slide
        Ellipse2D exec   = elipse(w * 0.50, h * 0.20, w * 0.30, h * 0.30);
        Ellipse2D espera = elipse(w * 0.18, h * 0.72, w * 0.28, h * 0.32);
        Ellipse2D pronto = elipse(w * 0.76, h * 0.72, w * 0.40, h * 0.38);

        g.setStroke(new BasicStroke(2f));
        g.setColor(Cores.BORDA);
        linha(g, exec, espera);
        linha(g, espera, pronto);
        linha(g, pronto, exec);

        // seta do escalonador (Pronto -> Execução), paralela à linha entre os dois
        Point2D de = borda(pronto, exec.getCenterX(), exec.getCenterY());
        Point2D ate = borda(exec, pronto.getCenterX(), pronto.getCenterY());
        double dx = ate.getX() - de.getX(), dy = ate.getY() - de.getY(), len = Math.hypot(dx, dy);
        double nx = -dy / len * 22, ny = dx / len * 22;          // deslocamento perpendicular (para a direita)
        if (nx < 0) { nx = -nx; ny = -ny; }
        double enc = 0.12;                                       // encurta as pontas
        g.setColor(new Color(0x1F618D));
        seta(g, de.getX() + dx * enc + nx, de.getY() + dy * enc + ny, ate.getX() - dx * enc + nx, ate.getY() - dy * enc + ny);
        String nomeEsc = sim == null ? "Escalonador" : "Escalonador: " + sim.getEscalonador().nome();
        g.setFont(getFont().deriveFont(Font.BOLD, 12f));
        FontMetrics fm = g.getFontMetrics();
        int lw = fm.stringWidth(nomeEsc) + 12;
        int mx = (int) ((de.getX() + ate.getX()) / 2 + nx + 8), my = (int) ((de.getY() + ate.getY()) / 2 + ny);
        mx = Math.min(mx, w - lw - 6);
        g.setColor(Color.WHITE);
        g.fillRect(mx, my - 12, lw, 22);
        g.setColor(Cores.BORDA);
        g.drawRect(mx, my - 12, lw, 22);
        g.drawString(nomeEsc, mx + 6, my + 4);

        List<Processo> emExec = new ArrayList<>(), emEspera = new ArrayList<>(), terminados = new ArrayList<>(), futuros = new ArrayList<>();
        List<Processo> prontos = sim == null ? List.of() : sim.getProntos();
        if (sim != null) {
            if (sim.getAtual() != null) emExec.add(sim.getAtual());
            emEspera.addAll(sim.getEspera());
            for (Processo p : sim.getProcessos()) {
                if (p.getEstado() == Estado.TERMINADO) terminados.add(p);
                else if (p.getEstado() == null) futuros.add(p);
            }
        }
        estado(g, exec, "Execução", emExec, "CPU livre");
        estado(g, espera, "Espera", emEspera, "sem E/S");
        estado(g, pronto, "Pronto", prontos, "fila vazia");

        // relógio e listas laterais
        g.setFont(getFont().deriveFont(Font.BOLD, 20f));
        g.setColor(Cores.BORDA);
        g.drawString("t = " + (sim == null ? 0 : sim.getTempo()), 14, 30);
        g.setFont(getFont().deriveFont(12f));
        g.setColor(Cores.TEXTO_SUAVE);
        int y = 54;
        g.drawString("Ainda não chegaram:", 14, y);
        fileira(g, futuros, 14, y + 6, true);
        g.setColor(Cores.TEXTO_SUAVE);
        g.drawString("Terminados:", w - 230, 24);
        fileira(g, terminados, w - 230, 30, false);
    }

    private void estado(Graphics2D g, Ellipse2D e, String titulo, List<Processo> ps, String vazio) {
        g.setColor(Cores.ESTADO);
        g.fill(e);
        g.setColor(Cores.BORDA);
        g.setStroke(new BasicStroke(2f));
        g.draw(e);
        g.setFont(getFont().deriveFont(Font.BOLD, 15f));
        FontMetrics fm = g.getFontMetrics();
        int tx = (int) (e.getCenterX() - fm.stringWidth(titulo) / 2.0);
        int ty = (int) (e.getMinY() + e.getHeight() * 0.28);
        g.drawString(titulo, tx, ty);

        if (ps.isEmpty()) {
            g.setFont(getFont().deriveFont(Font.ITALIC, 11f));
            g.setColor(Cores.TEXTO_SUAVE);
            fm = g.getFontMetrics();
            g.drawString(vazio, (int) (e.getCenterX() - fm.stringWidth(vazio) / 2.0), ty + 26);
            return;
        }
        // chips em fileiras dentro da elipse (ordem = ordem da fila)
        int chipW = 46, chipH = 30, gap = 5;
        int porLinha = Math.max(1, (int) ((e.getWidth() * 0.78) / (chipW + gap)));
        int linhas = (ps.size() + porLinha - 1) / porLinha;
        int y0 = ty + 8;
        if (y0 + linhas * (chipH + gap) > e.getMaxY() - 4) y0 = (int) (e.getMaxY() - 4 - linhas * (chipH + gap));
        for (int i = 0; i < ps.size(); i++) {
            int lin = i / porLinha, col = i % porLinha;
            int nNaLinha = Math.min(porLinha, ps.size() - lin * porLinha);
            int larg = nNaLinha * chipW + (nNaLinha - 1) * gap;
            int x = (int) (e.getCenterX() - larg / 2.0) + col * (chipW + gap);
            chip(g, ps.get(i), x, y0 + lin * (chipH + gap), chipW, chipH, true);
        }
    }

    private void fileira(Graphics2D g, List<Processo> ps, int x, int y, boolean mostrarChegada) {
        for (int i = 0; i < ps.size(); i++) {
            Processo p = ps.get(i);
            int cx = x + (i % 6) * 36, cy = y + (i / 6) * 26;
            g.setColor(mostrarChegada ? Cores.clara(Cores.doProcesso(p.pid)) : Cores.doProcesso(p.pid));
            g.fillRoundRect(cx, cy, 32, 22, 10, 10);
            g.setColor(mostrarChegada ? Cores.BORDA : Color.WHITE);
            g.setFont(getFont().deriveFont(Font.BOLD, 11f));
            String s = "P" + p.pid;
            g.drawString(s, cx + 16 - g.getFontMetrics().stringWidth(s) / 2, cy + 15);
            if (mostrarChegada) {
                g.setColor(Cores.TEXTO_SUAVE);
                g.setFont(getFont().deriveFont(9f));
                g.drawString("t" + p.chegada, cx + 8, cy + 31);
            }
        }
    }

    private void chip(Graphics2D g, Processo p, int x, int y, int cw, int ch, boolean comRestante) {
        g.setColor(Cores.doProcesso(p.pid));
        g.fillRoundRect(x, y, cw, ch, 12, 12);
        g.setColor(Color.WHITE);
        g.setFont(getFont().deriveFont(Font.BOLD, 12f));
        FontMetrics fm = g.getFontMetrics();
        String s = "P" + p.pid;
        g.drawString(s, x + cw / 2 - fm.stringWidth(s) / 2, y + 13);
        if (comRestante) {
            g.setFont(getFont().deriveFont(10f));
            fm = g.getFontMetrics();
            String r = "r=" + p.getRestante();
            g.drawString(r, x + cw / 2 - fm.stringWidth(r) / 2, y + 25);
        }
    }

    private static Ellipse2D elipse(double cx, double cy, double w, double h) {
        return new Ellipse2D.Double(cx - w / 2, cy - h / 2, w, h);
    }

    private static void linha(Graphics2D g, Ellipse2D a, Ellipse2D b) {
        Point2D pa = borda(a, b.getCenterX(), b.getCenterY()), pb = borda(b, a.getCenterX(), a.getCenterY());
        g.draw(new Line2D.Double(pa, pb));
    }

    /** Ponto na borda da elipse na direção de (tx, ty). */
    private static Point2D borda(Ellipse2D e, double tx, double ty) {
        double ang = Math.atan2(ty - e.getCenterY(), tx - e.getCenterX());
        return new Point2D.Double(e.getCenterX() + e.getWidth() / 2 * Math.cos(ang), e.getCenterY() + e.getHeight() / 2 * Math.sin(ang));
    }

    private static void seta(Graphics2D g, double x1, double y1, double x2, double y2) {
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
        double ang = Math.atan2(y2 - y1, x2 - x1), t = 12;
        Path2D cab = new Path2D.Double();
        cab.moveTo(x2, y2);
        cab.lineTo(x2 - t * Math.cos(ang - 0.4), y2 - t * Math.sin(ang - 0.4));
        cab.lineTo(x2 - t * Math.cos(ang + 0.4), y2 - t * Math.sin(ang + 0.4));
        cab.closePath();
        g.fill(cab);
    }
}
