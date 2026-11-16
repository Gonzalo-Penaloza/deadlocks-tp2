package temporal;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import hilos.ContadorInvariantes;
import hilos.GestorHilos;
import hilos.GestorHilos.Resultado;
import hilos.PlanHilos;
import monitor.MonitorInterface;

/**
 * *** TEMPORAL — pruebas de la etapa 3 sin JUnit (se corre con main) ***
 *
 * Prueba que la capa de hilos:
 *   1) crea la cantidad de hilos esperada y completa exactamente 200 invariantes,
 *   2) no deja hilos vivos al terminar,
 *   3) termina aunque el monitor se cuelgue (timeout) o un hilo falle (excepción).
 */
public class PruebaHilos {

    private static final int TOKENS_P0 = 3;
    private static final int INVARIANTES = 200;
    private static final int CORRIDAS = 30;

    private static int fallos = 0;

    public static void main(String[] args) throws Exception {
        pruebaNormal();
        pruebaMonitorColgado();
        pruebaFallaEnHilo();

        System.out.println();
        System.out.println(fallos == 0 ? "TODO OK" : "FALLARON " + fallos + " verificaciones");
        System.exit(fallos == 0 ? 0 : 1);
    }

    private static void pruebaNormal() throws Exception {
        System.out.println("== Prueba 1: " + CORRIDAS + " corridas de " + INVARIANTES + " invariantes ==");
        long totalTarjeta = 0;
        long totalRiesgo = 0;
        long totalTransf = 0;

        for (int corrida = 1; corrida <= CORRIDAS; corrida++) {
            MonitorTemporal monitor = new MonitorTemporal(TOKENS_P0);
            PlanHilos plan = PlanHilos.psp(TOKENS_P0);
            ContadorInvariantes contador = new ContadorInvariantes(INVARIANTES);
            GestorHilos gestor = new GestorHilos(monitor, plan, contador);

            long ini = System.nanoTime();
            Resultado r = gestor.ejecutar(30_000);
            long ms = (System.nanoTime() - ini) / 1_000_000;

            int[] d = monitor.getDisparos();
            String id = "corrida " + corrida;

            check(r == Resultado.OK, id + ": resultado = " + r + " " + contador.getError());
            check(gestor.getHilos().size() == 9, id + ": se esperaban 9 hilos y hay " + gestor.getHilos().size());
            check(gestor.getHilos().stream().noneMatch(Thread::isAlive), id + ": quedaron hilos vivos");
            check(d[0] == INVARIANTES && d[9] == INVARIANTES, id + ": T0/T9 = " + d[0] + "/" + d[9]);
            check(d[1] + d[4] + d[6] == INVARIANTES, id + ": los 3 flujos no suman " + INVARIANTES);
            check(d[2] == d[1] && d[3] == d[1], id + ": flujo tarjeta incompleto");
            check(d[5] == d[4], id + ": flujo alto riesgo incompleto");
            check(d[7] == d[6] && d[8] == d[6], id + ": flujo transferencia incompleto");
            check(Arrays.equals(monitor.getMarcado(), monitor.getMarcadoInicial()),
                    id + ": marcado final distinto del inicial " + Arrays.toString(monitor.getMarcado()));
            check(contador.getAdmitidos() == INVARIANTES && contador.getCompletados() == INVARIANTES,
                    id + ": contador admitidos/completados = " + contador.getAdmitidos() + "/" + contador.getCompletados());

            totalTarjeta += d[1];
            totalRiesgo += d[4];
            totalTransf += d[6];
            if (corrida <= 3) {
                System.out.printf("  corrida %d: tarjeta=%d altoRiesgo=%d transferencia=%d (%d ms)%n",
                        corrida, d[1], d[4], d[6], ms);
            }
        }
        System.out.printf("  promedio sobre %d corridas: tarjeta=%.1f altoRiesgo=%.1f transferencia=%.1f%n",
                CORRIDAS, totalTarjeta / (double) CORRIDAS, totalRiesgo / (double) CORRIDAS,
                totalTransf / (double) CORRIDAS);
    }

    /** Monitor que nunca dispara nada (como un deadlock): el gestor debe cortar por timeout. */
    private static void pruebaMonitorColgado() throws Exception {
        System.out.println("== Prueba 2: monitor colgado -> TIMEOUT y sin hilos vivos ==");
        MonitorInterface colgado = t -> esperarHastaInterrupcion();
        GestorHilos gestor = new GestorHilos(colgado, PlanHilos.psp(TOKENS_P0), new ContadorInvariantes(INVARIANTES));

        Resultado r = gestor.ejecutar(500);

        check(r == Resultado.TIMEOUT, "monitor colgado: resultado = " + r);
        check(gestor.getHilos().stream().noneMatch(Thread::isAlive), "monitor colgado: quedaron hilos vivos");
    }

    /** Un hilo falla con excepción: el gestor no debe esperar el timeout completo. */
    private static void pruebaFallaEnHilo() throws Exception {
        System.out.println("== Prueba 3: excepción en un hilo -> ERROR rápido y sin hilos vivos ==");
        MonitorInterface conFalla = t -> {
            if (t == 1) {
                throw new RuntimeException("falla simulada en T1");
            }
            return esperarHastaInterrupcion();
        };
        ContadorInvariantes contador = new ContadorInvariantes(INVARIANTES);
        GestorHilos gestor = new GestorHilos(conFalla, PlanHilos.psp(TOKENS_P0), contador);

        long ini = System.nanoTime();
        Resultado r = gestor.ejecutar(30_000);
        long ms = (System.nanoTime() - ini) / 1_000_000;

        check(r == Resultado.ERROR, "falla en hilo: resultado = " + r);
        check(ms < 5_000, "falla en hilo: tardó " + ms + " ms (debía cortar enseguida)");
        check(gestor.getHilos().stream().noneMatch(Thread::isAlive), "falla en hilo: quedaron hilos vivos");
        System.out.println("  causa registrada: " + contador.getError());
    }

    /** Se comporta como el monitor real: bloquea y devuelve false si lo interrumpen. */
    private static boolean esperarHastaInterrupcion() {
        try {
            new CountDownLatch(1).await();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void check(boolean ok, String mensaje) {
        if (!ok) {
            fallos++;
            System.out.println("  FALLO: " + mensaje);
        }
    }
}
