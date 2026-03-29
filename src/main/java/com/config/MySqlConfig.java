package com.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public class MySqlConfig {

    private static final Logger log = Logger.getLogger(MySqlConfig.class.getName());

    private MySqlConfig() {
        throw new IllegalStateException("Clase de utilidad");
    }

    public static Connection getConnection() throws SQLException {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String pass = System.getenv("DB_PASS");

        if (url == null || user == null || pass == null) {
            log.severe("Error: Faltan variables de entorno (DB_URL, DB_USER, DB_PASS)");
            throw new SQLException("Faltan variables de entorno para la conexión a la BD.");
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            log.info("Intentando conectar a MySQL en: " + url);
            return DriverManager.getConnection(url, user, pass);

        } catch (ClassNotFoundException e) {
            log.severe("No se encontró el driver de MySQL: " + e.getMessage());
            throw new SQLException("Driver MySQL no encontrado en el classpath", e);
        } catch (SQLException e) {
            log.severe("Error de conexión JDBC: " + e.getMessage());
            throw e;
        }
    }
}