package simulador.algoritmos;

import simulador.*;
import java.util.List;

/** First-Come, First-Served: não preemptivo, ordem de chegada na fila. */
public class FCFS implements Escalonador {
    public Processo escolher(List<Processo> prontos, Processo atual, int fatiaUsada, int tempo) {
        if (atual != null) return atual;
        return prontos.isEmpty() ? null : prontos.get(0);
    }
    public String nome() { return "FCFS"; }
}
