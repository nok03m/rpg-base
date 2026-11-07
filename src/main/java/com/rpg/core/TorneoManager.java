package com.rpg.core;

import com.rpg.entities.Escuadron;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.Personaje;

import java.util.List;

/**
 * Torneo de 4 gladiadores: dos semifinales y una final.
 * TALLER CONCURRENCIA (seccion 3): las dos semifinales corren AL MISMO TIEMPO,
 * cada una en su propio hilo con su Runnable. El hilo principal arranca ambas
 * con start(), espera con join() a que terminen y SOLO DESPUES lee la memoria
 * compartida (el arreglo de finalistas) para lanzar la final.
 *
 * Ciclo de vida observable: cada semifinal pasa por
 * NEW (creada) -> RUNNABLE (start) -> TIMED_WAITING (pausa entre rondas)
 * -> TERMINATED (join retorno). Si el duelo fuera instantaneo, veriamos
 * TERMINATED casi de inmediato: por eso Arena.duelo acepta una pausa por ronda.
 */
public class TorneoManager {

    /** Pausa de cada ronda de semifinal (ms): hace visible el ciclo de vida del hilo. */
    private static final long PAUSA_RONDA_MS = 40;
    /** Espera antes de muestrear los estados "durante la ejecucion" (ms): cae dentro de la
     * primera pausa de ronda (40 ms), cuando los hilos deberian verse en TIMED_WAITING. */
    private static final long PAUSA_MUESTREO_MS = 15;

    private final Arena arena;
    private final RegistroCombate registro;

    public TorneoManager(Arena arena, RegistroCombate registro) {
        this.arena = arena;
        this.registro = registro;
    }

    /** @return el campeon del torneo */
    public Personaje jugarTorneo() {
        List<Personaje> gladiadores = crearGladiadores().getMiembros();

        // Memoria compartida entre el hilo principal y los dos hilos de semifinal.
        // Cada posicion la escribe UN solo hilo y el principal la lee SOLO despues de
        // los join(): por eso no hace falta sincronizarla (el join establece la
        // relacion "pasa antes" que garantiza la visibilidad de lo escrito).
        Personaje[] finalistas = new Personaje[2];

        Thread semifinalA = new Thread(() ->
                finalistas[0] = simularSemifinal(gladiadores.get(0), gladiadores.get(1)), "Semifinal-A");
        Thread semifinalB = new Thread(() ->
                finalistas[1] = simularSemifinal(gladiadores.get(2), gladiadores.get(3)), "Semifinal-B");

        // ---- estado NEW: los hilos existen pero aun no arrancaron
        System.out.printf("[Estados] %s=%s  %s=%s%n",
                semifinalA.getName(), semifinalA.getState(),
                semifinalB.getName(), semifinalB.getState());

        semifinalA.start();
        semifinalB.start();

        try {
            Thread.sleep(PAUSA_MUESTREO_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // ---- estados durante la ejecucion (RUNNABLE o TIMED_WAITING, segun el momento)
        System.out.printf("[Estados] %s=%s  %s=%s  (en ejecucion)%n",
                semifinalA.getName(), semifinalA.getState(),
                semifinalB.getName(), semifinalB.getState());

        // El hilo principal se bloquea hasta que CADA semifinal termine.
        try {
            semifinalA.join();
            semifinalB.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("El torneo fue interrumpido", e);
        }

        // ---- estados despues del join: TERMINATED
        System.out.printf("[Estados] %s=%s  %s=%s  (terminadas)%n",
                semifinalA.getName(), semifinalA.getState(),
                semifinalB.getName(), semifinalB.getState());

        // Solo aqui, con ambos join() retornados, es seguro leer los finalistas.
        return simularFinal(finalistas[0], finalistas[1]);
    }

    public Personaje simularSemifinal(Personaje a, Personaje b) {
        registro.agregar("=== SEMIFINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        Personaje ganador = arena.duelo(a, b, PAUSA_RONDA_MS);
        registro.agregar("Clasifica a la final: %s", ganador.getNombre());
        return ganador;
    }

    private Personaje simularFinal(Personaje a, Personaje b) {
        a.restaurarVida();
        b.restaurarVida();
        registro.agregar("=== FINAL: %s vs %s ===", a.getNombre(), b.getNombre());
        return arena.duelo(a, b, PAUSA_RONDA_MS);
    }

    private Escuadron crearGladiadores() {
        Escuadron escuadron = new Escuadron("Gladiadores");
        escuadron.agregar(new Guerrero("Maximo"));
        escuadron.agregar(new Mago("Circe"));
        escuadron.agregar(new Guerrero("Aquiles"));
        escuadron.agregar(new Mago("Merlina"));
        return escuadron;
    }
}
