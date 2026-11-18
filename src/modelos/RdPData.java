package modelos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RdPData {
    // Mapas para convertir IDs del XML (P0, T0) a índices (0, 1, 2...)
    public final Map<String, Integer> placeToIndex = new HashMap<>();
    public final Map<String, Integer> transitionToIndex = new HashMap<>();
    public final Map<Integer, String> indexToTransition = new HashMap<>();

    public final int[][] incidenceMatrix;
    public final int[] initialMarking;

    public RdPData() {
        // Inicialización manual basada en el XML proporcionado
        // Lugares: P0 a P9 (10 plazas)
        for (int i = 0; i <= 9; i++) {
            placeToIndex.put("P" + i, i);
        }

        // Transiciones: T0 a T9 (10 transiciones)
        for (int i = 0; i <= 9; i++) {
            transitionToIndex.put("T" + i, i);
            indexToTransition.put(i, "T" + i);
        }

        initialMarking = new int[10];
        initialMarking[0] = 3; // P0 tiene 3 tokens iniciales
        initialMarking[7] = 1; // P7 tiene 1 token inicial
        initialMarking[8] = 1; // P8 tiene 1 token inicial

        incidenceMatrix = new int[10][10];
        
        // Llenado de la matriz (Filas: Plazas, Columnas: Transiciones)
        // Ejemplo T0: de P0 a P1
        setIncidence("P0", "T0", -1);
        setIncidence("P1", "T0", 1);

        // Flujo Tarjeta: T1 (P1, P7 -> P2), T2 (P2 -> P3), T3 (P3 -> P7, P9)
        setIncidence("P1", "T1", -1);
        setIncidence("P7", "T1", -1);
        setIncidence("P2", "T1", 1);
        
        setIncidence("P2", "T2", -1);
        setIncidence("P3", "T2", 1);
        
        setIncidence("P3", "T3", -1);
        setIncidence("P7", "T3", 1);
        setIncidence("P9", "T3", 1);

        // Flujo Alto Riesgo: T4 (P1, P7, P8 -> P4), T5 (P4 -> P7, P8, P9)
        setIncidence("P1", "T4", -1);
        setIncidence("P7", "T4", -1);
        setIncidence("P8", "T4", -1);
        setIncidence("P4", "T4", 1);
        
        setIncidence("P4", "T5", -1);
        setIncidence("P7", "T5", 1);
        setIncidence("P8", "T5", 1);
        setIncidence("P9", "T5", 1);

        // Flujo Transferencia: T6 (P1, P8 -> P5), T7 (P5 -> P6), T8 (P6 -> P8, P9)
        setIncidence("P1", "T6", -1);
        setIncidence("P8", "T6", -1);
        setIncidence("P5", "T6", 1);
        
        setIncidence("P5", "T7", -1);
        setIncidence("P6", "T7", 1);
        
        setIncidence("P6", "T8", -1);
        setIncidence("P8", "T8", 1);
        setIncidence("P9", "T8", 1);

        // Finalización: T9 (P9 -> P0)
        setIncidence("P9", "T9", -1);
        setIncidence("P0", "T9", 1);
    }

    private void setIncidence(String p, String t, int val) {
        incidenceMatrix[placeToIndex.get(p)][transitionToIndex.get(t)] = val;
    }
}
