package com.rpg.concurrency;

import com.rpg.core.TurnoCallback;

/**
 * Taller 4.2 - Seccion 1: gestor del temporizador de turno.
 * Crea el hilo con el {@link TemporizadorTurnoTask} y lo arranca con start()
 * (nunca se llama a run() directo: eso NO crearia un hilo nuevo).
 *
 * Reglas del taller:
 * - Cada turno crea un Thread nuevo: nunca se llama a start() dos veces sobre
 *   el mismo Thread (lanza IllegalThreadStateException).
 * - Si el jugador responde a tiempo, se cancela con interrupt(): sin banderas
 *   booleanas manuales.
 */
public class GestorTurno {

    private final TemporizadorTurnoTask tarea;
    private final Thread hilo;

    public GestorTurno(int segundos, TurnoCallback callback) {
        this.tarea = new TemporizadorTurnoTask(segundos, callback);
        this.hilo = new Thread(tarea, "TemporizadorTurno");
    }

    /** Arranca la cuenta regresiva en un hilo nuevo. */
    public void iniciar() {
        hilo.start();
    }

    /** El jugador respondio a tiempo: se interrumpe la cuenta regresiva. */
    public void cancelar() {
        hilo.interrupt();
    }

    /** @return true si el tiempo se agoto antes de que el jugador eligiera */
    public boolean expiroTiempo() {
        return tarea.expiro();
    }
}
