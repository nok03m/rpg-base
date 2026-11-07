public class Main {
    public static void ejecutarAccion(Personaje heroe) {
        // Mutacion que viola las reglas de negocio si el setter no esta validado
        heroe.setPuntosVida(999999); // Excede vidaMaxima
        heroe.setPuntosVida(-100); // Vida negativa sin actualizar estaVivo a false
    }
}
