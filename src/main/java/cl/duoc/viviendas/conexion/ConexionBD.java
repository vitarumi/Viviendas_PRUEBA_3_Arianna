package cl.duoc.viviendas.conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Reúne los datos de conexión a la base de datos y entrega conexiones.
 * Ninguna otra clase debe contener estos datos.
 */
public class ConexionBD {

    // =====================================================================
    private static final String URL = "jdbc:oracle:thin:@localhost:1521/xe";
    private static final String USUARIO = "SYSTEM";
    private static final String CLAVE = "777";

    private ConexionBD() {
    }

    /**
     * @return una conexión abierta a la base de datos. Quien la utiliza debe cerrarla.
     * @throws SQLException si no es posible conectarse.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, CLAVE);
    }
}
