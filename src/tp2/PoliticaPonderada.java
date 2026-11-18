package tp2;

import java.util.List;
import java.util.Random;

/**
 * Resuelve un conflicto estructural (un conjunto de transiciones que comparten plaza de
 * entrada) asignando, para cada token que llega al conflicto, UNA transición ganadora
 * sorteada según pesos. Mientras ese token no se consuma, solo la ganadora está permitida.
 *
 * No conoce la red: recibe por constructor qué transiciones forman el conflicto y sus pesos.
 */
public class PoliticaPonderada implements Politica {

    private final String nombre;
    private final int[] conflicto;
    private final double[] pesos;
    private final Random random = new Random();
    private int asignada = -1; // transición del conflicto habilitada para el próximo token

    public PoliticaPonderada(String nombre, int[] conflicto, double[] pesos) {
        this.nombre = nombre;
        this.conflicto = conflicto.clone();
        this.pesos = pesos.clone();
    }

    @Override
    public boolean permite(int t) {
        if (!enConflicto(t)) {
            return true;
        }
        if (asignada < 0) {
            asignada = sortear();
        }
        return t == asignada;
    }

    @Override
    public int elegir(List<Integer> candidatas) {
        // Las transiciones del conflicto ya fueron filtradas por permite(); entre el resto
        // de candidatas (que no compiten por tokens) cualquiera es válida.
        return candidatas.get(random.nextInt(candidatas.size()));
    }

    @Override
    public void notificarDisparo(int t) {
        if (t == asignada) {
            asignada = -1; // el próximo token del conflicto se sortea de nuevo
        }
    }

    @Override
    public String nombre() {
        return nombre;
    }

    private boolean enConflicto(int t) {
        for (int c : conflicto) {
            if (c == t) {
                return true;
            }
        }
        return false;
    }

    private int sortear() {
        double total = 0;
        for (double p : pesos) {
            total += p;
        }
        double r = random.nextDouble() * total;
        for (int i = 0; i < conflicto.length; i++) {
            r -= pesos[i];
            if (r < 0) {
                return conflicto[i];
            }
        }
        return conflicto[conflicto.length - 1];
    }
}
