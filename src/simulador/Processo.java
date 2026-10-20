package simulador;

/**
 * Um processo alterna rajadas de CPU e de E/S: rajadas = {CPU, E/S, CPU, E/S, CPU...}.
 * Índices pares são CPU, ímpares são E/S. Ex.: {5, 3, 2} = 5 de CPU, 3 de E/S, 2 de CPU.
 */
public class Processo {
    public final int pid;
    public final int chegada;
    public final int prioridade;      // menor número = mais prioritário
    public final int[] rajadas;

    // estado dinâmico
    Estado estado = null;             // null = ainda não chegou
    int rajadaAtual = 0;              // índice em rajadas
    int restante;                     // tempo restante da rajada atual
    int entradaNaFila;                // instante em que entrou em PRONTO (para aging)

    // tempos registrados (métricas)
    int primeiroInicio = -1;
    int fim = -1;
    int tempoEspera = 0;              // total de ticks em PRONTO
    int maiorEsperaContinua = 0;      // maior trecho seguido em PRONTO (indicador de starvation)
    int esperaContinua = 0;

    public Processo(int pid, int chegada, int prioridade, int... rajadas) {
        if (rajadas.length == 0 || rajadas.length % 2 == 0)
            throw new IllegalArgumentException("rajadas devem começar e terminar com CPU (tamanho ímpar)");
        this.pid = pid;
        this.chegada = chegada;
        this.prioridade = prioridade;
        this.rajadas = rajadas.clone();
        this.restante = rajadas[0];
    }

    /** Cópia "zerada", para rodar o mesmo conjunto em vários algoritmos. */
    public Processo copia() { return new Processo(pid, chegada, prioridade, rajadas); }

    public Estado getEstado() { return estado; }
    public int getRestante() { return restante; }
    public int getRajadaAtual() { return rajadaAtual; }
    public int getEntradaNaFila() { return entradaNaFila; }
    public int getPrimeiroInicio() { return primeiroInicio; }
    public int getFim() { return fim; }
    public int getTempoEspera() { return tempoEspera; }
    public int getMaiorEsperaContinua() { return maiorEsperaContinua; }

    public int cpuTotal() {
        int s = 0;
        for (int i = 0; i < rajadas.length; i += 2) s += rajadas[i];
        return s;
    }
    public int turnaround() { return fim - chegada; }
    public int resposta() { return primeiroInicio - chegada; }

    @Override public String toString() { return "P" + pid; }
}
