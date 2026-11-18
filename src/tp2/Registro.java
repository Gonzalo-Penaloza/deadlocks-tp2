package tp2;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Archivos de log:
 *  - transiciones_<politica>.txt : la secuencia completa de disparos en una sola línea (T0T1T2...),
 *    que después se analiza con expresiones regulares.
 *  - ejecucion_<politica>.log    : un renglón por disparo con tiempo, hilo y marcado resultante.
 *
 * Solo lo usa el Monitor desde dentro de la sección crítica, así que el orden
 * de escritura coincide con el orden real de los disparos.
 */
public class Registro {

    private final long inicio = System.currentTimeMillis();
    private final StringBuilder secuencia = new StringBuilder();
    private final BufferedWriter detalle;
    private final Path archivoSecuencia;

    public Registro(Path carpeta, String sufijo) throws IOException {
        Files.createDirectories(carpeta);
        this.archivoSecuencia = carpeta.resolve("transiciones_" + sufijo + ".txt");
        this.detalle = Files.newBufferedWriter(carpeta.resolve("ejecucion_" + sufijo + ".log"),
                StandardCharsets.UTF_8);
    }

    public void registrarDisparo(int t, int[] marcado) {
        secuencia.append('T').append(t);
        try {
            detalle.write(String.format("%7d ms | %-14s | T%d | M=%s%n",
                    System.currentTimeMillis() - inicio, Thread.currentThread().getName(), t,
                    Arrays.toString(marcado)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void registrarLinea(String linea) {
        try {
            detalle.write(linea);
            detalle.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getSecuencia() {
        return secuencia.toString();
    }

    public Path getArchivoSecuencia() {
        return archivoSecuencia;
    }

    public void cerrar() throws IOException {
        Files.writeString(archivoSecuencia, secuencia.toString(), StandardCharsets.UTF_8);
        detalle.close();
    }
}
