package simulador.algoritmos;

import simulador.*;
import java.util.Comparator;
import java.util.List;

/** Shortest Job First: não preemptivo, escolhe a menor rajada de CPU. */
public class SJF implements Escalonador {
    public Processo escolher(List<Processo> prontos, Processo atual, int fatiaUsada, int tempo) {
        if (atual != null) return atual;
        return prontos.stream().min(Comparator.comparingInt(Processo::getRestante)).orElse(null);
    }
    public String nome() { return "SJF"; }
}
