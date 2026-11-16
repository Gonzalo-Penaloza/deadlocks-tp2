package hilos;

import monitor.MonitorInterface;

/**
 * Hilo "trabajador": repite en bucle una secuencia fija de transiciones, pidiéndole al monitor
 * que dispare cada una. Toda la sincronización vive en el monitor; este hilo solo llama a
 * fireTransition y obedece lo que le responde.
 *
 * Ejemplo: el hilo del flujo de tarjeta tiene la secuencia [T1, T2, T3] y hace
 * T1 -> T2 -> T3 -> T1 -> T2 -> T3 ... hasta que lo cortan.
 */
public final class HiloTransiciones implements Runnable {

    private final int[] secuencia;
    private final RolHilo rol;
    private final MonitorInterface monitor;
    private final ContadorInvariantes contador;

    public HiloTransiciones(int[] secuencia, RolHilo rol, MonitorInterface monitor, ContadorInvariantes contador) {
        this.secuencia = secuencia.clone();
        this.rol = rol;
        this.monitor = monitor;
        this.contador = contador;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() && puedeIniciarCiclo()) {
            for (int transicion : secuencia) {
                // false = me interrumpieron mientras esperaba: el sistema terminó, salgo.
                if (!monitor.fireTransition(transicion)) {
                    return;
                }
            }
            if (rol == RolHilo.LIQUIDACION) {
                contador.registrarCompletado();
            }
        }
    }

    /** Solo los hilos de admisión están limitados: no pueden entrar más de N transacciones. */
    private boolean puedeIniciarCiclo() {
        return rol != RolHilo.ADMISION || contador.reservarAdmision();
    }
}
