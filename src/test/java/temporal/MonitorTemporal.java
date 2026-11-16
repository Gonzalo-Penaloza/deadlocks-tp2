package temporal;

import java.util.Arrays;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import monitor.MonitorInterface;

/**
 * *** TEMPORAL — SOLO PARA PROBAR LA ETAPA 3 ***
 *
 * Reemplaza provisoriamente a las etapas 1 (núcleo de la red), 2 (monitor) y 4 (política y tiempo).
 * No tiene política (desempata cualquiera) ni tiempos. Cuando estén las etapas reales,
 * se cambia este objeto por el Monitor real y los tests de hilos siguen igual.
 *
 * Sí respeta el contrato de MonitorInterface: si interrumpen al hilo mientras espera, devuelve false.
 * Y verifica los invariantes de plaza después de cada disparo.
 */
public class MonitorTemporal implements MonitorInterface {

    private static final int PLAZAS = 10;
    private static final int TRANSICIONES = 10;

    /** Invariantes de plaza de la red del PSP: suma de tokens de estas plazas = constante. */
    private static final int[][] INVARIANTES_PLAZA = {
        {7, 2, 3, 4},                 // gateway de tarjetas (P7) + quien lo usa
        {8, 4, 5, 6},                 // motor antifraude (P8) + quien lo usa
        {0, 1, 2, 3, 4, 5, 6, 9}      // transacciones en el sistema
    };

    private final int[][] pre = new int[PLAZAS][TRANSICIONES];   // pre[p][t]: tokens que t saca de p
    private final int[][] post = new int[PLAZAS][TRANSICIONES];  // post[p][t]: tokens que t pone en p
    private final int[] marcado;
    private final int[] marcadoInicial;
    private final int[] sumaEsperada = new int[INVARIANTES_PLAZA.length];
    private final int[] disparos = new int[TRANSICIONES];

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition cambioDeMarcado = lock.newCondition();

    public MonitorTemporal(int tokensP0) {
        marcadoInicial = new int[] {tokensP0, 0, 0, 0, 0, 0, 0, 1, 1, 0};
        marcado = marcadoInicial.clone();

        //            T   plazas de entrada      plazas de salida
        arcos(0, new int[] {0},       new int[] {1});
        arcos(1, new int[] {1, 7},    new int[] {2});
        arcos(2, new int[] {2},       new int[] {3});
        arcos(3, new int[] {3},       new int[] {7, 9});
        arcos(4, new int[] {1, 7, 8}, new int[] {4});
        arcos(5, new int[] {4},       new int[] {7, 8, 9});
        arcos(6, new int[] {1, 8},    new int[] {5});
        arcos(7, new int[] {5},       new int[] {6});
        arcos(8, new int[] {6},       new int[] {8, 9});
        arcos(9, new int[] {9},       new int[] {0});

        for (int i = 0; i < INVARIANTES_PLAZA.length; i++) {
            sumaEsperada[i] = suma(INVARIANTES_PLAZA[i]);
        }
    }

    @Override
    public boolean fireTransition(int t) {
        lock.lock();
        try {
            if (Thread.currentThread().isInterrupted()) {
                return false;
            }
            while (!habilitada(t)) {
                try {
                    cambioDeMarcado.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();   // contrato: restaurar el flag
                    return false;
                }
            }
            disparar(t);
            verificarInvariantesDePlaza();
            cambioDeMarcado.signalAll();
            return true;
        } finally {
            lock.unlock();
        }
    }

    // ----- solo para tests -----

    public int[] getDisparos() {
        lock.lock();
        try {
            return disparos.clone();
        } finally {
            lock.unlock();
        }
    }

    public int[] getMarcado() {
        lock.lock();
        try {
            return marcado.clone();
        } finally {
            lock.unlock();
        }
    }

    public int[] getMarcadoInicial() {
        return marcadoInicial.clone();
    }

    // ----- internos (siempre con el lock tomado) -----

    private boolean habilitada(int t) {
        for (int p = 0; p < PLAZAS; p++) {
            if (marcado[p] < pre[p][t]) {
                return false;
            }
        }
        return true;
    }

    private void disparar(int t) {
        for (int p = 0; p < PLAZAS; p++) {
            marcado[p] += post[p][t] - pre[p][t];   // M' = M + I * e_t
        }
        disparos[t]++;
    }

    private void verificarInvariantesDePlaza() {
        for (int i = 0; i < INVARIANTES_PLAZA.length; i++) {
            if (suma(INVARIANTES_PLAZA[i]) != sumaEsperada[i]) {
                throw new IllegalStateException("Invariante de plaza " + (i + 1) + " violado. Marcado: "
                        + Arrays.toString(marcado));
            }
        }
    }

    private int suma(int[] plazas) {
        int s = 0;
        for (int p : plazas) {
            s += marcado[p];
        }
        return s;
    }

    private void arcos(int t, int[] entrada, int[] salida) {
        for (int p : entrada) {
            pre[p][t]++;
        }
        for (int p : salida) {
            post[p][t]++;
        }
    }
}
