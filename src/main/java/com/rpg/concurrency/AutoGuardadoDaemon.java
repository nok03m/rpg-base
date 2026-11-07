package com.rpg.concurrency;

import com.rpg.core.MotorJuego;

/**
 * Taller 4.2 - Seccion 4 + TALLER ARCHIVOS (seccion 7): auto-guardado en segundo plano.
 * Hilo DEMONIO: mientras el juego corre, cada cierto intervalo llama a
 * motor.guardarProgreso(false), que HOY TAMBIEN ESCRIBE EN DISCO (log .txt y partida.dat
 * via GestorArchivos), ademas de la copia en memoria.
 *
 * Seguridad del archivo compartido (seccion 7): el hilo principal puede estar guardando
 * cuando salta el intervalo del demonio. Las escrituras pasan por los metodos
 * synchronized de la unica instancia de GestorArchivos, asi que nunca dos hilos
 * escriben partida.dat a la vez; y como la escritura usa try-with-resources, el close()
 * con flush ocurre siempre: no queda un archivo a medio escribir (y al ser demonio,
 * la JVM lo aborta de golpe al salir sin esperar cierre).
 *
 * Diferencia entre hilo de usuario y hilo demonio (investigacion obligatoria):
 * la JVM se apaga cuando solo quedan hilos de USUARIO vivos; los hilos
 * DEMONIO se abortan de golpe en ese momento. Por eso la aplicacion cierra
 * de inmediato aunque este hilo este en su bucle infinito while (true):
 * no impide la salida, a cambio de que nunca escriba a mitad de una operacion
 * critica (su guardado es solo una copia en memoria, atomico por synchronized).
 *
 * Detalle obligatorio: setDaemon(true) se llama en el constructor, ANTES de
 * start(). Hacerlo despues de start() lanza IllegalThreadStateException.
 */
public class AutoGuardadoDaemon extends Thread {

    private final MotorJuego motor;
    private final long intervaloMs;
    private int copias;

    public AutoGuardadoDaemon(MotorJuego motor, long intervaloMs) {
        super("AutoGuardadoDaemon");
        this.motor = motor;
        this.intervaloMs = intervaloMs;
        // Debe ocurrir ANTES de start(): si se llama despues, IllegalThreadStateException.
        setDaemon(true);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(intervaloMs);
            } catch (InterruptedException e) {
                // Nadie deberia interrumpir a un demonio, pero el catch nunca queda vacio:
                // se restaura la bandera y se termina el bucle de forma ordenada.
                Thread.currentThread().interrupt();
                return;
            }
            copias++;
            motor.guardarProgreso(false);
            System.out.printf("[AutoGuardado] copia #%d del progreso a memoria y disco (hilo daemon)%n", copias);
        }
    }
}
