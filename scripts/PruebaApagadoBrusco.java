import com.rpg.core.Consola;
import com.rpg.core.MotorJuego;
import com.rpg.persistencia.EstadoPartida;

import java.io.FileInputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * TALLER ARCHIVOS (seccion 7): prueba del criterio que las evaluaciones dejaron pendiente:
 * "comprobar que el daemon no deja un archivo a medio escribir al cerrar la JVM".
 *
 * Como funciona (dos modos en la misma clase):
 * - MODO PADRE (sin argumentos): lanza el juego como SUBPROCESO con autoguardado cada
 *   25 ms, espera a que el demonio haya hecho varias copias y mata el proceso con
 *   destroyForcibly() (el equivalente a apagar la JVM a lo bruto / SIGKILL, sin darle
 *   tiempo a limpiar nada). Repite el apagado brusco en 5 momentos distintos.
 * - MODO DEMONIO (--demonio <intervaloMs>): es el subproceso; corre el MotorJuego real
 *   y queda guardando en bucle hasta que el padre lo mata (o 15 s de tope de seguridad).
 *
 * Lo que se comprueba tras CADA apagado brusco:
 *   1. El partida.dat EXISTE y CARGA COMPLETO con el mismo filtro del juego: nunca
 *      queda un archivo a medio escribir, ni siquiera con SIGKILL a mitad de ejecucion.
 *      (El guardado atomico .tmp -> Files.move ATOMIC_MOVE garantiza que el .dat o es
 *      la version anterior completa o la nueva completa, jamas una mezcla.)
 *   2. Un .tmp residual es ADMISIBLE y esperado (era una escritura en curso que el
 *      SIGKILL corto): el diseno lo contempla y el proximo guardado lo reescribe.
 * Ejecutar: java PruebaApagadoBrusco
 */
public class PruebaApagadoBrusco {

    private static int fallos = 0;

    public static void main(String[] args) throws Exception {
        if (args.length >= 2 && "--demonio".equals(args[0])) {
            modoDemonio(Long.parseLong(args[1]));
            return;
        }
        modoPadre();
    }

    // ---- modo subproceso: el juego real guardando en bucle -------------------------

    private static void modoDemonio(long intervaloMs) throws Exception {
        // MotorJuego arranca el AutoGuardadoDaemon real; guardarProgreso(false) vuelca
        // log .txt y partida.dat a disco cada intervaloMs, igual que en el juego.
        new MotorJuego(new Consola(new Scanner("")), intervaloMs);
        Thread.sleep(15_000);   // tope de seguridad: el padre normal lo mata mucho antes
    }

    // ---- modo padre: matar la JVM durante el guardado y auditar el .dat ------------

    private static void modoPadre() throws Exception {
        // CLASSPATH ABSOLUTO obligatorio: el subproceso corre con carpeta de trabajo
        // temporal, asi que un classpath relativo ("out") no resolveria ahi y el
        // subproceso moriria con ClassNotFoundException antes de guardar nada.
        String classpath = java.nio.file.Paths.get(System.getProperty("java.class.path"))
                .toAbsolutePath().normalize().toString();
        Path carpeta = Files.createTempDirectory("apagado-brusco");
        System.out.println("Carpeta de trabajo del subproceso: " + carpeta);

        for (int intento = 1; intento <= 5; intento++) {
            long vidaDelProceso = 300L + intento * 120L;   // 420..900 ms: ya hubo varios guardados

            ProcessBuilder lanzador = new ProcessBuilder(
                    "java", "-cp", classpath, "PruebaApagadoBrusco", "--demonio", "25");
            lanzador.directory(carpeta.toFile());
            lanzador.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            lanzador.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process subproceso = lanzador.start();

            Thread.sleep(vidaDelProceso);
            // Apagado BRUSCO: sin oportunidad de limpiar, exactamente el escenario del criterio.
            subproceso.destroyForcibly();
            subproceso.waitFor();

            Path dat = carpeta.resolve("partidas/partida.dat");
            Path tmp = carpeta.resolve("partidas/partida.dat.tmp");
            boolean existe = Files.exists(dat);
            EstadoPartida cargada = existe ? cargarConFiltro(dat) : null;
            boolean tmpResidual = Files.exists(tmp);

            check("Apagado brusco #" + intento + " (vida " + vidaDelProceso + " ms): el .dat existe",
                    existe);
            check("Apagado brusco #" + intento + ": el .dat carga COMPLETO, nunca a medio escribir",
                    cargada != null);
            if (tmpResidual) {
                // No es un fallo: es una escritura que el SIGKILL corto. El .dat quedo intacto.
                System.out.println("  (info) quedo un .tmp residual: era una escritura en curso, "
                        + "el .dat completo siguio siendo el anterior y carga bien.");
            }
        }

        System.out.println();
        if (fallos == 0) {
            System.out.println("RESULTADO: EL DAEMON NUNCA DEJA UN .dat A MEDIAS, NI SIQUIERA "
                    + "CON APAGADO BRUSCO (5 apagados forzados)");
        } else {
            System.out.println("RESULTADO: " + fallos + " check(s) con FALLO");
            System.exit(1);
        }
    }

    /**
     * Carga el .dat con el MISMO filtro de deserializacion del juego, usando el patron
     * anidado que cierra el flujo aunque el constructor falle (la leccion del hardening).
     */
    private static EstadoPartida cargarConFiltro(Path ruta) {
        ObjectInputFilter filtro = ObjectInputFilter.Config.createFilter(
                "com.rpg.**;java.base/*;maxdepth=32;maxrefs=4096;maxbytes=4194304;maxarray=16384;!*");
        try (FileInputStream flujo = new FileInputStream(ruta.toFile())) {
            try (ObjectInputStream entrada = new ObjectInputStream(flujo)) {
                entrada.setObjectInputFilter(filtro);
                return (EstadoPartida) entrada.readObject();
            }
        } catch (Exception e) {
            return null;   // un .dat a medias caeria aqui: seria el FALLO que buscamos
        }
    }

    private static void check(String nombre, boolean condicion) {
        System.out.println((condicion ? "[OK]   " : "[FALLO]") + " " + nombre);
        if (!condicion) {
            fallos++;
        }
    }
}
