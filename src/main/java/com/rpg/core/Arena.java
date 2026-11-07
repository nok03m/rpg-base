package com.rpg.core;

import com.rpg.entities.Personaje;
import com.rpg.exceptions.SinManaException;
import com.rpg.exceptions.VidaInsuficienteException;

import java.util.Random;

/** Reglas de un combate: ataques, habilidades, efectos al inicio del turno y duelos automaticos. */
public class Arena {

    private static final int PROBABILIDAD_CRITICO = 15;
    private static final double FACTOR_CRITICO = 1.5;
    private static final int VARIACION_DANIO = 3;
    private static final int PROBABILIDAD_ESPECIAL_IA = 35;
    private static final int MAXIMO_RONDAS = 40;

    private final RegistroCombate registro;
    private final Random azar;

    public Arena(RegistroCombate registro, Random azar) {
        this.registro = registro;
        this.azar = azar;
    }

    /**
     * Procesa el inicio de turno: regeneraciones de clase y aturdimiento.
     * TALLER CONCURRENCIA: los efectos DoT/HoT ya NO se procesan aqui turno a turno:
     * cada efecto corre en su propio hilo (EfectoContinuoTask) y aplica sus ticks de
     * forma continua, incluso entre turnos. Por eso ya no se llama a procesarEfectos.
     * @return true si el personaje puede actuar este turno
     */
    public boolean iniciarTurno(Personaje personaje) {
        personaje.alIniciarTurno();
        if (!personaje.estaVivo()) {
            registro.agregar("%s cayo por los danos de sus efectos", personaje.getNombre());
            return false;
        }
        if (personaje.consumirAturdimiento()) {
            registro.agregar("%s esta aturdido y pierde el turno", personaje.getNombre());
            return false;
        }
        return true;
    }

    public void atacar(Personaje atacante, Personaje defensor) {
        boolean critico = azar.nextInt(100) < PROBABILIDAD_CRITICO;
        double factor = critico ? FACTOR_CRITICO : 1.0;
        int danioBruto = (int) Math.round(atacante.calcularDanio() * factor) + azar.nextInt(VARIACION_DANIO + 1);
        int vidaAntes = defensor.getVida();
        defensor.defender(danioBruto);
        registro.agregar("%s ataca a %s%s y causa %d de dano", atacante.getNombre(), defensor.getNombre(),
                critico ? " (CRITICO)" : "", vidaAntes - defensor.getVida());
        registro.agregar("   %s", FormatoCombate.resumen(defensor));
    }

    /** @return true si el especial se ejecuto; false si fallo por falta de mana o de vida */
    public boolean intentarEspecial(Personaje atacante, Personaje defensor) {
        try {
            registro.agregar(atacante.realizarAtaqueEspecial(defensor));
            registro.agregar("   %s", FormatoCombate.resumen(defensor));
            return true;
        } catch (SinManaException | VidaInsuficienteException e) {
            registro.agregar("%s no puede usar su habilidad: %s", atacante.getNombre(), e.getMessage());
            return false;
        }
    }

    /** Turno completo controlado por el programa (enemigos y gladiadores). */
    public void turnoAutomatico(Personaje atacante, Personaje defensor) {
        if (!atacante.estaVivo() || !defensor.estaVivo() || !iniciarTurno(atacante)) {
            return;
        }
        boolean quiereEspecial = azar.nextInt(100) < PROBABILIDAD_ESPECIAL_IA;
        if (quiereEspecial && intentarEspecial(atacante, defensor)) {
            return;
        }
        atacar(atacante, defensor);
    }

    /** Pelea hasta que uno cae (o se acaban las rondas). @return el ganador */
    public Personaje duelo(Personaje a, Personaje b) {
        return duelo(a, b, 0);
    }

    /**
     * Duelo con una pausa opcional entre rondas. TALLER CONCURRENCIA: el torneo usa
     * una pausa pequena (pense del gladiador) para que la ejecucion paralela de las
     * semifinales sea visible en los Thread.getState() y en el reloj; sin pausa, un
     * duelo entero termina en menos de un milisegundo y no daria tiempo a observar
     * los estados RUNNABLE / TIMED_WAITING del ciclo de vida del hilo.
     */
    public Personaje duelo(Personaje a, Personaje b, long pausaEntreRondasMs) {
        for (int ronda = 1; ronda <= MAXIMO_RONDAS && a.estaVivo() && b.estaVivo(); ronda++) {
            registro.agregar("-- Ronda %d --", ronda);
            turnoAutomatico(a, b);
            turnoAutomatico(b, a);
            if (pausaEntreRondasMs > 0) {
                try {
                    Thread.sleep(pausaEntreRondasMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return a.getVida() >= b.getVida() ? a : b;
    }
}
