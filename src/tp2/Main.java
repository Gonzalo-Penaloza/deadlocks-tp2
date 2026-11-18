package tp2;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Punto de entrada. Uso:
 *   java tp2.Main                          -> corre la política aleatoria y después la prioritaria
 *   java tp2.Main aleatoria                -> solo aleatoria
 *   java tp2.Main prioritaria              -> solo prioritaria
 *   java tp2.Main aleatoria 100 100 150 100 100   -> además fija alfa de T2 T3 T5 T7 T8 (ms)
 *
 * Toda la información específica de ESTA red (matriz, marcado, tiempos, invariantes,
 * hilos) vive acá y en ninguna otra clase.
 */
public class Main {

    static final int INVARIANTES_OBJETIVO = 200;

    /** Conflicto estructural en P1: T1 (tarjeta), T4 (alto riesgo), T6 (transferencia). */
    static final int[] CONFLICTO = {1, 4, 6};
    static final double PESO_ALTO_RIESGO = 0.5;

    //                       T0  T1  T2  T3  T4  T5  T6  T7  T8  T9
    static final int[][] INCIDENCIA = {
            /* P0 */ {-1,  0,  0,  0,  0,  0,  0,  0,  0,  1},
            /* P1 */ { 1, -1,  0,  0, -1,  0, -1,  0,  0,  0},
            /* P2 */ { 0,  1, -1,  0,  0,  0,  0,  0,  0,  0},
            /* P3 */ { 0,  0,  1, -1,  0,  0,  0,  0,  0,  0},
            /* P4 */ { 0,  0,  0,  0,  1, -1,  0,  0,  0,  0},
            /* P5 */ { 0,  0,  0,  0,  0,  0,  1, -1,  0,  0},
            /* P6 */ { 0,  0,  0,  0,  0,  0,  0,  1, -1,  0},
            /* P7 */ { 0, -1,  0,  1, -1,  1,  0,  0,  0,  0},
            /* P8 */ { 0,  0,  0,  0, -1,  1, -1,  0,  1,  0},
            /* P9 */ { 0,  0,  0,  1,  0,  1,  0,  0,  1, -1},
    };

    //                                  P0 P1 P2 P3 P4 P5 P6 P7 P8 P9
    static final int[] MARCADO_INICIAL = {3, 0, 0, 0, 0, 0, 0, 1, 1, 0};

    static final int[][] INVARIANTES_PLAZA = {
            {1, 1, 1, 1, 1, 1, 1, 0, 0, 1}, // transacciones en el sistema = 3
            {0, 0, 1, 1, 1, 0, 0, 1, 0, 0}, // gateway de tarjetas: libre o en uso = 1
            {0, 0, 0, 0, 1, 1, 1, 0, 1, 0}, // motor antifraude: libre o en uso = 1
    };

    // Hilos (ver informe): 3 de entrada/salida + 1 por flujo.
    static final int[][] SEGMENTOS = {
            {0, 9}, {0, 9}, {0, 9},  // admisión (T0) y salida a liquidación (T9)
            {1, 2, 3},               // flujo tarjeta
            {4, 5},                  // flujo alto riesgo
            {6, 7, 8},               // flujo transferencia
    };
    static final String[] NOMBRES = {
            "EntradaSalida-1", "EntradaSalida-2", "EntradaSalida-3",
            "Tarjeta", "AltoRiesgo", "Transferencia"};

    public static void main(String[] args) throws Exception {
        String modo = args.length > 0 ? args[0].toLowerCase() : "ambas";
        long[] tiempos = {100, 100, 150, 100, 100}; // T2 T3 T5 T7 T8
        if (args.length == 6) {
            for (int i = 0; i < 5; i++) {
                tiempos[i] = Long.parseLong(args[i + 1]);
            }
        }

        List<Politica> politicas = new ArrayList<>();
        if (!modo.equals("prioritaria")) {
            politicas.add(new PoliticaAleatoria(CONFLICTO));
        }
        if (!modo.equals("aleatoria")) {
            politicas.add(new PoliticaPrioritaria(CONFLICTO, 4, PESO_ALTO_RIESGO));
        }
        for (Politica p : politicas) {
            ejecutar(p, tiempos);
        }
    }

    static void ejecutar(Politica politica, long[] tiempos) throws Exception {
        long[] alfa = new long[10];
        alfa[2] = tiempos[0];
        alfa[3] = tiempos[1];
        alfa[5] = tiempos[2];
        alfa[7] = tiempos[3];
        alfa[8] = tiempos[4];

        int[] limites = new int[10];
        Arrays.fill(limites, -1);
        limites[0] = INVARIANTES_OBJETIVO; // no se admiten más de 200 transacciones

        RedDePetri red = new RedDePetri(INCIDENCIA, MARCADO_INICIAL, alfa, limites,
                INVARIANTES_PLAZA, 9, INVARIANTES_OBJETIVO);
        Path carpetaLog = Paths.get("log");
        Registro registro = new Registro(carpetaLog, politica.nombre());
        Monitor monitor = new Monitor(red, politica, registro);

        System.out.println("=== Política " + politica.nombre() + " | alfa T2,T3,T5,T7,T8 = "
                + Arrays.toString(tiempos) + " ms ===");

        List<Thread> hilos = new ArrayList<>();
        for (int i = 0; i < SEGMENTOS.length; i++) {
            hilos.add(new Thread(new Disparador(monitor, SEGMENTOS[i]), NOMBRES[i]));
        }
        long inicio = System.currentTimeMillis();
        for (Thread h : hilos) {
            h.start();
        }
        for (Thread h : hilos) {
            h.join(); // requerimiento 3: el programa no termina con hilos vivos
        }
        long duracion = System.currentTimeMillis() - inicio;

        int[] disparos = red.getContadorDisparos();
        AnalizadorInvariantes analizador = new AnalizadorInvariantes();
        analizador.analizar(registro.getSecuencia());

        String resumen = String.format(
                "Duración: %.2f s%n"
                        + "Disparos por transición: %s%n"
                        + "Marcado final: %s%n"
                        + "Invariantes (contadores)  tarjeta=%d  altoRiesgo=%d  transferencia=%d  total=%d%n"
                        + "Invariantes (regex)       tarjeta=%d  altoRiesgo=%d  transferencia=%d  total=%d%n"
                        + "Resto tras regex: \"%s\" -> %s%n",
                duracion / 1000.0, Arrays.toString(disparos), Arrays.toString(red.getMarcado()),
                disparos[1], disparos[4], disparos[6], disparos[9],
                analizador.getTarjeta(), analizador.getAltoRiesgo(), analizador.getTransferencia(),
                analizador.getTotal(), analizador.getResto(),
                analizador.getResto().isEmpty() ? "INVARIANTES DE TRANSICIÓN OK" : "ERROR");
        registro.registrarLinea("");
        registro.registrarLinea(resumen);
        registro.cerrar();
        System.out.println(resumen);
    }
}
