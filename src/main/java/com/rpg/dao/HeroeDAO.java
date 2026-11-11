package com.rpg.dao;

import com.rpg.model.Heroe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Todo el JDBC de la entidad Heroe vive aqui (y solo aqui).
 * Cada metodo abre su conexion con try-with-resources, usa PreparedStatement
 * con ? para los datos y nunca devuelve un ResultSet abierto: la fila se
 * convierte a Heroe antes de salir del metodo.
 */
public class HeroeDAO {

    private static final String SQL_INSERTAR =
            "INSERT INTO heroes (nombre, tipo, vida_maxima, nivel) VALUES (?, ?, ?, ?)";
    private static final String SQL_LISTAR =
            "SELECT id, nombre, tipo, vida_maxima, nivel FROM heroes ORDER BY id";
    private static final String SQL_BUSCAR_POR_TIPO =
            "SELECT id, nombre, tipo, vida_maxima, nivel FROM heroes WHERE tipo = ? ORDER BY id";
    private static final String SQL_ACTUALIZAR_NIVEL =
            "UPDATE heroes SET nivel = ? WHERE id = ?";
    private static final String SQL_ELIMINAR =
            "DELETE FROM heroes WHERE id = ?";

    /** Inserta un heroe nuevo. @return filas afectadas (1 si el nombre era nuevo) */
    public int insertar(Heroe heroe) throws SQLException {
        try (Connection conexion = ConexionDB.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_INSERTAR)) {
            sentencia.setString(1, heroe.getNombre());
            sentencia.setString(2, heroe.getTipo());
            sentencia.setInt(3, heroe.getVidaMaxima());
            sentencia.setInt(4, heroe.getNivel());
            return sentencia.executeUpdate();
        }
    }

    /** Lista todos los heroes ordenados por id. */
    public List<Heroe> listar() throws SQLException {
        try (Connection conexion = ConexionDB.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR);
             ResultSet resultado = sentencia.executeQuery()) {
            List<Heroe> heroes = new ArrayList<>();
            while (resultado.next()) {
                heroes.add(mapearFila(resultado));
            }
            return heroes;
        }
    }

    /** Devuelve los heroes de un tipo (GUERRERO o MAGO); el dato viaja como parametro ?. */
    public List<Heroe> buscarPorTipo(String tipo) throws SQLException {
        try (Connection conexion = ConexionDB.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR_POR_TIPO)) {
            sentencia.setString(1, tipo);
            try (ResultSet resultado = sentencia.executeQuery()) {
                List<Heroe> heroes = new ArrayList<>();
                while (resultado.next()) {
                    heroes.add(mapearFila(resultado));
                }
                return heroes;
            }
        }
    }

    /** Cambia el nivel de un heroe. @return filas afectadas (0 si el id no existe) */
    public int actualizarNivel(int id, int nivel) throws SQLException {
        try (Connection conexion = ConexionDB.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_ACTUALIZAR_NIVEL)) {
            sentencia.setInt(1, nivel);
            sentencia.setInt(2, id);
            return sentencia.executeUpdate();
        }
    }

    /** Elimina un heroe. @return filas afectadas (0 si el id no existe) */
    public int eliminar(int id) throws SQLException {
        try (Connection conexion = ConexionDB.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_ELIMINAR)) {
            sentencia.setInt(1, id);
            return sentencia.executeUpdate();
        }
    }

    /** Convierte la fila actual del ResultSet en un objeto Heroe. */
    private static Heroe mapearFila(ResultSet fila) throws SQLException {
        return new Heroe(
                fila.getInt("id"),
                fila.getString("nombre"),
                fila.getString("tipo"),
                fila.getInt("vida_maxima"),
                fila.getInt("nivel"));
    }
}
