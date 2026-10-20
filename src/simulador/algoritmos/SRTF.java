package simulador.algoritmos;

import simulador.*;
import java.util.List;

/** Shortest Remaining Time First: SJF preemptivo. Em empate, o atual continua. */
public class SRTF implements Escalonador {
    public Processo escolher(List<Processo> prontos, Processo atual, int fatiaUsada, int tempo) {
        Processo melhor = atual;
        for (Processo p : prontos)
            if (melhor == null || p.getRestante() < melhor.getRestante()) melhor = p;
        return melhor;
    }
    public String nome() { return "SRTF"; }
}
