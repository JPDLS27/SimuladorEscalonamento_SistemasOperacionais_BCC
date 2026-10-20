package simulador.algoritmos;

import simulador.*;
import java.util.List;

/**
 * Prioridade (menor número = mais prioritário), preemptiva ou não.
 * Com aging > 0, a cada 'aging' ticks na fila o processo ganha 1 nível de prioridade,
 * o que evita starvation.
 */
public class Prioridade implements Escalonador {
    private final boolean preemptivo;
    private final int aging;   // 0 = sem aging

    public Prioridade(boolean preemptivo, int aging) {
        this.preemptivo = preemptivo;
        this.aging = aging;
    }

    private int efetiva(Processo p, int tempo) {
        if (aging <= 0 || p.getEstado() != Estado.PRONTO) return p.prioridade;
        return p.prioridade - (tempo - p.getEntradaNaFila()) / aging;
    }

    public Processo escolher(List<Processo> prontos, Processo atual, int fatiaUsada, int tempo) {
        if (atual != null && !preemptivo) return atual;
        Processo melhor = atual;
        for (Processo p : prontos)
            if (melhor == null || efetiva(p, tempo) < efetiva(melhor, tempo)) melhor = p;
        return melhor;
    }

    public String nome() {
        return "Prioridade" + (preemptivo ? " preemptiva" : "") + (aging > 0 ? " + aging(" + aging + ")" : "");
    }
}
