package com.rpg.entities;

import com.rpg.entities.enums.RaridadItem;
import com.rpg.entities.enums.TipoEfecto;
import com.rpg.entities.enums.TipoPersonaje;
import com.rpg.exceptions.SinManaException;

import java.io.Serial;

public class Mago extends Personaje {

    // Hereda Serializable de Personaje; el UID propio cubre los campos de esta clase.
    @Serial
    private static final long serialVersionUID = 1L;

    private static final int VIDA = 80;
    private static final int ATAQUE = 14;
    private static final int DEFENSA = 3;
    private static final int MANA_MAXIMO = 60;
    private static final int COSTO_MANA_ESPECIAL = 20;
    private static final int COSTO_MANA_FURIA = 10;
    private static final int REGENERACION_MANA = 5;
    private static final double MULTIPLICADOR_ESPECIAL = 1.5;
    private static final double MULTIPLICADOR_FURIA = 1.5;
    private static final String NOMBRE_ESPECIAL = "Bola de Fuego";
    private static final EstadoEfecto QUEMADURA = new EstadoEfecto("Quemadura", TipoEfecto.DANIO_CONTINUO, 4, 3);

    private int mana;

    public Mago(String nombre) {
        this(nombre, MANA_MAXIMO, 0, 0);
    }

    public Mago(String nombre, int mana) {
        this(nombre, mana, 0, 0);
    }

    public Mago(String nombre, int mana, int posicionX, int posicionY) {
        super(nombre, TipoPersonaje.MAGO, VIDA, ATAQUE, DEFENSA, posicionX, posicionY);
        this.mana = mana;
        aprenderHabilidad(NOMBRE_ESPECIAL);
        equiparArma(new Arma("Baston de Roble", 4, RaridadItem.RARO));
    }

    @Override
    public void alIniciarTurno() {
        mana = Math.min(MANA_MAXIMO, mana + REGENERACION_MANA);
    }

    /** Bola de Fuego: ignora la defensa del objetivo y lo deja ardiendo (DoT). */
    @Override
    public String realizarAtaqueEspecial(Personaje objetivo) throws SinManaException {
        if (mana < COSTO_MANA_ESPECIAL) {
            throw new SinManaException(COSTO_MANA_ESPECIAL, mana);
        }
        mana -= COSTO_MANA_ESPECIAL;
        int danio = (int) Math.round(calcularDanio() * MULTIPLICADOR_ESPECIAL);
        objetivo.recibirDanio(danio);
        objetivo.aplicarEfecto(QUEMADURA);
        return String.format("%s lanza %s (-%d de mana) y causa %d de dano a %s, que queda en llamas",
                getNombre(), NOMBRE_ESPECIAL, COSTO_MANA_ESPECIAL, danio, objetivo.getNombre());
    }

    public int getMana() {
        return mana;
    }

    /** Ataque comun con el arma equipada. */
    public String atacar(Personaje objetivo) {
        int vidaAntes = objetivo.getVida();
        objetivo.defender(calcularDanio());
        return String.format("%s ataca a %s y causa %d de dano",
                getNombre(), objetivo.getNombre(), vidaAntes - objetivo.getVida());
    }

    /** Sobrecarga: en modo furia canaliza mana extra para potenciar el golpe. */
    public String atacar(Personaje objetivo, boolean modoFuria) {
        if (!modoFuria) {
            return atacar(objetivo);
        }
        if (mana < COSTO_MANA_FURIA) {
            return String.format("%s no tiene mana para la furia arcana; %s",
                    getNombre(), atacar(objetivo));
        }
        mana -= COSTO_MANA_FURIA;
        int vidaAntes = objetivo.getVida();
        objetivo.defender((int) Math.round(calcularDanio() * MULTIPLICADOR_FURIA));
        return String.format("%s ataca a %s EN MODO FURIA (-%d de mana) y causa %d de dano",
                getNombre(), objetivo.getNombre(), COSTO_MANA_FURIA, vidaAntes - objetivo.getVida());
    }
}
