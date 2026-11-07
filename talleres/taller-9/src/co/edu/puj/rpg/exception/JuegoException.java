package co.edu.puj.rpg.exception;

/**
 * Clase base abstracta de las excepciones verificadas del dominio del
 * juego. Toda excepcion de negocio hereda de aqui. Los constructores
 * aceptan una causa original para no perder el rastro de la pila al
 * reenvolver excepciones.
 */
public abstract class JuegoException extends Exception {

    protected JuegoException(String mensaje) {
        super(mensaje);
    }

    protected JuegoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
