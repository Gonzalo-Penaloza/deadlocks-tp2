package tp2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Requerimiento 13: verificación de los invariantes de transición sobre el log, con regex.
 *
 * Idea: en el log los invariantes aparecen ENTRELAZADOS (varios hilos a la vez), por eso la
 * regex permite "basura" (.*?) entre cada transición de un invariante. En cada pasada se
 * elimina un invariante completo y se conserva lo que había entre medio (grupos de relleno).
 * Se repite hasta que no haya más coincidencias. Si al final no queda nada, todos los
 * disparos forman invariantes completos.
 */
public class AnalizadorInvariantes {

    // T0 ... (T1 ... T2 ... T3 | T4 ... T5 | T6 ... T7 ... T8) ... T9
    private static final Pattern INVARIANTE = Pattern.compile(
            "T0(.*?)(?:(T1)(.*?)T2(.*?)T3|(T4)(.*?)T5|(T6)(.*?)T7(.*?)T8)(.*?)T9");

    private int tarjeta, altoRiesgo, transferencia;
    private String resto;

    public void analizar(String secuencia) {
        String actual = secuencia;
        while (true) {
            Matcher m = INVARIANTE.matcher(actual);
            if (!m.find()) {
                break;
            }
            String relleno;
            if (m.group(2) != null) {
                tarjeta++;
                relleno = m.group(1) + m.group(3) + m.group(4);
            } else if (m.group(5) != null) {
                altoRiesgo++;
                relleno = m.group(1) + m.group(6);
            } else {
                transferencia++;
                relleno = m.group(1) + m.group(8) + m.group(9);
            }
            relleno += m.group(10);
            actual = actual.substring(0, m.start()) + relleno + actual.substring(m.end());
        }
        resto = actual;
    }

    public int getTarjeta() { return tarjeta; }
    public int getAltoRiesgo() { return altoRiesgo; }
    public int getTransferencia() { return transferencia; }
    public int getTotal() { return tarjeta + altoRiesgo + transferencia; }
    public String getResto() { return resto; }
}
