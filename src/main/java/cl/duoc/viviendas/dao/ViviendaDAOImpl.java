package cl.duoc.viviendas.dao;

import cl.duoc.viviendas.conexion.ConexionBD;
import cl.duoc.viviendas.model.Casa;
import cl.duoc.viviendas.model.Departamento;
import cl.duoc.viviendas.model.Vivienda;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

// Implemento ViviendaDAO porque aquí concentro toda la lógica JDBC con Oracle;
// así el controlador trabaja solo con la interfaz y no ve SQL.
public class ViviendaDAOImpl implements ViviendaDAO {

    // Corresponden a los valores insertados en la tabla tipo_vivienda.
    private static final int TIPO_DEPARTAMENTO = 1;
    private static final int TIPO_CASA = 2;

    @Override
    public List<Vivienda> obtenerTodos() throws PersistenciaException {
        // Creo la lista donde iré guardando cada Departamento o Casa recuperada.
        List<Vivienda> viviendas = new ArrayList<>();

        // Consulto vivienda junto con tipo_vivienda ordenando por id.
        String sql = "SELECT v.id, v.id_tipo, t.descripcion, v.codigo, v.superficie, v.habitaciones, "
                + "v.piso, v.gasto_comun_al_dia, v.estacionamiento, v.patio "
                + "FROM vivienda v "
                + "JOIN tipo_vivienda t ON v.id_tipo = t.id_tipo "
                + "ORDER BY v.id";

        // Uso try-with-resources para que Connection, PreparedStatement y ResultSet
        // se cierren automáticamente al terminar o si ocurre una excepción.
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Recorro cada fila de la tabla vivienda.
            while (rs.next()) {
                int id = rs.getInt("id");
                int idTipo = rs.getInt("id_tipo");
                String codigo = rs.getString("codigo");
                double superficie = rs.getDouble("superficie");
                int habitaciones = rs.getInt("habitaciones");

                // Reconstruyo Departamento o Casa según el id_tipo de la fila.
                if (idTipo == TIPO_DEPARTAMENTO) {
                    int piso = rs.getInt("piso");
                    // En Oracle los booleanos se guardan como 1 (sí) o 0 (no).
                    boolean gastoComunAlDia = rs.getInt("gasto_comun_al_dia") == 1;
                    boolean estacionamiento = rs.getInt("estacionamiento") == 1;

                    Departamento depto = new Departamento(
                            codigo, superficie, habitaciones, piso, gastoComunAlDia, estacionamiento
                    );
                    depto.setId(id);
                    viviendas.add(depto);

                } else if (idTipo == TIPO_CASA) {
                    boolean patio = rs.getInt("patio") == 1;

                    Casa casa = new Casa(codigo, superficie, habitaciones, patio);
                    casa.setId(id);
                    viviendas.add(casa);
                }
            }

            return viviendas;
        } catch (SQLException e) {
            // Lanzo PersistenciaException indicando qué operación falló.
            throw new PersistenciaException("Error al obtener las viviendas desde la base de datos.", e);
        }
    }

    @Override
    public void insertar(Vivienda vivienda) throws PersistenciaException {
        // Sentencia INSERT parametrizada con ? (el id lo genera la base de datos).
        String sql = "INSERT INTO vivienda ("
                + "id_tipo, codigo, superficie, habitaciones, "
                + "piso, gasto_comun_al_dia, estacionamiento, patio"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Cargo los datos comunes de Vivienda.
            ps.setString(2, vivienda.getCodigo());
            ps.setDouble(3, vivienda.getSuperficie());
            ps.setInt(4, vivienda.getHabitaciones());

            if (vivienda instanceof Departamento) {
                Departamento depto = (Departamento) vivienda;
                ps.setInt(1, TIPO_DEPARTAMENTO);
                ps.setInt(5, depto.getPiso());
                ps.setInt(6, depto.isGastoComunAlDia() ? 1 : 0);
                ps.setInt(7, depto.isEstacionamiento() ? 1 : 0);
                // patio aplica solo a Casa, así que en Departamento queda NULL.
                ps.setNull(8, Types.NUMERIC);

            } else if (vivienda instanceof Casa) {
                Casa casa = (Casa) vivienda;
                ps.setInt(1, TIPO_CASA);
                // piso, gasto_comun_al_dia y estacionamiento aplican solo a Departamento.
                ps.setNull(5, Types.NUMERIC);
                ps.setNull(6, Types.NUMERIC);
                ps.setNull(7, Types.NUMERIC);
                ps.setInt(8, casa.isPatio() ? 1 : 0);
            }

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Error al insertar la vivienda en la base de datos.", e);
        }
    }

    @Override
    public void actualizar(Vivienda vivienda) throws PersistenciaException {
        // Sentencia UPDATE parametrizada: actualizo columnas y filtro por id = ?.
        String sql = "UPDATE vivienda SET "
                + "id_tipo = ?, codigo = ?, superficie = ?, habitaciones = ?, "
                + "piso = ?, gasto_comun_al_dia = ?, estacionamiento = ?, patio = ? "
                + "WHERE id = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(2, vivienda.getCodigo());
            ps.setDouble(3, vivienda.getSuperficie());
            ps.setInt(4, vivienda.getHabitaciones());

            if (vivienda instanceof Departamento) {
                Departamento depto = (Departamento) vivienda;
                ps.setInt(1, TIPO_DEPARTAMENTO);
                ps.setInt(5, depto.getPiso());
                ps.setInt(6, depto.isGastoComunAlDia() ? 1 : 0);
                ps.setInt(7, depto.isEstacionamiento() ? 1 : 0);
                ps.setNull(8, Types.NUMERIC);

            } else if (vivienda instanceof Casa) {
                Casa casa = (Casa) vivienda;
                ps.setInt(1, TIPO_CASA);
                ps.setNull(5, Types.NUMERIC);
                ps.setNull(6, Types.NUMERIC);
                ps.setNull(7, Types.NUMERIC);
                ps.setInt(8, casa.isPatio() ? 1 : 0);
            }

            // Asigno el id de la vivienda para el WHERE id = ?.
            ps.setInt(9, vivienda.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Error al actualizar la vivienda en la base de datos.", e);
        }
    }

    @Override
    public void eliminar(int id) throws PersistenciaException {
        // Elimino únicamente la vivienda con el id indicado.
        String sql = "DELETE FROM vivienda WHERE id = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Error al eliminar la vivienda de la base de datos.", e);
        }
    }
}