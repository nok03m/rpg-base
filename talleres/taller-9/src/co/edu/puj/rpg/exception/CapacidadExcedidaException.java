package co.edu.puj.rpg.exception;

/**
 * Excepcion verificada: se intenta agregar un item o habilidad cuando el
 * contenedor correspondiente (inventario o libro) alcanzo su capacidad
 * maxima.
 */
public class CapacidadExcedidaException extends JuegoException {

    public CapacidadExcedidaException(String contenedor, int capacidadMaxima) {
        super(contenedor + " esta lleno: capacidad maxima de "
                + capacidadMaxima + " elementos alcanzada");
    }
}
