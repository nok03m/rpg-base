package co.edu.puj.rpg.modelo;

/**
 * Nucleo de atributos vitales del Personaje. Es una parte constitutiva
 * (Composicion 1..1): se instancia dentro del constructor de Personaje y
 * no existe un setter publico que permita reemplazar la instancia por una
 * externa. La manipulacion de vida y mana solo ocurre mediante sus metodos
 * internos, validando los rangos admitidos.
 */
public class CorazonRPG {

    private int saludBase;
    private int manaBase;

    public CorazonRPG(int saludBase, int manaBase) {
        if (saludBase <= 0 || manaBase < 0) {
            throw new IllegalArgumentException("Atributos vitales invalidos para el corazon");
        }
        this.saludBase = saludBase;
        this.manaBase = manaBase;
    }

    public int getSaludBase() {
        return saludBase;
    }

    public int getManaBase() {
        return manaBase;
    }

    /** Manipulacion interna: aumenta la salud y el mana base del corazon. */
    public void fortalecer(int saludExtra, int manaExtra) {
        if (saludExtra < 0 || manaExtra < 0) {
            throw new IllegalArgumentException("Los aumentos no pueden ser negativos");
        }
        this.saludBase += saludExtra;
        this.manaBase += manaExtra;
    }

    /** Manipulacion interna: reduce la salud y el mana base sin bajar de cero. */
    public void drenar(int saludPerdida, int manaPerdida) {
        if (saludPerdida < 0 || manaPerdida < 0) {
            throw new IllegalArgumentException("Las reducciones no pueden ser negativas");
        }
        this.saludBase = Math.max(0, this.saludBase - saludPerdida);
        this.manaBase = Math.max(0, this.manaBase - manaPerdida);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[salud base: ").append(saludBase)
          .append(", mana base: ").append(manaBase).append(']');
        return sb.toString();
    }
}
