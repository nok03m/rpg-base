package com.rpg.concurrency;

import com.rpg.core.RegistroCombate;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Personaje;

/**
 * Taller 4.2 - Seccion 2: un efecto DoT (dano) o HoT (curacion) corriendo en
 * SU PROPIO hilo, en reemplazo de la logica por turnos de
 * {@code Personaje.procesarEfectos}.
 *
 * Cada cierto intervalo (en milisegundos) aplica un tick de dano o curacion
 * sobre el personaje y consume una unidad de duracion. Cuando la duracion se
 * agota, termina solo y avisa al {@link GestorEfectos} para que lo elimine del
 * personaje. Si otro hilo lo interrumpe (antidoto, purificacion, fin del
 * combate), sale limpio por el catch de {@code InterruptedException}.
 *
 * Reglas del taller que cumple:
 * - while (!Thread.currentThread().isInterrupted()) en lugar de banderas manuales.
 * - Thread.sleep() dentro de try-catch de InterruptedException, con el catch DENTRO del
 *   bucle: limpia el estado, restaura la bandera y sale con break literal.
 * - Nunca Thread.stop() (deprecado y peligroso).
 */
public class EfectoContinuoTask implements Runnable {

    private final Personaje objetivo;
    private final EstadoEfecto efecto;   // es la MISMA instancia guardada en el mapa del personaje
    private final long intervaloMs;
    private final GestorEfectos gestor;
    private final RegistroCombate registro;

    public EfectoContinuoTask(Personaje objetivo, EstadoEfecto efecto, long intervaloMs,
                              GestorEfectos gestor, RegistroCombate registro) {
        this.objetivo = objetivo;
        this.efecto = efecto;
        this.intervaloMs = intervaloMs;
        this.gestor = gestor;
        this.registro = registro;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            // El try-catch va DENTRO del bucle, como pide la guia: el catch limpia el estado,
            // restaura la bandera de interrupcion y sale del bucle con break literal.
            try {
                Thread.sleep(intervaloMs);
            } catch (InterruptedException e) {
                // Cancelacion: antidoto, purificacion o fin del combate.
                if (gestor.esPropietarioActual(this.efecto.getNombre())) {
                    // Limpieza del estado del personaje (solo si este hilo aun posee el efecto;
                    // si fue reaplicado, quien limpia es el hilo nuevo, nunca este).
                    objetivo.quitarEfecto(efecto.getNombre());
                    registro.agregar("%s: %s interrumpido", objetivo.getNombre(), efecto.getNombre());
                }
                Thread.currentThread().interrupt();
                break;
            }
            if (!objetivo.estaVivo()) {
                // El personaje cayo por otro motivo: el efecto ya no tiene sentido.
                registro.agregar("%s cayo: %s se disuelve", objetivo.getNombre(), efecto.getNombre());
                terminar();
                return;
            }
            aplicarTick();
            efecto.consumirTurno();
            if (efecto.haTerminado()) {
                registro.agregar("%s: termina %s", objetivo.getNombre(), efecto.getNombre());
                terminar();
                return;
            }
        }
    }

    private void aplicarTick() {
        if (efecto.esDanio()) {
            objetivo.recibirDanio(efecto.getValorPorTick());
            registro.agregar("%s sufre %d de dano por %s", objetivo.getNombre(),
                    efecto.getValorPorTick(), efecto.getNombre());
        } else {
            int curado = objetivo.curar(efecto.getValorPorTick());
            registro.agregar("%s recupera %d de vida por %s", objetivo.getNombre(),
                    curado, efecto.getNombre());
        }
    }

    /** Fin natural del efecto: se olvida el hilo y se retira del personaje. */
    private void terminar() {
        // Si el efecto fue REAPLICADO mientras este hilo terminaba, el registro del gestor
        // ya apunta al hilo nuevo: este hilo viejo no debe tocar ni el registro ni el estado
        // del personaje (antes podria borrar el efecto nuevo por tener el mismo nombre).
        if (!gestor.esPropietarioActual(efecto.getNombre())) {
            return;
        }
        // 1) olvidar el hilo ANTES de tocar al personaje (fuera del candado del gestor
        //    no hay inversion de candados: personaje -> gestor).
        gestor.olvidarHilo(efecto.getNombre());
        // 2) quitarEfecto avisa al oyente; como el hilo ya no esta en el mapa, no se
        //    interrumpe a si mismo.
        objetivo.quitarEfecto(efecto.getNombre());
    }
}
