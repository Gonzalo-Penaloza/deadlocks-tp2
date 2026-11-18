package modelos;

import java.util.Arrays;

public class PetriNet {
    private final int[][] incidenceMatrix;
    private final int[] currentMarking;
    private final int numPlaces;
    private final int numTransitions;

    public PetriNet(int[][] incidenceMatrix, int[] initialMarking) {
        this.incidenceMatrix = incidenceMatrix;
        this.currentMarking = Arrays.copyOf(initialMarking, initialMarking.length);
        this.numPlaces = initialMarking.length;
        this.numTransitions = incidenceMatrix[0].length;
    }

    /**
     * Verifica si una transición está habilitada según el marcado actual.
     * Una transición está habilitada si para toda plaza p de entrada, 
     * el marcado actual es mayor o igual al peso del arco (p, t).
     */
    public boolean isEnabled(int transitionIndex) {
        for (int i = 0; i < numPlaces; i++) {
            // En una red P/T estándar, si la incidencia es negativa, es un arco de entrada.
            // Para simplificar, asumimos arcos de peso 1 si no se especifica.
            // Si incidenceMatrix[i][transitionIndex] < 0, se requieren tokens.
            if (incidenceMatrix[i][transitionIndex] < 0) {
                if (currentMarking[i] < Math.abs(incidenceMatrix[i][transitionIndex])) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Dispara una transición actualizando el marcado.
     * M_next = M_actual + Incidencia * vector_disparo
     */
    public boolean fire(int transitionIndex) {
        if (!isEnabled(transitionIndex)) {
            return false;
        }

        for (int i = 0; i < numPlaces; i++) {
            currentMarking[i] += incidenceMatrix[i][transitionIndex];
        }
        
        // Verificación de invariantes de plaza (Debe ser implementado por el Integrante 1)
        checkPlaceInvariants();
        
        return true;
    }

    public int[] getCurrentMarking() {
        return Arrays.copyOf(currentMarking, currentMarking.length);
    }

    private void checkPlaceInvariants() {
        // Invariante de Plaza 1: Recursos de Gateway (P7)
        // P7 + P2 + P3 + P4 = 1 (Tokens iniciales en P7)
        int gatewayResources = currentMarking[7] + currentMarking[2] + currentMarking[3] + currentMarking[4];
        if (gatewayResources != 1) {
            throw new RuntimeException("¡ERROR! Invariante de Plaza P7 violado. Recursos: " + gatewayResources);
        }

        // Invariante de Plaza 2: Recursos de Motor Antifraude (P8)
        // P8 + P4 + P5 + P6 = 1 (Tokens iniciales en P8)
        int antifraudResources = currentMarking[8] + currentMarking[4] + currentMarking[5] + currentMarking[6];
        if (antifraudResources != 1) {
            throw new RuntimeException("¡ERROR! Invariante de Plaza P8 violado. Recursos: " + antifraudResources);
        }

        // Invariante de Plaza 3: Conservación de transacciones totales
        // P0 + P1 + P2 + P3 + P4 + P5 + P6 + P9 = 3 (Tokens iniciales en P0)
        int totalTransactions = currentMarking[0] + currentMarking[1] + currentMarking[2] + 
                                currentMarking[3] + currentMarking[4] + currentMarking[5] + 
                                currentMarking[6] + currentMarking[9];
        if (totalTransactions != 3) {
            throw new RuntimeException("¡ERROR! Invariante de Transacciones violado. Total: " + totalTransactions);
        }
    }
}
