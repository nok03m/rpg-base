package com.rpg.concurrency;

import com.rpg.core.TurnoCallback;

/**
 * Taller 4.2 - Seccion 1: cuenta regresiva del turno del jugador.
 * Implementa {@link Runnable} y corre en su propio hilo (lo crea {@link GestorTurno}).
 *
 * Ciclo de vida del hilo: NEW -> (start) RUNNABLE -> TIMED_WAITING (cada sleep de 1 s)
 * -> TERMINATED cuando llega a cero (invoca el callback) o cuando otro hilo lo
 * interrumpe con {@code interrupt()} porque el jugador respondio a tiempo.
 *
 * Nunca se usan banderas booleanas manuales: la cancelacion es solo con interrupt().
 */
public class TemporizadorTurnoTask implements Runnable {

    private static final long MILIS_POR_SEGUNDO = 1000;

    private final int segundos;
    private final TurnoCallback callback;
    /** volatile: el hilo principal lo lee mientras el temporizador lo escribe. */
    private volatile boolean expiro;

    public TemporizadorTurnoTask(int segundos, TurnoCallback callback) {
        if (segundos <= 0) {
            throw new IllegalArgumentException("Los segundos deben ser positivos");
        }
        this.segundos = segundos;
        this.callback = callback;
    }

    @Override
    public void run() {
        for (int restante = segundos; restante > 0; restante--) {
            try {
                Thread.sleep(MILIS_POR_SEGUNDO);
            } catch (InterruptedException e) {
                // El jugador respondio a tiempo y GestorTurno.cancelar() nos interrumpio:
                // se restaura la bandera de interrupcion (buena practica) y se sale sin
                // invocar el callback. Nunca se deja un catch vacio.
                Thread.currentThread().interrupt();
                return;
            }
        }
        expiro = true;
        callback.onTiempoAgotado();
    }

    /** @return true si el tiempo llego a cero antes de que el jugador eligiera */
    public boolean expiro() {
        return expiro;
    }
}
