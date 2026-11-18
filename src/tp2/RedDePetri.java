package tp2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Representa la red de Petri: estructura (matriz de incidencia), estado (marcado),
 * semántica temporal (tiempo alfa de cada transición) e invariantes de plaza.
 *
 * NO es thread-safe por sí misma: todos los accesos se hacen desde dentro del Monitor,
 * que garantiza exclusión mutua. Por eso no lleva synchronized.
 */
public class RedDePetri {

    private final int[][] incidencia;          // C = Post - Pre  (plazas x transiciones)
    private final int[][] pre;                 // Pre: tokens que consume cada transición
    private final int[] marcado;               // M actual
    private final long[] alfa;                 // tiempo mínimo de sensibilización (ms); 0 = inmediata
    private final long[] sensibilizadaDesde;   // instante en que cada transición quedó sensibilizada (-1 = no lo está)
    private final int[] limiteDisparos;        // cantidad máxima de disparos (-1 = sin límite)
    private final int[] contadorDisparos;
    private final int[][] invariantesPlaza;    // cada fila es un vector y tal que y·M = constante
    private final int[] constantesInvariantes; // y·M0
    private final int transicionFin;           // transición cuyo conteo indica la finalización
    private final int disparosParaFin;

    public RedDePetri(int[][] incidencia, int[] marcadoInicial, long[] alfa, int[] limiteDisparos,
                      int[][] invariantesPlaza, int transicionFin, int disparosParaFin) {
        this.incidencia = copiar(incidencia);
        this.marcado = marcadoInicial.clone();
        this.alfa = alfa.clone();
        this.limiteDisparos = limiteDisparos.clone();
        this.invariantesPlaza = copiar(invariantesPlaza);
        this.transicionFin = transicionFin;
        this.disparosParaFin = disparosParaFin;

        int plazas = incidencia.length;
        int transiciones = incidencia[0].length;
        this.contadorDisparos = new int[transiciones];
        this.sensibilizadaDesde = new long[transiciones];

        // Pre se deduce de C porque la red no tiene autolazos (ninguna transición
        // consume y produce en la misma plaza).
        this.pre = new int[plazas][transiciones];
        for (int p = 0; p < plazas; p++) {
            for (int t = 0; t < transiciones; t++) {
                pre[p][t] = Math.max(0, -incidencia[p][t]);
            }
        }

        this.constantesInvariantes = new int[invariantesPlaza.length];
        for (int i = 0; i < invariantesPlaza.length; i++) {
            constantesInvariantes[i] = productoEscalar(invariantesPlaza[i], marcado);
        }
        actualizarTiempos(System.currentTimeMillis());
    }

    public int cantidadTransiciones() {
        return contadorDisparos.length;
    }

    /** Sensibilizada por marcado: cada plaza de entrada tiene los tokens que pide el arco. */
    public boolean estaSensibilizada(int t) {
        if (limiteDisparos[t] >= 0 && contadorDisparos[t] >= limiteDisparos[t]) {
            return false;
        }
        for (int p = 0; p < marcado.length; p++) {
            if (marcado[p] < pre[p][t]) {
                return false;
            }
        }
        return true;
    }

    /** Milisegundos que faltan para que se cumpla alfa (<= 0 significa que ya puede dispararse). */
    public long tiempoRestante(int t, long ahora) {
        if (alfa[t] == 0) {
            return 0;
        }
        return alfa[t] - (ahora - sensibilizadaDesde[t]);
    }

    /** Ecuación fundamental para un disparo: M' = M + C·e_t. Precondición: estaSensibilizada(t). */
    public void disparar(int t) {
        for (int p = 0; p < marcado.length; p++) {
            marcado[p] += incidencia[p][t];
        }
        contadorDisparos[t]++;
        // La transición disparada "reinicia su reloj" aunque siga sensibilizada.
        sensibilizadaDesde[t] = -1;
        actualizarTiempos(System.currentTimeMillis());
        verificarInvariantesPlaza();
    }

    private void actualizarTiempos(long ahora) {
        for (int t = 0; t < contadorDisparos.length; t++) {
            if (estaSensibilizada(t)) {
                if (sensibilizadaDesde[t] < 0) {
                    sensibilizadaDesde[t] = ahora;
                }
            } else {
                sensibilizadaDesde[t] = -1;
            }
        }
    }

    /** Requerimiento 12: se verifica después de cada disparo. */
    private void verificarInvariantesPlaza() {
        for (int i = 0; i < invariantesPlaza.length; i++) {
            int valor = productoEscalar(invariantesPlaza[i], marcado);
            if (valor != constantesInvariantes[i]) {
                throw new IllegalStateException("Se violó el invariante de plaza " + i
                        + ": esperado " + constantesInvariantes[i] + ", obtenido " + valor
                        + " con M=" + Arrays.toString(marcado));
            }
        }
    }

    public boolean haTerminado() {
        return contadorDisparos[transicionFin] >= disparosParaFin;
    }

    public int[] getMarcado() {
        return marcado.clone();
    }

    public int[] getContadorDisparos() {
        return contadorDisparos.clone();
    }

    public List<Integer> getSensibilizadas() {
        List<Integer> lista = new ArrayList<>();
        for (int t = 0; t < contadorDisparos.length; t++) {
            if (estaSensibilizada(t)) {
                lista.add(t);
            }
        }
        return lista;
    }

    private static int productoEscalar(int[] a, int[] b) {
        int s = 0;
        for (int i = 0; i < a.length; i++) {
            s += a[i] * b[i];
        }
        return s;
    }

    private static int[][] copiar(int[][] m) {
        int[][] c = new int[m.length][];
        for (int i = 0; i < m.length; i++) {
            c[i] = m[i].clone();
        }
        return c;
    }
}
