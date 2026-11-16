package hilos;

/**
 * Papel que cumple un hilo dentro del ciclo de vida de una transacción.
 * Sirve para saber cuándo una transacción entra al sistema y cuándo termina,
 * sin que el monitor tenga que enterarse (el monitor es agnóstico a la red).
 */
public enum RolHilo {

    /** Dispara la transición que admite transacciones (T0). Pide permiso antes de cada ciclo. */
    ADMISION,

    /** Dispara transiciones intermedias de un flujo. No necesita coordinación extra. */
    INTERMEDIO,

    /** Dispara la transición que liquida (T9). Al terminar cada ciclo, cuenta un invariante completado. */
    LIQUIDACION
}
