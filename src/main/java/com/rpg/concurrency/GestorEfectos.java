package com.rpg.concurrency;

import com.rpg.core.RegistroCombate;
import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.OyenteEfectos;
import com.rpg.entities.Personaje;

import java.util.HashMap;
import java.util.Map;

/**
 * Taller 4.2 - Seccion 2: registro central de los hilos de efectos activos.
 * Implementa {@link OyenteEfectos}: el dominio avisa "aplique/quite un efecto"
 * y este gestor crea o interrumpe los hilos correspondientes.
 *
 * El mapa {@code hilos} lo tocan varios hilos a la vez (el principal aplica y
 * quita efectos; los propios hilos de efectos avisan su fin natural), por eso
 * todos sus accesos van sincronizados sobre el gestor.
 *
 * Orden de candados para evitar deadlock:
 * - El hilo principal va de Personaje (monitor) hacia GestorEfectos (candado).
 * - Los hilos de efectos llaman a {@link #olvidarHilo} (candado, y lo sueltan)
 *   ANTES de tocar al Personaje (monitor). Nunca se retienen los dos candados.
 */
public class GestorEfectos implements OyenteEfectos {

    private final long intervaloMs;
    private final RegistroCombate registro;
    /** nombre del efecto -> hilo que lo procesa. Siempre bajo synchronized(this). */
    private final Map<String, Thread> hilos = new HashMap<>();

    public GestorEfectos(long intervaloMs, RegistroCombate registro) {
        this.intervaloMs = intervaloMs;
        this.registro = registro;
    }

    // ---- OyenteEfectos (llamado desde Personaje, normalmente en el hilo principal)

    @Override
    public void efectoAplicado(Personaje objetivo, EstadoEfecto efecto) {
        Thread nuevo;
        Thread anterior;
        synchronized (this) {
            nuevo = new Thread(new EfectoContinuoTask(objetivo, efecto, intervaloMs, this, registro),
                    "Efecto-" + efecto.getNombre() + "-" + objetivo.getNombre());
            anterior = hilos.put(efecto.getNombre(), nuevo);
        }
        if (anterior != null) {
            // Reaplicacion del mismo efecto: el hilo viejo se interrumpe y queda el nuevo.
            anterior.interrupt();
        }
        nuevo.start();
    }

    @Override
    public void efectoQuitado(Personaje objetivo, String nombreEfecto) {
        Thread hilo = tomarYOlvidar(nombreEfecto);
        if (hilo != null) {
            hilo.interrupt();
        }
    }

    // ---- llamado desde los propios hilos de efectos (fin natural)

    /** Quita el hilo del registro SIN interrumpirlo (ya esta terminando solo). */
    public void olvidarHilo(String nombreEfecto) {
        tomarYOlvidar(nombreEfecto);
    }

    // ---- fin de combate

    /** Interrumpe todos los hilos de efectos activos (fin del combate). */
    public void detenerTodo() {
        synchronized (this) {
            for (Thread hilo : hilos.values()) {
                hilo.interrupt();
            }
            hilos.clear();
        }
    }

    /** @return true si queda algun hilo de efecto corriendo (util en pruebas) */
    public boolean hayHilosActivos() {
        synchronized (this) {
            return !hilos.isEmpty();
        }
    }

    private synchronized Thread tomarYOlvidar(String nombreEfecto) {
        return hilos.remove(nombreEfecto);
    }
}
