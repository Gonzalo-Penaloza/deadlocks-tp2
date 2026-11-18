package tp2;

/**
 * Hilo genérico: dispara cíclicamente una secuencia fija de transiciones (su "segmento"
 * de la red). La responsabilidad de cada hilo se define solo por esa secuencia.
 */
public class Disparador implements Runnable {

    private final MonitorInterface monitor;
    private final int[] secuencia;

    public Disparador(MonitorInterface monitor, int[] secuencia) {
        this.monitor = monitor;
        this.secuencia = secuencia.clone();
    }

    @Override
    public void run() {
        while (true) {
            for (int t : secuencia) {
                if (!monitor.fireTransition(t)) {
                    return; // la ejecución terminó
                }
            }
        }
    }
}
