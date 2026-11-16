package hilos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Describe QUÉ hilos hay que crear: cuántos y qué transiciones dispara cada uno.
 * Es solo datos; no crea ningún hilo (eso lo hace {@link GestorHilos}).
 *
 * Si cambia la red, se cambia este plan (el monitor no se toca).
 */
public final class PlanHilos {

    /** Un grupo de hilos idénticos: mismo nombre base, misma secuencia, mismo rol. */
    public static final class EspecificacionHilo {
        private final String nombre;
        private final int cantidad;
        private final int[] secuencia;
        private final RolHilo rol;

        public EspecificacionHilo(String nombre, int cantidad, int[] secuencia, RolHilo rol) {
            if (cantidad <= 0 || secuencia.length == 0) {
                throw new IllegalArgumentException("Cantidad y secuencia deben ser > 0: " + nombre);
            }
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.secuencia = secuencia.clone();
            this.rol = rol;
        }

        public String getNombre() {
            return nombre;
        }

        public int getCantidad() {
            return cantidad;
        }

        public int[] getSecuencia() {
            return secuencia.clone();
        }

        public RolHilo getRol() {
            return rol;
        }
    }

    private final List<EspecificacionHilo> especificaciones;

    public PlanHilos(List<EspecificacionHilo> especificaciones) {
        this.especificaciones = Collections.unmodifiableList(new ArrayList<>(especificaciones));
    }

    public List<EspecificacionHilo> getEspecificaciones() {
        return especificaciones;
    }

    public int totalHilos() {
        int total = 0;
        for (EspecificacionHilo e : especificaciones) {
            total += e.getCantidad();
        }
        return total;
    }

    /**
     * Plan para la red de Petri del PSP (Figura 1 de la consigna).
     *
     * Invariantes de transición:
     *   IT1 (tarjeta)       : T0 T1 T2 T3 T9
     *   IT2 (alto riesgo)   : T0 T4 T5 T9
     *   IT3 (transferencia) : T0 T6 T7 T8 T9
     *
     * Caso 1 (conflicto en P1): T0 es previa al conflicto, y luego un hilo por invariante.
     * Caso 2 (join en P9)     : T9 es posterior al join, y hay tantos hilos como tokens
     *                           simultáneos puede haber en P9 (= tokens iniciales de P0).
     *
     * @param tokensP0 tokens iniciales de P0 (leer del RedDePetri.xml; en la Figura 1 son 3)
     */
    public static PlanHilos psp(int tokensP0) {
        List<EspecificacionHilo> l = new ArrayList<>();
        l.add(new EspecificacionHilo("Admision", tokensP0, new int[] {0}, RolHilo.ADMISION));
        l.add(new EspecificacionHilo("Tarjeta", 1, new int[] {1, 2, 3}, RolHilo.INTERMEDIO));
        l.add(new EspecificacionHilo("AltoRiesgo", 1, new int[] {4, 5}, RolHilo.INTERMEDIO));
        l.add(new EspecificacionHilo("Transferencia", 1, new int[] {6, 7, 8}, RolHilo.INTERMEDIO));
        l.add(new EspecificacionHilo("Liquidacion", tokensP0, new int[] {9}, RolHilo.LIQUIDACION));
        return new PlanHilos(l);
    }
}
