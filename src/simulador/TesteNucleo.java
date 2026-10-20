package simulador;

import simulador.algoritmos.*;
import java.util.List;

/** Confere o núcleo contra exemplos clássicos (Silberschatz) e mostra Gantt em texto. */
public class TesteNucleo {
    static int falhas = 0;

    static Simulador roda(Escalonador e, List<Processo> ps) {
        Simulador s = new Simulador(e, ps);
        s.executarTudo();
        return s;
    }

    static void confere(String nome, double obtido, double esperado) {
        boolean ok = Math.abs(obtido - esperado) < 0.01;
        if (!ok) falhas++;
        System.out.printf("%s %-40s obtido=%.2f esperado=%.2f%n", ok ? "[OK]  " : "[FALHA]", nome, obtido, esperado);
    }

    static String gantt(Simulador s) {
        StringBuilder sb = new StringBuilder("|");
        for (Processo p : s.getGantt()) sb.append(p == null ? "--" : p.toString()).append('|');
        return sb.toString();
    }

    public static void main(String[] args) {
        // Exemplo 1: P1=24, P2=3, P3=3, todos em t=0
        List<Processo> ex1 = List.of(new Processo(1,0,0,24), new Processo(2,0,0,3), new Processo(3,0,0,3));
        confere("FCFS espera média (24,3,3)", new Metricas(roda(new FCFS(), ex1)).esperaMedia, 17);
        confere("RR q=4 espera média (24,3,3)", new Metricas(roda(new RoundRobin(4), ex1)).esperaMedia, 17.0/3);

        // Exemplo 2: SJF com P1=6, P2=8, P3=7, P4=3 em t=0
        List<Processo> ex2 = List.of(new Processo(1,0,0,6), new Processo(2,0,0,8), new Processo(3,0,0,7), new Processo(4,0,0,3));
        confere("SJF espera média (6,8,7,3)", new Metricas(roda(new SJF(), ex2)).esperaMedia, 7);

        // Exemplo 3: SRTF com chegadas 0,1,2,3 e bursts 8,4,9,5
        List<Processo> ex3 = List.of(new Processo(1,0,0,8), new Processo(2,1,0,4), new Processo(3,2,0,9), new Processo(4,3,0,5));
        confere("SRTF espera média (8,4,9,5)", new Metricas(roda(new SRTF(), ex3)).esperaMedia, 6.5);

        // Exemplo 4: Prioridade não preemptiva, bursts 10,1,2,1,5 prioridades 3,1,4,5,2
        List<Processo> ex4 = List.of(new Processo(1,0,3,10), new Processo(2,0,1,1), new Processo(3,0,4,2),
                                     new Processo(4,0,5,1), new Processo(5,0,2,5));
        confere("Prioridade espera média", new Metricas(roda(new Prioridade(false, 0), ex4)).esperaMedia, 8.2);

        // Exemplo 5: E/S (estado Espera). P1: 3 CPU, 4 E/S, 2 CPU. P2: 5 CPU
        List<Processo> ex5 = List.of(new Processo(1,0,0,3,4,2), new Processo(2,0,0,5));
        Simulador s5 = roda(new FCFS(), ex5);
        System.out.println("\nFCFS com E/S: " + gantt(s5));
        confere("FCFS com E/S: fim de P1", s5.getProcessos().get(0).getFim(), 10);
        System.out.println("Log:"); s5.getLog().forEach(l -> System.out.println("  " + l));

        // Starvation: P1 de baixa prioridade e uma fila de processos prioritários chegando
        List<Processo> fome = new java.util.ArrayList<>();
        fome.add(new Processo(1, 0, 9, 3));
        for (int i = 0; i < 15; i++) fome.add(new Processo(i + 2, i * 2, 1, 2));
        Metricas sem = new Metricas(roda(new Prioridade(true, 0), fome));
        Metricas com = new Metricas(roda(new Prioridade(true, 2), fome));
        System.out.printf("%nStarvation -> maior espera sem aging: %d | com aging: %d%n", sem.maiorEspera, com.maiorEspera);
        if (com.maiorEspera >= sem.maiorEspera) { falhas++; System.out.println("[FALHA] aging não reduziu a espera"); }

        System.out.println(falhas == 0 ? "\nTodos os testes passaram." : "\n" + falhas + " falha(s).");
        System.exit(falhas == 0 ? 0 : 1);
    }
}
