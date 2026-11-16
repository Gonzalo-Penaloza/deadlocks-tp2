package hilos;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Controla cuándo termina la ejecución: exactamente {@code objetivo} invariantes completos.
 *
 * Estrategia (para que NO queden invariantes a medias en el log):
 *  1. Compuerta de admisión: solo se permite que entren {@code objetivo} transacciones (T0).
 *  2. Las transacciones ya admitidas terminan todo su recorrido hasta T9.
 *  3. Al registrarse el último T9, se libera el latch y el gestor corta a los hilos restantes.
 *
 * Es thread-safe: lo usan a la vez todos los hilos de admisión y de liquidación.
 */
public final class ContadorInvariantes {

    private final int objetivo;
    private final AtomicInteger admitidos = new AtomicInteger();
    private final AtomicInteger completados = new AtomicInteger();
    private final CountDownLatch fin = new CountDownLatch(1);
    private final AtomicReference<String> error = new AtomicReference<>();

    public ContadorInvariantes(int objetivo) {
        if (objetivo <= 0) {
            throw new IllegalArgumentException("El objetivo debe ser > 0");
        }
        this.objetivo = objetivo;
    }

    /**
     * Un hilo de admisión pide permiso para iniciar una transacción nueva.
     * Es atómico: aunque haya varios hilos de admisión compitiendo, nunca se entregan
     * más de {@code objetivo} permisos.
     *
     * @return true si puede intentar disparar T0; false si ya se admitieron todas.
     */
    public boolean reservarAdmision() {
        // getAndUpdate devuelve el valor anterior: si era < objetivo, este hilo consiguió un permiso.
        return admitidos.getAndUpdate(a -> a < objetivo ? a + 1 : a) < objetivo;
    }

    /** Un hilo de liquidación avisa que acaba de disparar T9 (un invariante completo). */
    public void registrarCompletado() {
        if (completados.incrementAndGet() == objetivo) {
            fin.countDown();
        }
    }

    /** Corta la ejecución por una falla (por ejemplo, una excepción en un hilo). Gana la primera causa. */
    public void abortar(String motivo) {
        error.compareAndSet(null, motivo);
        fin.countDown();
    }

    /**
     * Espera a que se complete el objetivo o a que alguien aborte.
     *
     * @return true si el latch se liberó, false si venció el timeout.
     */
    public boolean esperarFin(long timeoutMs) throws InterruptedException {
        return fin.await(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public String getError() {
        return error.get();
    }

    public int getObjetivo() {
        return objetivo;
    }

    public int getAdmitidos() {
        return admitidos.get();
    }

    public int getCompletados() {
        return completados.get();
    }
}
