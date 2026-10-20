package simulador;

import java.util.List;

/** Cada algoritmo implementa esta interface. Trocar de algoritmo = trocar de classe. */
public interface Escalonador {
    /**
     * Decide quem usa a CPU no próximo tick.
     * @param prontos    fila de prontos, em ordem de chegada na fila (não inclui o atual)
     * @param atual      processo em execução, ou null se a CPU está livre
     * @param fatiaUsada ticks seguidos que o atual já executou (para Round Robin)
     * @param tempo      relógio atual
     * @return o processo que vai executar (pode ser o próprio atual), ou null se não há ninguém.
     *         Se retornar outro processo com atual != null, o atual sofre preempção e volta para PRONTO.
     */
    Processo escolher(List<Processo> prontos, Processo atual, int fatiaUsada, int tempo);

    String nome();
}
