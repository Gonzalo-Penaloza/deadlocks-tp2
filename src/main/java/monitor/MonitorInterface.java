package monitor;

/**
 * Interfaz exigida por la consigna. Es lo ÚNICO que la capa de hilos conoce del monitor.
 */
public interface MonitorInterface {

    /**
     * Intenta disparar la transición indicada. El hilo que llama queda bloqueado
     * hasta que la transición pueda dispararse (o hasta que lo interrumpan).
     *
     * CONTRATO con la capa de hilos (etapa 3):
     *  - devuelve true  -> la transición se disparó.
     *  - devuelve false -> el hilo fue interrumpido mientras esperaba (fin de la ejecución);
     *                      la transición NO se disparó. El monitor debe restaurar el flag de
     *                      interrupción (Thread.currentThread().interrupt()) y no tragarse
     *                      la InterruptedException.
     *
     * @param transition índice de la transición (0 = T0, 1 = T1, ...)
     */
    boolean fireTransition(int transition);
}
