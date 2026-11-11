package com.rpg.controller;

import com.rpg.dao.HeroeDAO;
import com.rpg.model.Heroe;

import java.sql.SQLException;
import java.util.List;

/**
 * Coordina la gestion de heroes: aplica las reglas de negocio, llama al DAO
 * y decide el mensaje que vera el usuario. No imprime nada (eso es de la View)
 * y no conoce JDBC mas alla de capturar SQLException.
 */
public class HeroeController {

    public static final String NO_EXISTE_ID = "No existe un heroe con ese id.";

    private final HeroeDAO dao;
    private String ultimoError = "Sin errores.";

    public HeroeController(HeroeDAO dao) {
        this.dao = dao;
    }

    /** Inserta un heroe nuevo tras validar sus datos. @return mensaje para la View */
    public String insertar(String nombre, String tipo, int vidaMaxima, int nivel) {
        try {
            Heroe heroe = new Heroe(0, nombre, tipo, vidaMaxima, nivel); // el POJO valida
            int filas = dao.insertar(heroe);
            if (filas == 0) {
                return "No se pudo guardar el heroe.";
            }
            return "Heroe guardado: " + heroe + ". Su id lo ves en la opcion 2 (listar).";
        } catch (SQLException e) {
            return traducir(e);
        }
    }

    /** @return la lista completa de heroes, o null si la BD fallo (con mensaje guardado) */
    public List<Heroe> listar() {
        try {
            return dao.listar();
        } catch (SQLException e) {
            ultimoError = traducir(e);
            return null;
        }
    }

    /** @return los heroes del tipo pedido (GUERRERO o MAGO), o null si la BD fallo */
    public List<Heroe> buscarPorTipo(String tipo) {
        String tipoNormalizado = normalizarTipo(tipo);
        if (tipoNormalizado == null) {
            ultimoError = "El tipo solo puede ser GUERRERO o MAGO.";
            return null;
        }
        try {
            return dao.buscarPorTipo(tipoNormalizado);
        } catch (SQLException e) {
            ultimoError = traducir(e);
            return null;
        }
    }

    /** Busca un heroe por id dentro del listado (el DAO solo ofrece las 5 consultas del taller). */
    public Heroe obtenerPorId(int id) {
        List<Heroe> heroes = listar();
        if (heroes == null) {
            return null;
        }
        for (Heroe heroe : heroes) {
            if (heroe.getId() == id) {
                return heroe;
            }
        }
        return null;
    }

    /** Actualiza el nivel. @return mensaje para la View */
    public String actualizarNivel(int id, int nivel) {
        if (nivel < 1) {
            return "El nivel debe ser mayor o igual a 1.";
        }
        try {
            int filas = dao.actualizarNivel(id, nivel);
            if (filas == 0) {
                return NO_EXISTE_ID;
            }
            return "Nivel actualizado (" + filas + " fila(s) afectadas).";
        } catch (SQLException e) {
            return traducir(e);
        }
    }

    /** Elimina un heroe. @return mensaje para la View */
    public String eliminar(int id) {
        try {
            int filas = dao.eliminar(id);
            if (filas == 0) {
                return NO_EXISTE_ID;
            }
            return "Heroe eliminado (" + filas + " fila(s) afectadas).";
        } catch (SQLException e) {
            return traducir(e);
        }
    }

    public String getUltimoError() {
        return ultimoError;
    }

    /** Acepta "mago", "Mago"... y devuelve el valor canonico que espera la columna ENUM. */
    private String normalizarTipo(String tipo) {
        if (tipo == null) {
            return null;
        }
        String normalizado = tipo.trim().toUpperCase();
        return normalizado.equals(Heroe.TIPO_GUERRERO) || normalizado.equals(Heroe.TIPO_MAGO)
                ? normalizado : null;
    }

    /**
     * Traduce los errores de la BD a mensajes claros: duplicados, conexion caida,
     * config incompleta, etc. Nunca se imprime una traza.
     */
    private String traducir(SQLException e) {
        // 1062 = entrada duplicada (nombre UNIQUE); 23000 = estado ANSI de integridad.
        if (e.getErrorCode() == 1062 || "23000".equals(e.getSQLState())) {
            return "Ya existe un heroe con ese nombre (el nombre debe ser unico).";
        }
        return "Error de base de datos: " + e.getMessage();
    }
}
