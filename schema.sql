-- EJERCICIO GRUPAL: Registro de Heroes persistente
-- Esquema de la base de datos rpg_grupal_db.
-- Ejecutar una sola vez antes de iniciar el juego (por ejemplo: mysql -u root -p < schema.sql).

CREATE DATABASE IF NOT EXISTS rpg_grupal_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE rpg_grupal_db;

-- Cada heroe guardado del juego. El nombre es unico para que el mismo heroe
-- se actualice en lugar de duplicarse entre sesiones.
CREATE TABLE IF NOT EXISTS heroes (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(50) NOT NULL UNIQUE,
    tipo         ENUM('GUERRERO', 'MAGO') NOT NULL,
    vida_maxima  INT NOT NULL,
    nivel        INT NOT NULL DEFAULT 1,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_vida_positiva CHECK (vida_maxima > 0),
    CONSTRAINT chk_nivel_positivo CHECK (nivel >= 1)
);

-- Datos de ejemplo para probar "Cargar heroe desde BD" sin insertar a mano.
INSERT INTO heroes (nombre, tipo, vida_maxima, nivel) VALUES
    ('Aldair', 'GUERRERO', 120, 2),
    ('Lyra',   'MAGO',      90,  1);
