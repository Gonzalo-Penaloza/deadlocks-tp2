package tp2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Monitor de concurrencia que gobierna los disparos de la red de Petri.
 *
 * Es agnóstico a la red: no menciona ninguna transición en particular. Todo lo que
 * sabe de la red lo pregunta al objeto RedDePetri que recibe por constructor.
 *
 * Mecanismo: semáforo de exclusión mutua + una cola (semáforo en 0) por transición,
 * con "paso de testigo" (passing the baton): cuando un hilo sale y hay otro esperando
 * cuya transición quedó sensibilizada, en vez de liberar el mutex se lo "pasa"
 * directamente al hilo elegido por la política. Así, ningún hilo recién llegado
 * puede colarse entre medio y la política es la que realmente decide los conflictos.
 */
public class Monitor implements MonitorInterface {

    private final Semaphore mutex = new Semaphore(1, true);
    private final Semaphore[] colas;
    private final int[] esperando;
    private final RedDePetri red;
    private final Politica politica;
    private final Registro registro;
    private volatile boolean terminado = false;

    public Monitor(RedDePetri red, Politica politica, Registro registro) {
        this.red = red;
        this.politica = politica;
        this.registro = registro;
        int n = red.cantidadTransiciones();
        this.colas = new Semaphore[n];
        this.esperando = new int[n];
        for (int t = 0; t < n; t++) {
            colas[t] = new Semaphore(0);
        }
    }

    /**
     * Intenta disparar la transición t. Bloquea al hilo hasta que pueda dispararla.
     *
     * @return true si se disparó; false si la ejecución terminó y el hilo debe salir.
     */
    @Override
    public boolean fireTransition(int t) {
        try {
            mutex.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        // A partir de acá el hilo tiene el "testigo": está dentro de la sección crítica.
        while (true) {
            if (terminado) {
                mutex.release();
                return false;
            }

            if (puedeDispararse(t)) {
                long espera = red.tiempoRestante(t, System.currentTimeMillis());
                if (espera > 0) {
                    // Transición temporal todavía fuera de su ventana: dormir FUERA del monitor
                    // para no bloquear a los demás hilos, y después volver a competir.
                    salir();
                    try {
                        Thread.sleep(espera);
                        mutex.acquire();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                    continue;
                }

                red.disparar(t);
                politica.notificarDisparo(t);
                registro.registrarDisparo(t, red.getMarcado());

                if (red.haTerminado()) {
                    terminado = true;
                    despertarATodos();
                    mutex.release();
                    return true;
                }
                salir();
                return true;
            }

            // No sensibilizada (o la política no la habilita): me encolo en la cola de t y suelto el monitor.
            esperando[t]++;
            salir();
            try {
                colas[t].acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            if (terminado) {
                // Despertado por despertarATodos(): NO tengo el testigo, no toco el mutex.
                return false;
            }
            // Despertado por paso de testigo: ya estoy dentro de la sección crítica.
            esperando[t]--;
        }
    }

    /**
     * Salida del monitor: si hay transiciones sensibilizadas con hilos esperando,
     * la política elige una y se le pasa el testigo; si no, se libera el mutex.
     */
    private void salir() {
        List<Integer> candidatas = new ArrayList<>();
        for (int t : red.getSensibilizadas()) {
            if (esperando[t] > 0 && politica.permite(t)) {
                candidatas.add(t);
            }
        }
        if (candidatas.isEmpty()) {
            mutex.release();
        } else {
            int elegida = politica.elegir(candidatas);
            colas[elegida].release(); // paso de testigo: el mutex sigue "tomado"
        }
    }

    /** Sensibilizada por marcado y habilitada por la política. */
    private boolean puedeDispararse(int t) {
        return red.estaSensibilizada(t) && politica.permite(t);
    }

    private void despertarATodos() {
        for (int t = 0; t < colas.length; t++) {
            colas[t].release(esperando[t]);
        }
    }
}
