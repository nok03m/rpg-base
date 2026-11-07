package co.edu.puj.rpg.exception;

/**
 * Excepcion verificada: un personaje intenta ejecutar una habilidad
 * magica sin el nivel de mana requerido.
 */
public class SinManaException extends JuegoException {

    public SinManaException(int manaRequerido, int manaDisponible) {
        super("Mana insuficiente: la habilidad cuesta " + manaRequerido
                + " y el personaje solo tiene " + manaDisponible);
    }
}
