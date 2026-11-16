package hilos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import monitor.MonitorInterface;

/**
 * Orquesta el ciclo de vida de los hilos: los crea según el {@link PlanHilos}, los arranca,
 * espera a que se completen los invariantes pedidos, los corta y verifica que no quede
 * ninguno vivo (requisito 3 de la consigna).
 *
 * Un GestorHilos se usa para UNA ejecución.
 */
public final class GestorHilos {

    public enum Resultado {
        /** Se completaron todos los invariantes y no quedó ningún hilo activo. */
        OK,
        /** Venció el timeout antes de completar los invariantes (posible deadlock). */
        TIMEOUT,
        /** Algún hilo terminó con una excepción. Ver {@link ContadorInvariantes#getError()}. */
        ERROR
    }

    /** Tiempo máximo que se espera a que cada hilo muera después de interrumpirlo. */
    private static final long ESPERA_JOIN_MS = 5_000;

    private final MonitorInterface monitor;
    private final PlanHilos plan;
    private final ContadorInvariantes contador;
    private final List<Thread> hilos = new ArrayList<>();

    public GestorHilos(MonitorInterface monitor, PlanHilos plan, ContadorInvariantes contador) {
        this.monitor = monitor;
        this.plan = plan;
        this.contador = contador;
    }

    /**
     * Corre el sistema hasta completar los invariantes (o hasta el timeout) y espera la muerte
     * de todos los hilos antes de volver.
     *
     * @param timeoutMs máximo a esperar por los invariantes (red de seguridad contra deadlocks)
     * @throws IllegalStateException si después de interrumpir queda algún hilo vivo
     */
    public Resultado ejecutar(long timeoutMs) throws InterruptedException {
        crearHilos();
        for (Thread h : hilos) {
            h.start();
        }

        boolean liberado = false;
        try {
            liberado = contador.esperarFin(timeoutMs);
        } finally {
            detenerHilos();
        }

        if (contador.getError() != null) {
            return Resultado.ERROR;
        }
        return liberado ? Resultado.OK : Resultado.TIMEOUT;
    }

    /** Hilos creados (solo lectura). Útil para informar cantidad y para los tests. */
    public List<Thread> getHilos() {
        return Collections.unmodifiableList(hilos);
    }

    private void crearHilos() {
        hilos.clear();
        for (PlanHilos.EspecificacionHilo esp : plan.getEspecificaciones()) {
            for (int i = 1; i <= esp.getCantidad(); i++) {
                String nombre = esp.getCantidad() > 1 ? esp.getNombre() + "-" + i : esp.getNombre();
                HiloTransiciones tarea =
                        new HiloTransiciones(esp.getSecuencia(), esp.getRol(), monitor, contador);
                Thread hilo = new Thread(tarea, nombre);
                // Si un hilo muere por una excepción, el sistema podría quedar colgado esperando
                // invariantes que nunca llegan: se aborta para no esperar al timeout.
                hilo.setUncaughtExceptionHandler((t, e) -> contador.abortar(t.getName() + ": " + e));
                hilos.add(hilo);
            }
        }
    }

    /**
     * Interrumpe a todos (los que estén bloqueados esperando en el monitor salen con false)
     * y espera a que terminen.
     */
    private void detenerHilos() throws InterruptedException {
        for (Thread h : hilos) {
            h.interrupt();
        }
        List<String> vivos = new ArrayList<>();
        for (Thread h : hilos) {
            h.join(ESPERA_JOIN_MS);
            if (h.isAlive()) {
                vivos.add(h.getName());
            }
        }
        if (!vivos.isEmpty()) {
            throw new IllegalStateException("Quedaron hilos activos: " + vivos);
        }
    }
}
