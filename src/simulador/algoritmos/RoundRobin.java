package simulador.algoritmos;

import simulador.*;
import java.util.List;

/** Round Robin: cada processo roda no máximo 'quantum' ticks e volta ao fim da fila. */
public class RoundRobin implements Escalonador {
    private final int quantum;
    public RoundRobin(int quantum) { this.quantum = quantum; }

    public Processo escolher(List<Processo> prontos, Processo atual, int fatiaUsada, int tempo) {
        if (atual != null && (fatiaUsada < quantum || prontos.isEmpty())) return atual;
        return prontos.isEmpty() ? null : prontos.get(0);
    }
    public String nome() { return "Round Robin (q=" + quantum + ")"; }
}
