package com.rpg.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unica clase del proyecto que conoce las credenciales de la base de datos.
 * Lee el archivo config.txt (raiz del proyecto) y entrega conexiones JDBC.
 * Ninguna otra clase lee el archivo ni conoce el usuario o la clave.
 *
 * Partes de la URL "jdbc:mysql://localhost:3306/rpg_grupal_db":
 * - jdbc:mysql:// -> protocolo: JDBC como puente y MySQL como dialecto del servidor.
 * - localhost     -> host: la maquina donde corre el servidor MySQL (misma PC aqui).
 * - 3306          -> puerto de escucha por defecto de MySQL.
 * - rpg_grupal_db -> esquema (base de datos) que se selecciona al conectar.
 *
 * DriverManager busca entre los drivers registrados (SPI de JDBC 4+) el que
 * acepte esta URL; si falta la dependencia mysql-connector-j en el pom,
 * lanza "No suitable driver found for jdbc:mysql:...".
 */
public final class ConexionDB {

    public static final String ARCHIVO_CONFIG = "config.txt";

    private ConexionDB() {
        // Clase utilitaria: no se instancia.
    }

    /** Devuelve una conexion nueva; el que la pide la cierra con try-with-resources. */
    public static Connection getConnection() throws SQLException {
        Map<String, String> config = leerConfiguracion();
        return DriverManager.getConnection(config.get("url"), config.get("usuario"), config.get("clave"));
    }

    /**
     * Lee config.txt con File + BufferedReader/FileReader linea por linea (readLine),
     * separando cada linea en clave y valor con split("=", 2).
     * Si el archivo no existe, no se puede leer o falta una clave, lanza una SQLException
     * con un mensaje claro para el usuario (el juego no se cae con una traza).
     */
    private static Map<String, String> leerConfiguracion() throws SQLException {
        File archivo = new File(ARCHIVO_CONFIG);
        if (!archivo.exists()) {
            throw new SQLException("No se encontro " + ARCHIVO_CONFIG + " en la raiz del proyecto. "
                    + "Copia config.example.txt como config.txt y pon tus datos reales.");
        }
        Map<String, String> valores = new HashMap<>();
        try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty() || !linea.contains("=")) {
                    continue; // se ignoran lineas vacias o sin formato clave=valor
                }
                String[] partes = linea.split("=", 2);
                valores.put(partes[0].trim(), partes[1].trim());
            }
        } catch (FileNotFoundException e) {
            throw new SQLException("No se encontro el archivo config.txt: " + e.getMessage());
        } catch (IOException e) {
            throw new SQLException("No se pudo leer config.txt: " + e.getMessage());
        }
        for (String clave : List.of("url", "usuario", "clave")) {
            if (!valores.containsKey(clave) || valores.get(clave).isEmpty()) {
                throw new SQLException("A config.txt le falta la clave '" + clave + "'.");
            }
        }
        return valores;
    }
}
