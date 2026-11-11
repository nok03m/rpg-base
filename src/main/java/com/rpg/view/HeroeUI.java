package com.rpg.view;

import com.rpg.core.Consola;
import com.rpg.controller.HeroeController;
import com.rpg.model.Heroe;

import java.util.List;

/**
 * View de la gestion de heroes: solo lee por consola e imprime.
 * No hay una sola linea de JDBC ni reglas de negocio: todo se delega
 * en el Controller y los mensajes salen de el.
 */
public class HeroeUI {

    private final Consola consola;
    private final HeroeController controller;

    public HeroeUI(Consola consola, HeroeController controller) {
        this.consola = consola;
        this.controller = controller;
    }

    /** Menu con las 5 operaciones del taller (mas volver). */
    public void iniciar() {
        int opcion;
        do {
            System.out.println("\n=== GESTION DE HEROES (BD) ===");
            System.out.println("1. Insertar heroe");
            System.out.println("2. Listar heroes");
            System.out.println("3. Buscar por tipo");
            System.out.println("4. Actualizar nivel");
            System.out.println("5. Eliminar heroe");
            System.out.println("0. Volver al menu principal");
            opcion = consola.leerEntero("Opcion: ", 0, 5);
            switch (opcion) {
                case 1 -> insertar();
                case 2 -> listarTodos();
                case 3 -> buscarPorTipo();
                case 4 -> actualizarNivel();
                case 5 -> eliminar();
                default -> System.out.println("Volviendo al menu principal.");
            }
        } while (opcion != 0);
    }

    private void insertar() {
        String nombre = consola.leerNombre("Nombre del heroe: ");
        String tipo = pedirTipo();
        int vida = consola.leerEntero("Vida maxima (50 a 200): ", 50, 200);
        int nivel = consola.leerEntero("Nivel inicial (1 a 99): ", 1, 99);
        System.out.println(controller.insertar(nombre, tipo, vida, nivel));
    }

    private void listarTodos() {
        List<Heroe> heroes = controller.listar();
        imprimirListado(heroes);
    }

    private void buscarPorTipo() {
        String tipo = pedirTipo();
        List<Heroe> heroes = controller.buscarPorTipo(tipo);
        if (heroes != null && heroes.isEmpty()) {
            System.out.println("No hay heroes de ese tipo.");
            return;
        }
        imprimirListado(heroes);
    }

    private void actualizarNivel() {
        int id = consola.leerEntero("Id del heroe: ", 1, Integer.MAX_VALUE);
        int nivel = consola.leerEntero("Nuevo nivel (1 a 99): ", 1, 99);
        System.out.println(controller.actualizarNivel(id, nivel));
    }

    private void eliminar() {
        int id = consola.leerEntero("Id del heroe a eliminar: ", 1, Integer.MAX_VALUE);
        System.out.println(controller.eliminar(id));
    }

    private void imprimirListado(List<Heroe> heroes) {
        if (heroes == null) {
            System.out.println(controller.getUltimoError());
            return;
        }
        if (heroes.isEmpty()) {
            System.out.println("No hay heroes guardados todavia.");
            return;
        }
        System.out.println("--- Heroes guardados ---");
        for (Heroe heroe : heroes) {
            System.out.println("  " + heroe);
        }
    }

    private String pedirTipo() {
        int clase = consola.leerEntero("Tipo (1. Guerrero, 2. Mago): ", 1, 2);
        return clase == 1 ? Heroe.TIPO_GUERRERO : Heroe.TIPO_MAGO;
    }

    /**
     * Flujo de integracion con el juego: muestra los heroes guardados y pide el id
     * del que se quiere usar en un combate. Devuelve null si no hay heroes o el id no existe.
     */
    public Heroe elegirHeroe() {
        List<Heroe> heroes = controller.listar();
        imprimirListado(heroes);
        if (heroes == null || heroes.isEmpty()) {
            return null;
        }
        int id = consola.leerEntero("Id del heroe a cargar: ", 1, Integer.MAX_VALUE);
        Heroe heroe = controller.obtenerPorId(id);
        if (heroe == null) {
            System.out.println(HeroeController.NO_EXISTE_ID);
        }
        return heroe;
    }
}
