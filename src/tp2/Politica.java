package tp2;

import java.util.List;

/**
 * Política de resolución de conflictos. El Monitor la consulta en dos momentos:
 *  1) permite(t): guarda extra a la sensibilización. Una transición en conflicto solo
 *     puede dispararse si la política la habilita (así la política decide el ruteo).
 *  2) elegir(candidatas): al salir del monitor, a qué hilo en espera pasarle el testigo.
 * Después de cada disparo el monitor llama a notificarDisparo(t).
 */
public interface Politica {
    boolean permite(int t);

    int elegir(List<Integer> candidatas);

    void notificarDisparo(int t);

    String nombre();
}
