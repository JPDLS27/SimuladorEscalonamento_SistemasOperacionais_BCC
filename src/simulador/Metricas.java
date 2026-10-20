package simulador;

/** Uma métrica por objetivo do slide "Objetivos". */
public class Metricas {
    public final double esperaMedia, turnaroundMedio, respostaMedia;
    public final double utilizacaoCpu;   // "manter a CPU ocupada" (0..1)
    public final double throughput;      // processos concluídos por unidade de tempo
    public final int maiorEspera;        // "evitar starvation": pior espera contínua na fila
    public final int tempoTotal, trocasDeContexto;

    public Metricas(Simulador s) {
        var ps = s.getProcessos();
        int n = ps.size();
        double e = 0, t = 0, r = 0; int maior = 0;
        for (Processo p : ps) {
            e += p.getTempoEspera(); t += p.turnaround(); r += p.resposta();
            maior = Math.max(maior, p.getMaiorEsperaContinua());
        }
        long ocupado = s.getGantt().stream().filter(x -> x != null).count();
        tempoTotal = s.getTempo();
        esperaMedia = e / n; turnaroundMedio = t / n; respostaMedia = r / n;
        utilizacaoCpu = tempoTotal == 0 ? 0 : (double) ocupado / tempoTotal;
        throughput = tempoTotal == 0 ? 0 : (double) n / tempoTotal;
        maiorEspera = maior;
        trocasDeContexto = s.getTrocasDeContexto();
    }
}
