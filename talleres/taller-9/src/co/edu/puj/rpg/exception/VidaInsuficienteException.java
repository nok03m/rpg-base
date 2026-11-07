package co.edu.puj.rpg.exception;

/**
 * Excepcion NO verificada (unchecked): se intenta realizar una accion de
 * costo que sobrepasa los puntos de vida actuales del personaje. Al ser
 * un error de programacion de flujo, no obliga a capturarla en firma.
 */
public class VidaInsuficienteException extends RuntimeException {

    public VidaInsuficienteException(String nombrePersonaje, int costoVida, int vidaActual) {
        super(nombrePersonaje + " no puede pagar " + costoVida
                + " puntos de vida (vida actual: " + vidaActual + ")");
    }
}
