package com.capelli.database;

import com.capelli.model.CuentaBancaria;
import com.capelli.model.Trabajadora;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TrabajadoraDAO {

    /**
     * Guarda o actualiza una trabajadora y sus cuentas bancarias en la base de datos.
     * Utiliza una transacción para asegurar la integridad de los datos.
     * @param trabajadora El objeto Trabajadora a guardar.
     * @return El ID de la trabajadora guardada o actualizada.
     */
    public int save(Trabajadora trabajadora) throws SQLException, IOException {
        String sqlTrabajadora;
        // MODIFICACIÓN: Se agregan los campos de bono al SQL
        if (trabajadora.getId() == 0) {
            sqlTrabajadora = "INSERT INTO trabajadoras (nombres, apellidos, tipo_ci, numero_ci, telefono, correo, foto, bono_activo, monto_bono, razon_bono) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            sqlTrabajadora = "UPDATE trabajadoras SET nombres = ?, apellidos = ?, tipo_ci = ?, numero_ci = ?, telefono = ?, correo = ?, foto = ?, bono_activo = ?, monto_bono = ?, razon_bono = ? WHERE id = ?";
        }

        Connection conn = null;
        try {
            conn = Database.connect();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sqlTrabajadora)) {

                pstmt.setString(1, trabajadora.getNombres());
                pstmt.setString(2, trabajadora.getApellidos());
                pstmt.setString(3, trabajadora.getTipoCi());
                pstmt.setString(4, trabajadora.getNumeroCi());
                pstmt.setString(5, trabajadora.getTelefono());
                pstmt.setString(6, trabajadora.getCorreoElectronico());

                if (trabajadora.getFoto() != null) {
                    pstmt.setBytes(7, toByteArray(trabajadora.getFoto()));
                } else {
                    pstmt.setNull(7, java.sql.Types.BLOB);
                }

                // NUEVOS CAMPOS DE BONO
                pstmt.setBoolean(8, trabajadora.isBonoActivo());
                pstmt.setDouble(9, trabajadora.getMontoBono());
                pstmt.setString(10, trabajadora.getRazonBono());

                if (trabajadora.getId() != 0) {
                    pstmt.setInt(11, trabajadora.getId()); // El ID pasa a ser el parámetro 11 en el UPDATE
                }
                
                pstmt.executeUpdate();

                if (trabajadora.getId() == 0) {
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                        if (rs.next()) {
                            trabajadora.setId(rs.getInt(1));
                        } else {
                             throw new SQLException("No se pudo obtener el ID de la trabajadora insertada.");
                        }
                    }
                }
            }

            String sqlDeleteCuentas = "DELETE FROM cuentas_bancarias WHERE trabajadora_id = ?";
            try (PreparedStatement pstmtDelete = conn.prepareStatement(sqlDeleteCuentas)) {
                pstmtDelete.setInt(1, trabajadora.getId());
                pstmtDelete.executeUpdate();
            }
            
            String sqlInsertCuenta = "INSERT INTO cuentas_bancarias (trabajadora_id, banco, tipo_cuenta, numero_cuenta, es_principal) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsertCuenta)) {
                for (CuentaBancaria cuenta : trabajadora.getCuentas()) {
                    pstmtInsert.setInt(1, trabajadora.getId());
                    pstmtInsert.setString(2, cuenta.getBanco());
                    pstmtInsert.setString(3, cuenta.getTipoDeCuenta());
                    pstmtInsert.setString(4, cuenta.getNumeroDeCuenta());
                    pstmtInsert.setBoolean(5, cuenta.isEsPrincipal());
                    pstmtInsert.addBatch();
                }
                pstmtInsert.executeBatch();
            }
            
            conn.commit();

        } catch (SQLException | IOException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error haciendo rollback: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    System.err.println("Error cerrando la conexión: " + ex.getMessage());
                }
            }
        }
        return trabajadora.getId();
    }

    /**
     * Obtiene todas las trabajadoras de la base de datos.
     * @return Una lista de objetos Trabajadora.
     */
    public List<Trabajadora> getAll() throws SQLException, IOException {
        List<Trabajadora> trabajadoras = new ArrayList<>();
        String sql = "SELECT * FROM trabajadoras";

        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Trabajadora t = new Trabajadora();
                t.setId(rs.getInt("id"));
                t.setNombres(rs.getString("nombres"));
                t.setApellidos(rs.getString("apellidos"));
                t.setTipoCi(rs.getString("tipo_ci"));
                t.setNumeroCi(rs.getString("numero_ci"));
                t.setTelefono(rs.getString("telefono"));
                t.setCorreoElectronico(rs.getString("correo"));
                
                // Recuperar datos del bono (si las columnas no existen, devolverán valores por defecto o null, 
                // pero como ya ejecutamos el ALTER TABLE en Database.initialize, deberían existir)
                try {
                    t.setBonoActivo(rs.getBoolean("bono_activo"));
                    t.setMontoBono(rs.getDouble("monto_bono"));
                    t.setRazonBono(rs.getString("razon_bono"));
                } catch (SQLException ex) {
                    // En caso de que la columna no exista aún (versión vieja de DB sin actualizar),
                    // ignoramos el error y quedan los valores por defecto.
                }
                
                byte[] fotoBytes = rs.getBytes("foto");
                if (fotoBytes != null) {
                    t.setFoto(new ImageIcon(fotoBytes));
                }
                
                t.setCuentas(getCuentasByTrabajadoraId(t.getId(), conn));
                
                trabajadoras.add(t);
            }
        }
        return trabajadoras;
    }

    /**
     * Elimina una trabajadora de la base de datos.
     * @param id El ID de la trabajadora a eliminar.
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM trabajadoras WHERE id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    private List<CuentaBancaria> getCuentasByTrabajadoraId(int trabajadoraId, Connection conn) throws SQLException {
        List<CuentaBancaria> cuentas = new ArrayList<>();
        String sql = "SELECT * FROM cuentas_bancarias WHERE trabajadora_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, trabajadoraId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                cuentas.add(new CuentaBancaria(
                    rs.getString("banco"),
                    rs.getString("tipo_cuenta"),
                    rs.getString("numero_cuenta"),
                    rs.getBoolean("es_principal")
                ));
            }
        }
        return cuentas;
    }

    private byte[] toByteArray(ImageIcon icon) throws IOException {
        Image image = icon.getImage();
        BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);
        return baos.toByteArray();
    }
}