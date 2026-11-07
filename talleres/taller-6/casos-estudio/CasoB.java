public class Personaje {
    private int puntosVida;
    private boolean estaVivo;

    public Personaje(int puntosVida) {
        // Error de encapsulamiento: asignacion directa sin pasar por setter o validacion
        this.puntosVida = -50;
        this.estaVivo = true; // Inconsistencia: el personaje tiene vida negativa pero esta vivo
    }
}
