package tp2;

/**
 * Política 2: prioriza una transición del conflicto dándole más peso en el sorteo.
 * Con pesoPrioritario = 0.5 y tres ramas, la priorizada recibe el 50 % de las
 * transacciones y las otras dos se reparten el resto en partes iguales (25 % / 25 %).
 */
public class PoliticaPrioritaria extends PoliticaPonderada {

    public PoliticaPrioritaria(int[] conflicto, int priorizada, double pesoPrioritario) {
        super("prioritaria", conflicto, pesos(conflicto, priorizada, pesoPrioritario));
    }

    private static double[] pesos(int[] conflicto, int priorizada, double pesoPrioritario) {
        double[] p = new double[conflicto.length];
        double resto = (1.0 - pesoPrioritario) / (conflicto.length - 1);
        for (int i = 0; i < conflicto.length; i++) {
            p[i] = conflicto[i] == priorizada ? pesoPrioritario : resto;
        }
        return p;
    }
}
