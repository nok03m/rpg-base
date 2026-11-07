/**
 * Investigacion obligatoria del taller de concurrencia: HILOS DAEMON.
 *
 * Demuestra tres cosas:
 *  1. La JVM termina aunque un hilo demonio siga vivo (no espera a que termine).
 *  2. Un hilo de USUARIO si mantiene la JVM viva hasta que termine.
 *  3. setDaemon(true) despues de start() lanza IllegalThreadStateException.
 *
 * Ejecutar: java InvestigacionDaemon
 */
public class InvestigacionDaemon {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("== 1. Hilo DEMONIO: la JVM no lo espera ==");

        Thread demonio = new Thread(() -> {
            for (int i = 1; i <= 10; i++) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                System.out.println("  [daemon] tick " + i + " (se corta cuando la JVM sale)");
            }
        }, "DemonioDePrueba");
        // REGLA: setDaemon(true) ANTES de start(); aca se cumple.
        demonio.setDaemon(true);
        demonio.start();

        System.out.println("  (main) el daemon es demonio? " + demonio.isDaemon());

        Thread.sleep(1000); // el daemon alcanza a dar ~3 ticks

        System.out.println("  (main) el main termina AHORA: la JVM se apaga sin esperar al daemon.");
        System.out.println("== Observa: no veras los 10 ticks del daemon, solo ~3. ==");

        // Al salir de main, si no quedaran hilos de usuario, la JVM aborta al daemon.
        // Para que la demostracion 2 sea posible lanzamos UN hilo de usuario que
        // mantiene la JVM viva y ahi comprobamos la parte 3 antes de cerrar.
        Thread usuario = new Thread(() -> {
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("== 2. Hilo de USUARIO: la JVM espero hasta que termino. ==");
        }, "UsuarioDePrueba");
        usuario.start();

        // == 3. setDaemon(true) DESPUES de start() -> IllegalThreadStateException ==
        try {
            usuario.setDaemon(true); // el hilo ya esta arrancado: ilegal
            System.out.println("  [error] no deberia llegar aqui");
        } catch (IllegalThreadStateException e) {
            System.out.println("== 3. setDaemon(true) tras start() lanza IllegalThreadStateException: "
                    + e.getMessage() + " ==");
        }

        // Esperamos al hilo de usuario para que la JVM no se cierre todavia
        // (el demonio sigue dando ticks en segundo plano mientras tanto).
        usuario.join();
        System.out.println("== Fin: la JVM cierra y abandona al demonio de inmediato. ==");
    }
}
