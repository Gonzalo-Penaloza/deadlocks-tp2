package tp2;

/** Política 1: cada transacción se rutea a cualquiera de los flujos con igual probabilidad. */
public class PoliticaAleatoria extends PoliticaPonderada {

    public PoliticaAleatoria(int[] conflicto) {
        super("aleatoria", conflicto, pesosIguales(conflicto.length));
    }

    private static double[] pesosIguales(int n) {
        double[] p = new double[n];
        java.util.Arrays.fill(p, 1.0);
        return p;
    }
}
