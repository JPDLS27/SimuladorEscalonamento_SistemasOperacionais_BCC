package simulador.gui;

import simulador.*;
import simulador.algoritmos.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/** Janela principal do simulador. */
public class TelaPrincipal extends JFrame {
    private static final String[] ALGORITMOS = {"FCFS", "SJF", "SRTF", "Prioridade", "Round Robin"};

    private final DefaultTableModel modeloProcessos = new DefaultTableModel(
            new String[]{"PID", "Chegada", "Prioridade", "Rajadas (CPU,E/S,CPU...)"}, 0) {
        @Override public Class<?> getColumnClass(int c) { return c == 3 ? String.class : Integer.class; }
        @Override public boolean isCellEditable(int r, int c) { return c != 0; }
    };
    private final JTable tabelaProcessos = new JTable(modeloProcessos);

    private final JComboBox<String> comboAlgoritmo = new JComboBox<>(ALGORITMOS);
    private final JSpinner spQuantum = new JSpinner(new SpinnerNumberModel(2, 1, 50, 1));
    private final JCheckBox ckPreemptivo = new JCheckBox("Preemptiva", true);
    private final JSpinner spAging = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
    private final JSlider slVelocidade = new JSlider(1, 20, 6);

    private final JButton btPasso = new JButton("Passo ▸");
    private final JButton btExecutar = new JButton("Executar ▶");
    private final JButton btFim = new JButton("Até o fim ⏭");
    private final JButton btReiniciar = new JButton("Reiniciar ↺");

    private final PainelEstados painelEstados = new PainelEstados();
    private final PainelGantt painelGantt = new PainelGantt();
    private final DefaultTableModel modeloMetricas = new DefaultTableModel(
            new String[]{"PID", "Chegada", "CPU total", "1ª execução", "Fim", "Espera", "Turnaround", "Resposta", "Maior espera seguida"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JLabel lblResumo = new JLabel(" ");
    private final JTextArea txtLog = new JTextArea();

    private final javax.swing.Timer timer = new javax.swing.Timer(300, e -> passo());
    private Simulador sim;
    private int proximoPid = 1;

    public TelaPrincipal() {
        super("Simulador de Escalonamento de Processos");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(new EmptyBorder(8, 8, 8, 8));

        add(montarLateral(), BorderLayout.WEST);
        add(montarCentro(), BorderLayout.CENTER);

        carregarExemplo();
        atualizarOpcoes();
        ligarEventos();
        reiniciar();

        setSize(1360, 860);
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------ montagem
    private JComponent montarLateral() {
        JPanel lateral = new JPanel();
        lateral.setLayout(new BoxLayout(lateral, BoxLayout.Y_AXIS));
        lateral.setPreferredSize(new Dimension(390, 0));

        // processos
        JPanel pProc = new JPanel(new BorderLayout(4, 4));
        pProc.setBorder(new TitledBorder("Processos"));
        tabelaProcessos.setRowHeight(22);
        tabelaProcessos.getColumnModel().getColumn(0).setPreferredWidth(35);
        tabelaProcessos.getColumnModel().getColumn(1).setPreferredWidth(60);
        tabelaProcessos.getColumnModel().getColumn(2).setPreferredWidth(70);
        tabelaProcessos.getColumnModel().getColumn(3).setPreferredWidth(170);
        tabelaProcessos.putClientProperty("terminateEditOnFocusLost", true);
        tabelaProcessos.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, "P" + v, s, f, r, c);
                l.setForeground(Cores.doProcesso((Integer) v));
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                return l;
            }
        });
        JScrollPane sp = new JScrollPane(tabelaProcessos);
        sp.setPreferredSize(new Dimension(360, 230));
        pProc.add(sp, BorderLayout.CENTER);

        JPanel botoesProc = new JPanel(new GridLayout(2, 2, 4, 4));
        JButton btAdd = new JButton("Adicionar"), btRem = new JButton("Remover");
        JButton btAleat = new JButton("Aleatório"), btEx = new JButton("Exemplo da aula");
        btAdd.addActionListener(e -> { modeloProcessos.addRow(new Object[]{proximoPid++, 0, 1, "4"}); reiniciar(); });
        btRem.addActionListener(e -> {
            int[] sel = tabelaProcessos.getSelectedRows();
            for (int i = sel.length - 1; i >= 0; i--) modeloProcessos.removeRow(sel[i]);
            reiniciar();
        });
        btAleat.addActionListener(e -> { gerarAleatorio(); reiniciar(); });
        btEx.addActionListener(e -> { carregarExemplo(); reiniciar(); });
        botoesProc.add(btAdd); botoesProc.add(btRem); botoesProc.add(btAleat); botoesProc.add(btEx);
        pProc.add(botoesProc, BorderLayout.SOUTH);
        JLabel dica = new JLabel("<html><small>Rajadas: \"5\" = só CPU; \"3,4,2\" = 3 de CPU, 4 de E/S, 2 de CPU. "
                + "Prioridade: número menor = mais prioritário.</small></html>");
        dica.setForeground(Cores.TEXTO_SUAVE);
        pProc.add(dica, BorderLayout.NORTH);

        // algoritmo
        JPanel pAlg = new JPanel(new GridBagLayout());
        pAlg.setBorder(new TitledBorder("Algoritmo"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4); c.anchor = GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0; pAlg.add(new JLabel("Escalonador:"), c);
        c.gridx = 1; c.weightx = 1; pAlg.add(comboAlgoritmo, c);
        c.gridx = 0; c.gridy = 1; c.weightx = 0; pAlg.add(new JLabel("Quantum:"), c);
        c.gridx = 1; pAlg.add(spQuantum, c);
        c.gridx = 0; c.gridy = 2; pAlg.add(new JLabel("Aging (0 = sem):"), c);
        c.gridx = 1; pAlg.add(spAging, c);
        c.gridx = 1; c.gridy = 3; pAlg.add(ckPreemptivo, c);
        spAging.setToolTipText("A cada N ticks na fila, o processo ganha 1 nível de prioridade (evita starvation)");

        // controles
        JPanel pCtl = new JPanel(new GridBagLayout());
        pCtl.setBorder(new TitledBorder("Simulação"));
        c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4); c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        c.gridx = 0; c.gridy = 0; pCtl.add(btPasso, c);
        c.gridx = 1; pCtl.add(btExecutar, c);
        c.gridx = 0; c.gridy = 1; pCtl.add(btFim, c);
        c.gridx = 1; pCtl.add(btReiniciar, c);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        JPanel vel = new JPanel(new BorderLayout(4, 0));
        vel.add(new JLabel("Velocidade:"), BorderLayout.WEST);
        vel.add(slVelocidade, BorderLayout.CENTER);
        pCtl.add(vel, c);
        JButton btComparar = new JButton("Comparar todos os algoritmos");
        btComparar.setFont(btComparar.getFont().deriveFont(Font.BOLD));
        btComparar.addActionListener(e -> comparar());
        c.gridy = 3; pCtl.add(btComparar, c);

        for (JComponent j : new JComponent[]{pProc, pAlg, pCtl}) {
            j.setAlignmentX(Component.LEFT_ALIGNMENT);
            lateral.add(j);
            lateral.add(Box.createVerticalStrut(6));
        }
        pAlg.setMaximumSize(new Dimension(Integer.MAX_VALUE, pAlg.getPreferredSize().height));
        pCtl.setMaximumSize(new Dimension(Integer.MAX_VALUE, pCtl.getPreferredSize().height));
        return lateral;
    }

    private JComponent montarCentro() {
        JPanel topo = new JPanel(new BorderLayout());
        topo.setBorder(new TitledBorder("Estados dos processos"));
        topo.add(painelEstados);

        JScrollPane spGantt = new JScrollPane(painelGantt);
        spGantt.setBorder(new TitledBorder("Gráfico de Gantt"));
        spGantt.getHorizontalScrollBar().setUnitIncrement(24);

        JTable tabelaMetricas = new JTable(modeloMetricas);
        tabelaMetricas.setRowHeight(22);
        DefaultTableCellRenderer centro = new DefaultTableCellRenderer();
        centro.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < modeloMetricas.getColumnCount(); i++) tabelaMetricas.getColumnModel().getColumn(i).setCellRenderer(centro);
        JPanel pMet = new JPanel(new BorderLayout(4, 4));
        pMet.add(new JScrollPane(tabelaMetricas), BorderLayout.CENTER);
        lblResumo.setBorder(new EmptyBorder(4, 6, 4, 6));
        pMet.add(lblResumo, BorderLayout.SOUTH);

        txtLog.setEditable(false);
        txtLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Métricas", pMet);
        abas.addTab("Log de transições", new JScrollPane(txtLog));

        JSplitPane baixo = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spGantt, abas);
        baixo.setResizeWeight(0.62);
        JSplitPane tudo = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topo, baixo);
        tudo.setResizeWeight(0.38);
        return tudo;
    }

    private void ligarEventos() {
        btPasso.addActionListener(e -> passo());
        btExecutar.addActionListener(e -> alternarExecucao());
        btFim.addActionListener(e -> {
            timer.stop();
            if (garantirSimulador()) { sim.executarTudo(); atualizarTela(); }
        });
        btReiniciar.addActionListener(e -> reiniciar());
        comboAlgoritmo.addActionListener(e -> { atualizarOpcoes(); reiniciar(); });
        spQuantum.addChangeListener(e -> reiniciar());
        spAging.addChangeListener(e -> reiniciar());
        ckPreemptivo.addActionListener(e -> reiniciar());
        slVelocidade.addChangeListener(e -> timer.setDelay(velocidadeMs()));
        modeloProcessos.addTableModelListener(e -> { if (sim != null && sim.getTempo() > 0) reiniciar(); });
    }

    // ------------------------------------------------------------ lógica
    private int velocidadeMs() { return 1050 - slVelocidade.getValue() * 50; }

    private void atualizarOpcoes() {
        String alg = (String) comboAlgoritmo.getSelectedItem();
        spQuantum.setEnabled("Round Robin".equals(alg));
        boolean prio = "Prioridade".equals(alg);
        spAging.setEnabled(prio);
        ckPreemptivo.setEnabled(prio);
    }

    private Escalonador criarEscalonador() {
        return switch ((String) comboAlgoritmo.getSelectedItem()) {
            case "SJF" -> new SJF();
            case "SRTF" -> new SRTF();
            case "Prioridade" -> new Prioridade(ckPreemptivo.isSelected(), (Integer) spAging.getValue());
            case "Round Robin" -> new RoundRobin((Integer) spQuantum.getValue());
            default -> new FCFS();
        };
    }

    /** Lê a tabela de processos. Mostra erro e devolve null se algo estiver inválido. */
    private List<Processo> lerProcessos() {
        if (tabelaProcessos.isEditing()) tabelaProcessos.getCellEditor().stopCellEditing();
        List<Processo> ps = new ArrayList<>();
        for (int r = 0; r < modeloProcessos.getRowCount(); r++) {
            Object pid = modeloProcessos.getValueAt(r, 0);
            try {
                int chegada = (Integer) modeloProcessos.getValueAt(r, 1);
                int prio = (Integer) modeloProcessos.getValueAt(r, 2);
                String[] partes = String.valueOf(modeloProcessos.getValueAt(r, 3)).trim().split("\\s*[,; ]\\s*");
                int[] raj = new int[partes.length];
                for (int i = 0; i < partes.length; i++) {
                    raj[i] = Integer.parseInt(partes[i]);
                    if (raj[i] <= 0) throw new IllegalArgumentException("as rajadas devem ser maiores que zero");
                }
                if (chegada < 0) throw new IllegalArgumentException("a chegada não pode ser negativa");
                ps.add(new Processo((Integer) pid, chegada, prio, raj));
            } catch (NumberFormatException | NullPointerException ex) {
                return erro("P" + pid + ": rajadas inválidas. Use números separados por vírgula, ex.: 3,4,2");
            } catch (IllegalArgumentException ex) {
                return erro("P" + pid + ": " + ex.getMessage());
            }
        }
        if (ps.isEmpty()) return erro("Adicione pelo menos um processo.");
        return ps;
    }

    private List<Processo> erro(String msg) {
        timer.stop();
        btExecutar.setText("Executar ▶");
        JOptionPane.showMessageDialog(this, msg, "Processo inválido", JOptionPane.WARNING_MESSAGE);
        return null;
    }

    private boolean garantirSimulador() {
        if (sim != null) return true;
        List<Processo> ps = lerProcessos();
        if (ps == null) return false;
        sim = new Simulador(criarEscalonador(), ps);
        return true;
    }

    private void passo() {
        if (!garantirSimulador()) return;
        if (sim.terminou()) { timer.stop(); btExecutar.setText("Executar ▶"); return; }
        sim.passo();
        atualizarTela();
    }

    private void alternarExecucao() {
        if (timer.isRunning()) { timer.stop(); btExecutar.setText("Executar ▶"); return; }
        if (!garantirSimulador()) return;
        if (sim.terminou()) reiniciar();
        timer.setDelay(velocidadeMs());
        timer.start();
        btExecutar.setText("Pausar ⏸");
    }

    private void reiniciar() {
        timer.stop();
        btExecutar.setText("Executar ▶");
        sim = null;
        // simulador "vazio" só para desenhar o estado inicial, se a tabela for válida
        List<Processo> ps = lerProcessosSilencioso();
        if (ps != null) sim = new Simulador(criarEscalonador(), ps);
        atualizarTela();
    }

    private List<Processo> lerProcessosSilencioso() {
        try {
            List<Processo> ps = new ArrayList<>();
            for (int r = 0; r < modeloProcessos.getRowCount(); r++) {
                String[] partes = String.valueOf(modeloProcessos.getValueAt(r, 3)).trim().split("\\s*[,; ]\\s*");
                int[] raj = Arrays.stream(partes).mapToInt(Integer::parseInt).toArray();
                if (Arrays.stream(raj).anyMatch(x -> x <= 0)) return null;
                ps.add(new Processo((Integer) modeloProcessos.getValueAt(r, 0), (Integer) modeloProcessos.getValueAt(r, 1),
                        (Integer) modeloProcessos.getValueAt(r, 2), raj));
            }
            return ps.isEmpty() ? null : ps;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void atualizarTela() {
        painelEstados.setSimulador(sim);
        painelGantt.setSimulador(sim);
        modeloMetricas.setRowCount(0);
        if (sim == null) { lblResumo.setText(" "); txtLog.setText(""); return; }

        for (Processo p : sim.getProcessos()) {
            boolean fim = p.getEstado() == Estado.TERMINADO;
            boolean iniciou = p.getPrimeiroInicio() >= 0;
            modeloMetricas.addRow(new Object[]{
                "P" + p.pid, p.chegada, p.cpuTotal(),
                iniciou ? p.getPrimeiroInicio() : "-",
                fim ? p.getFim() : "-",
                p.getTempoEspera(),
                fim ? p.turnaround() : "-",
                iniciou ? p.resposta() : "-",
                p.getMaiorEsperaContinua()
            });
        }
        if (sim.terminou()) {
            Metricas m = new Metricas(sim);
            lblResumo.setText(String.format("<html><b>Concluído em t=%d</b> &nbsp;|&nbsp; Espera média: <b>%.2f</b> &nbsp;|&nbsp; "
                    + "Turnaround médio: <b>%.2f</b> &nbsp;|&nbsp; Resposta média: <b>%.2f</b><br>"
                    + "Utilização da CPU: <b>%.1f%%</b> &nbsp;|&nbsp; Throughput: <b>%.3f</b> proc/tick &nbsp;|&nbsp; "
                    + "Maior espera seguida (starvation): <b>%d</b> &nbsp;|&nbsp; Preempções: <b>%d</b></html>",
                    m.tempoTotal, m.esperaMedia, m.turnaroundMedio, m.respostaMedia, m.utilizacaoCpu * 100,
                    m.throughput, m.maiorEspera, m.trocasDeContexto));
            timer.stop();
            btExecutar.setText("Executar ▶");
        } else {
            lblResumo.setText("Simulando... as médias aparecem quando todos os processos terminarem.");
        }
        txtLog.setText(String.join("\n", sim.getLog()));
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
    }

    private void comparar() {
        List<Processo> ps = lerProcessos();
        if (ps == null) return;
        int q = (Integer) spQuantum.getValue();
        int aging = Math.max(1, (Integer) spAging.getValue() == 0 ? 3 : (Integer) spAging.getValue());
        List<Escalonador> todos = List.of(new FCFS(), new SJF(), new SRTF(),
                new Prioridade(false, 0), new Prioridade(true, 0), new Prioridade(true, aging), new RoundRobin(q));
        new DialogoComparacao(this, todos, ps).setVisible(true);
    }

    // ------------------------------------------------------------ dados de exemplo
    private void carregarExemplo() {
        modeloProcessos.setRowCount(0);
        Object[][] ex = {
            {1, 0, 3, "6,3,2"},
            {2, 1, 1, "3"},
            {3, 2, 4, "8"},
            {4, 3, 2, "2,4,2"},
            {5, 5, 5, "4"},
        };
        for (Object[] l : ex) modeloProcessos.addRow(l);
        proximoPid = 6;
    }

    private void gerarAleatorio() {
        Random r = new Random();
        modeloProcessos.setRowCount(0);
        int n = 5 + r.nextInt(3);
        for (int i = 1; i <= n; i++) {
            String raj = r.nextBoolean()
                    ? String.valueOf(2 + r.nextInt(7))
                    : (1 + r.nextInt(5)) + "," + (2 + r.nextInt(4)) + "," + (1 + r.nextInt(4));
            modeloProcessos.addRow(new Object[]{i, r.nextInt(10), 1 + r.nextInt(5), raj});
        }
        proximoPid = n + 1;
    }
}
