public class Main {
    public static void main(String[] args) {
        Personaje heroe = new Personaje("Aragorn", 100, 15, 50.0);

        // Intento ilicito de modificacion directa de un atributo privado
        heroe.puntosVida = 200;
        System.out.println("Vida: " + heroe.puntosVida);
    }
}
