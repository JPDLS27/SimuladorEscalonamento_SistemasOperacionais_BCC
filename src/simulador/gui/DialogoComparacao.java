package simulador.gui;

import simulador.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

/** Roda o mesmo conjunto de processos em todos os algoritmos e compara as métricas. */
class DialogoComparacao extends JDialog {
    // colunas e se "menor é melhor"
    private static final String[] COLS = {"Algoritmo", "Espera média", "Turnaround médio", "Resposta média",
            "Maior espera seguida", "Utilização CPU", "Throughput", "Preempções", "Tempo total"};
    private static final boolean[] MENOR_MELHOR = {false, true, true, true, true, false, false, true, true};

    DialogoComparacao(Frame dono, List<Escalonador> algoritmos, List<Processo> processos) {
        super(dono, "Comparação entre algoritmos", true);
        double[][] v = new double[algoritmos.size()][COLS.length];
        Object[][] linhas = new Object[algoritmos.size()][COLS.length];
        for (int i = 0; i < algoritmos.size(); i++) {
            Simulador s = new Simulador(algoritmos.get(i), processos);
            s.executarTudo();
            Metricas m = new Metricas(s);
            v[i] = new double[]{0, m.esperaMedia, m.turnaroundMedio, m.respostaMedia, m.maiorEspera,
                    m.utilizacaoCpu, m.throughput, m.trocasDeContexto, m.tempoTotal};
            linhas[i] = new Object[]{algoritmos.get(i).nome(),
                    String.format("%.2f", m.esperaMedia), String.format("%.2f", m.turnaroundMedio),
                    String.format("%.2f", m.respostaMedia), m.maiorEspera,
                    String.format("%.1f%%", m.utilizacaoCpu * 100), String.format("%.3f", m.throughput),
                    m.trocasDeContexto, m.tempoTotal};
        }
        // melhor valor de cada coluna
        double[] melhor = new double[COLS.length];
        for (int c = 1; c < COLS.length; c++) {
            melhor[c] = v[0][c];
            for (double[] l : v) melhor[c] = MENOR_MELHOR[c] ? Math.min(melhor[c], l[c]) : Math.max(melhor[c], l[c]);
        }

        JTable t = new JTable(new DefaultTableModel(linhas, COLS) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        });
        t.setRowHeight(26);
        t.getColumnModel().getColumn(0).setPreferredWidth(230);
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object val, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, val, s, f, r, c);
                l.setHorizontalAlignment(c == 0 ? LEFT : CENTER);
                boolean top = c > 0 && Math.abs(v[r][c] - melhor[c]) < 1e-9;
                l.setFont(l.getFont().deriveFont(top ? Font.BOLD : Font.PLAIN));
                if (!s) l.setBackground(top ? new Color(0xD5F5E3) : Color.WHITE);
                return l;
            }
        });

        JLabel legenda = new JLabel("<html>Em <b style='color:#1E8449'>verde</b>: melhor valor da coluna. "
                + "Cada métrica corresponde a um objetivo do escalonador: CPU ocupada (utilização), throughput, "
                + "tempo de resposta e starvation (maior espera seguida na fila).</html>");
        legenda.setBorder(new EmptyBorder(8, 8, 8, 8));

        setLayout(new BorderLayout());
        add(new JScrollPane(t), BorderLayout.CENTER);
        add(legenda, BorderLayout.SOUTH);
        setSize(1240, 340);
        setLocationRelativeTo(dono);
    }
}
