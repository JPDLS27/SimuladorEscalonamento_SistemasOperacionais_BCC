package simulador;

import java.util.*;

/** Relógio discreto: a cada chamada de passo() avança uma unidade de tempo. */
public class Simulador {
    private final Escalonador escalonador;
    private final List<Processo> todos = new ArrayList<>();
    private final List<Processo> prontos = new ArrayList<>();
    private final List<Processo> espera = new ArrayList<>();
    private final List<Processo> gantt = new ArrayList<>();   // gantt.get(t) = quem rodou no tick t (null = ociosa)
    private final List<String> log = new ArrayList<>();
    private final List<Estado[]> historico = new ArrayList<>(); // estado de cada processo em cada tick (para o Gantt)
    private Processo atual = null;
    private int fatiaUsada = 0;
    private int tempo = 0;
    private int trocasDeContexto = 0;
    private boolean preparado = false;

    public Simulador(Escalonador escalonador, List<Processo> processos) {
        this.escalonador = escalonador;
        for (Processo p : processos) todos.add(p.copia());
        todos.sort(Comparator.comparingInt((Processo p) -> p.chegada).thenComparingInt(p -> p.pid));
        preparar();
    }

    public boolean terminou() {
        for (Processo p : todos) if (p.estado != Estado.TERMINADO) return false;
        return true;
    }

    /**
     * Executa um tick. Ao fim do passo, o estado já reflete a decisão para o instante seguinte
     * (chegadas, fim de E/S e escolha do escalonador), então a tela nunca mostra a CPU livre
     * com processos esperando na fila.
     */
    public void passo() {
        if (!preparado) preparar();
        if (terminou()) return;

        // Um tick passa: CPU executa, E/S avança, fila espera
        gantt.add(atual);
        Estado[] foto = new Estado[todos.size()];
        for (int i = 0; i < foto.length; i++) foto[i] = todos.get(i).estado;
        historico.add(foto);
        if (atual != null) { atual.restante--; fatiaUsada++; }
        for (Processo p : espera) p.restante--;
        for (Processo p : prontos) {
            p.tempoEspera++;
            p.esperaContinua++;
            p.maiorEsperaContinua = Math.max(p.maiorEsperaContinua, p.esperaContinua);
        }
        tempo++;

        // Rajada de CPU acabou: vai para ESPERA (E/S) ou TERMINADO
        if (atual != null && atual.restante == 0) {
            if (atual.rajadaAtual + 1 < atual.rajadas.length) {
                atual.rajadaAtual++;
                atual.restante = atual.rajadas[atual.rajadaAtual];
                atual.estado = Estado.ESPERA;
                espera.add(atual);
                log.add(tempo + ": " + atual + " -> ESPERA (E/S de " + atual.restante + ")");
            } else {
                atual.estado = Estado.TERMINADO;
                atual.fim = tempo;
                log.add(tempo + ": " + atual + " -> TERMINADO");
            }
            atual = null;
            fatiaUsada = 0;
        }
        preparar();
    }

    /** Prepara o instante 'tempo': chegadas, fim de E/S e decisão do escalonador. */
    private void preparar() {
        preparado = true;
        if (terminou()) return;

        // 1. Processos que chegam agora entram em PRONTO
        for (Processo p : todos)
            if (p.estado == null && p.chegada == tempo) paraPronto(p, "chegou");

        // 2. E/S: quem terminou volta para PRONTO
        Iterator<Processo> it = espera.iterator();
        while (it.hasNext()) {
            Processo p = it.next();
            if (p.restante == 0) {
                it.remove();
                p.rajadaAtual++;
                p.restante = p.rajadas[p.rajadaAtual];
                paraPronto(p, "terminou E/S");
            }
        }

        // 3. Escalonador decide (Pronto -> Execução, ou preempção Execução -> Pronto)
        Processo escolhido = escalonador.escolher(Collections.unmodifiableList(prontos), atual, fatiaUsada, tempo);
        if (escolhido != atual) {
            if (atual != null) {
                paraPronto(atual, "preempção");
                trocasDeContexto++;
            }
            if (escolhido != null) {
                prontos.remove(escolhido);
                escolhido.estado = Estado.EXECUCAO;
                escolhido.esperaContinua = 0;
                if (escolhido.primeiroInicio < 0) escolhido.primeiroInicio = tempo;
                log.add(tempo + ": " + escolhido + " -> EXECUÇÃO");
            }
            atual = escolhido;
            fatiaUsada = 0;
        }
    }

    public void executarTudo() {
        int limite = 1_000_000;
        while (!terminou() && limite-- > 0) passo();
    }

    private void paraPronto(Processo p, String motivo) {
        p.estado = Estado.PRONTO;
        p.entradaNaFila = tempo;
        prontos.add(p);
        log.add(tempo + ": " + p + " -> PRONTO (" + motivo + ")");
    }

    // ---- consultas (usadas pela interface gráfica e pelas métricas) ----
    public int getTempo() { return tempo; }
    public Processo getAtual() { return atual; }
    public List<Processo> getProcessos() { return Collections.unmodifiableList(todos); }
    public List<Processo> getProntos() { return Collections.unmodifiableList(prontos); }
    public List<Processo> getEspera() { return Collections.unmodifiableList(espera); }
    public List<Processo> getGantt() { return Collections.unmodifiableList(gantt); }
    public List<Estado[]> getHistorico() { return Collections.unmodifiableList(historico); }
    public List<String> getLog() { return Collections.unmodifiableList(log); }
    public int getTrocasDeContexto() { return trocasDeContexto; }
    public Escalonador getEscalonador() { return escalonador; }
}
