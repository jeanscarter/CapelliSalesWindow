package com.capelli.capellisaleswindow;

import com.capelli.clientmanagementwindow.ClientManagementWindow;
import com.capelli.database.Database;
import com.capelli.model.Trabajadora;
import com.capelli.reports.DailyReportWindow;
import com.capelli.salesdashboard.SalesDashboardWindow;
import com.capelli.ui.MainPanel;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.swing.event.TableModelEvent;
import com.capelli.config.AppConfig;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CapelliSalesWindow extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(CapelliSalesWindow.class.getName());
    private Map<String, Double> preciosServicios = new HashMap<>();
    private List<String> trabajadorasNombres = new ArrayList<>();
    private List<Trabajadora> trabajadorasList = new ArrayList<>();
    private final List<String> tiposDescuento = new ArrayList<>(Arrays.asList(AppConfig.getDiscountTypes()));
    private final List<String> metodosPago = new ArrayList<>(Arrays.asList(AppConfig.getPaymentMethods()));
    private final List<String> serviciosConMultiplesTrabajadoras = Arrays.asList(AppConfig.getMultipleWorkerServices());
    private double tasaBcv = AppConfig.getDefaultBcvRate();

    private boolean isDarkMode = true; 

    private final DefaultTableModel tableModel;
    private JTable serviciosTable; 
    private JTextField cedulaField;
    private JLabel nombreClienteLabel;
    private JLabel tasaLabel;
    private JComboBox<String> serviciosComboBox;
    private JComboBox<String> trabajadorasComboBox;
    private JComboBox<String> descuentoComboBox;
    private JTextField propinaField;
    private JComboBox<String> propinaTrabajadoraComboBox;
    private JTextField montoPagadoField;
    private JLabel vueltoLabel;
    private JRadioButton monedaBs, monedaDolar;
    private JComboBox<String> pagoComboBox;
    private JLabel totalLabel;

    private JRadioButton pagoMovilCapelliRadio;
    private JRadioButton pagoMovilRosaRadio;
    private ButtonGroup pagoMovilDestinoGroup;
    private JPanel pagoMovilPanel;

    private final List<VentaServicio> serviciosAgregados = new ArrayList<>();
    private Cliente clienteActual = null;

    public CapelliSalesWindow() {
        super(AppConfig.getAppTitle());

        try {
            InputStream iconStream = getClass().getResourceAsStream(AppConfig.getIconPath());
            if (iconStream != null) {
                setIconImage(ImageIO.read(iconStream));
                LOGGER.info("Icono de aplicación cargado correctamente");
            } else {
                LOGGER.warning("No se pudo cargar el icono de la aplicación");
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error al cargar icono de aplicación", e);
        }

    
        SwingWorker<Double, Void> worker = new SwingWorker<Double, Void>() {
            @Override
            protected Double doInBackground() throws Exception {
                return BCVService.getBCVRate();
            }

            @Override
            protected void done() {
                try {
                    double rate = get();
                    if (rate > 0) {
                        tasaBcv = rate;
                        String formattedRate = String.format("%.2f", tasaBcv);
                        tasaLabel.setText("Tasa BCV: " + formattedRate + " Bs/$");
                    } else {
                        tasaLabel.setText("Tasa BCV: Error de carga");
                    }
                } catch (Exception e) {
                    tasaLabel.setText("Tasa BCV: Error");
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
        JLabel tasaLabel = new JLabel("Tasa BCV: Cargando..."); 

        tableModel = new DefaultTableModel(new String[]{"Servicio", "Trabajador(a)", "Precio ($)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2;
            }
        };

        cargarDatosDesdeDB();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(AppConfig.getDefaultWindowWidth(), AppConfig.getDefaultWindowHeight());
        if (AppConfig.isMaximizedByDefault()) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        mainPanel.add(crearPanelIzquierdo(), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        mainPanel.add(crearPanelTabla(), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        mainPanel.add(crearPanelDerechoInferior(), gbc);

        add(mainPanel);
    }

    private void cargarDatosDesdeDB() {
       
        String sqlServices = "SELECT name, price FROM services";
        try (Connection conn = Database.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlServices)) {
            preciosServicios.clear();
            while (rs.next()) {
                preciosServicios.put(rs.getString("name"), rs.getDouble("price"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar servicios: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
        }

        String sqlEmployees = "SELECT id, nombres, apellidos FROM trabajadoras ORDER BY nombres";
        try (Connection conn = Database.connect(); 
             Statement stmt = conn.createStatement(); 
             ResultSet rs = stmt.executeQuery(sqlEmployees)) {
            
            trabajadorasList.clear();
            trabajadorasNombres.clear();
            
            if (trabajadorasComboBox != null) {
                trabajadorasComboBox.removeAllItems();
            }

            while (rs.next()) {
                Trabajadora t = new Trabajadora();
                t.setId(rs.getInt("id"));
                t.setNombres(rs.getString("nombres"));
                t.setApellidos(rs.getString("apellidos"));
                
                trabajadorasList.add(t);
                String nombreCompleto = t.getNombreCompleto();
                trabajadorasNombres.add(nombreCompleto);

               
                if (trabajadorasComboBox != null) {
                    trabajadorasComboBox.addItem(nombreCompleto);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar trabajadoras: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
        }
        
    }

    private JPanel crearPanelIzquierdo() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Información de Venta"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel clientePanel = new JPanel(new GridBagLayout());
        clientePanel.setBorder(new TitledBorder("Datos de la Cliente"));
        GridBagConstraints gbcCliente = new GridBagConstraints();
        gbcCliente.insets = new Insets(5, 5, 5, 5);
        gbcCliente.fill = GridBagConstraints.HORIZONTAL;

        gbcCliente.gridx = 0;
        gbcCliente.gridy = 0;
        clientePanel.add(new JLabel("Cédula:"), gbcCliente);
        cedulaField = new JTextField(15);

        tasaLabel = new JLabel("Tasa BCV: Cargando...");
        tasaLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 4; 
        gbcCliente.gridwidth = 2;
        gbcCliente.anchor = GridBagConstraints.CENTER;
        clientePanel.add(tasaLabel, gbcCliente);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(clientePanel, gbc);
      
        cedulaField.addActionListener(e -> buscarClienteEnDB());
       
        gbcCliente.gridx = 1;
        gbcCliente.gridy = 0;
        gbcCliente.gridwidth = 2;
        gbcCliente.weightx = 1.0;
        clientePanel.add(cedulaField, gbcCliente);

        JButton buscarClienteBtn = new JButton("Buscar Cliente");
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 1;
        gbcCliente.gridwidth = 1;
        gbcCliente.weightx = 0.5;
        clientePanel.add(buscarClienteBtn, gbcCliente);
        buscarClienteBtn.addActionListener(e -> buscarClienteEnDB());

        JButton gestionarClientesBtn = new JButton("Gestionar Clientes");
        gbcCliente.gridx = 1;
        gbcCliente.gridy = 1;
        gbcCliente.gridwidth = 1;
        gbcCliente.weightx = 0.5;
        clientePanel.add(gestionarClientesBtn, gbcCliente);
        gestionarClientesBtn.addActionListener(e -> new ClientManagementWindow().setVisible(true));
        
        JButton gestionarTrabajadorasBtn = new JButton("Gestionar Trabajadoras");
        gbcCliente.gridx = 2; 
        gbcCliente.gridy = 1;
        gbcCliente.gridwidth = 1;
        gbcCliente.weightx = 0.5;
        clientePanel.add(gestionarTrabajadorasBtn, gbcCliente);
        
        gestionarTrabajadorasBtn.addActionListener(e -> {
            JFrame frame = new JFrame("Gestión de Trabajadoras");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.getContentPane().add(new MainPanel(isDarkMode)); 
            frame.pack();
            frame.setLocationRelativeTo(null); // Centrar
            frame.setVisible(true);

           
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                    cargarDatosDesdeDB();
                }
            });
        });

        nombreClienteLabel = new JLabel("Nombre: No cargado");
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 2;
        gbcCliente.gridwidth = 2;
        clientePanel.add(nombreClienteLabel, gbcCliente);

        JButton verReportesBtn = new JButton("Ver Reportes de Ventas");
        verReportesBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 3;
        gbcCliente.gridwidth = 2;
        gbcCliente.insets = new Insets(15, 5, 5, 5);
        clientePanel.add(verReportesBtn, gbcCliente);
        verReportesBtn.addActionListener(e -> {
            SalesDashboardWindow dashboard = new SalesDashboardWindow();
            dashboard.setVisible(true);
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(clientePanel, gbc);

        gbc.insets = new Insets(5, 5, 5, 5);

        JPanel serviciosPanel = new JPanel(new GridBagLayout());
        serviciosPanel.setBorder(new TitledBorder("Servicios"));
        GridBagConstraints gbcServicios = new GridBagConstraints();
        gbcServicios.insets = new Insets(5, 5, 5, 5);
        gbcServicios.fill = GridBagConstraints.HORIZONTAL;

        gbcServicios.gridx = 0;
        gbcServicios.gridy = 0;
        serviciosPanel.add(new JLabel("Servicio:"), gbcServicios);
        serviciosComboBox = new JComboBox<>(preciosServicios.keySet().toArray(new String[0]));
        gbcServicios.gridx = 1;
        gbcServicios.gridy = 0;
        gbcServicios.weightx = 1.0;
        serviciosPanel.add(serviciosComboBox, gbcServicios);

        gbcServicios.gridx = 0;
        gbcServicios.gridy = 1;
        serviciosPanel.add(new JLabel("Trabajador(a):"), gbcServicios);
        trabajadorasComboBox = new JComboBox<>(trabajadorasNombres.toArray(new String[0]));
        gbcServicios.gridx = 1;
        gbcServicios.gridy = 1;
        gbcServicios.weightx = 1.0;
        serviciosPanel.add(trabajadorasComboBox, gbcServicios);

      
        JButton agregarBtn = new JButton("Agregar Servicio");
        gbcServicios.gridx = 0;
        gbcServicios.gridy = 2;
        gbcServicios.gridwidth = 1; // Se ajusta a 1
        gbcServicios.weightx = 0.5;
        serviciosPanel.add(agregarBtn, gbcServicios);
        agregarBtn.addActionListener(e -> agregarServicio());

        JButton eliminarBtn = new JButton("Eliminar Servicio");
        gbcServicios.gridx = 1;
        gbcServicios.gridy = 2;
        gbcServicios.gridwidth = 1;
        gbcServicios.weightx = 0.5;
        serviciosPanel.add(eliminarBtn, gbcServicios);
        eliminarBtn.addActionListener(e -> eliminarServicioSeleccionado());
        

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(serviciosPanel, gbc);

        JButton verReporteDiarioBtn = new JButton("Ver Reporte del Día");
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 5; 
        gbcCliente.gridwidth = 2;
        gbcCliente.anchor = GridBagConstraints.CENTER;
        clientePanel.add(verReporteDiarioBtn, gbcCliente);

        verReporteDiarioBtn.addActionListener(e -> {
         
            new DailyReportWindow().setVisible(true);
        });

        JToggleButton themeToggle = new JToggleButton("Cambiar a Modo Claro", true);
        themeToggle.addActionListener(e -> {
            try {
                if (themeToggle.isSelected()) {
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                    themeToggle.setText("Cambiar a Modo Claro");
                    isDarkMode = true;
                } else {
                    UIManager.setLookAndFeel(new FlatLightLaf());
                    themeToggle.setText("Cambiar a Modo Oscuro");
                    isDarkMode = false;
                }
                SwingUtilities.updateComponentTreeUI(CapelliSalesWindow.this);
            } catch (UnsupportedLookAndFeelException ex) {
                ex.printStackTrace();
            }
        });
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(themeToggle, gbc);

        return panel;
    }

    private void buscarClienteEnDB() {
        String cedula = cedulaField.getText().trim();
        if (cedula.isEmpty()) {
            return;
        }

        String sql = "SELECT client_id, full_name FROM clients WHERE cedula = ?";
        try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cedula);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                clienteActual = new Cliente(rs.getInt("client_id"), cedula, rs.getString("full_name"));
                nombreClienteLabel.setText("Nombre: " + clienteActual.getNombre());
                JOptionPane.showMessageDialog(this, "Cliente cargado: " + clienteActual.getNombre(), "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                clienteActual = null;
                nombreClienteLabel.setText("Nombre: No encontrado");
                int response = JOptionPane.showConfirmDialog(this,
                        "Cliente no encontrado. ¿Desea registrar un nuevo cliente?",
                        "Cliente No Encontrado", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (response == JOptionPane.YES_OPTION) {

                    ClientManagementWindow clientWindow = new ClientManagementWindow(cedula);
                    clientWindow.setVisible(true);
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al buscar cliente: " + ex.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void agregarServicio() {
        String servicio = (String) serviciosComboBox.getSelectedItem();
        String trabajadora = (String) trabajadorasComboBox.getSelectedItem();
        Double precioOriginal = preciosServicios.get(servicio);

        boolean servicioYaAgregado = serviciosAgregados.stream().anyMatch(vs -> vs.getServicio().equals(servicio));
        if (serviciosConMultiplesTrabajadoras.contains(servicio) && servicioYaAgregado) {
            double precioDividido = precioOriginal / 2.0;
            for (int i = 0; i < serviciosAgregados.size(); i++) {
                if (serviciosAgregados.get(i).getServicio().equals(servicio)) {
                    serviciosAgregados.get(i).setPrecio(precioDividido);
                    tableModel.setValueAt(new DecimalFormat("#.##").format(precioDividido), i, 2);
                    break;
                }
            }
            serviciosAgregados.add(new VentaServicio(servicio, trabajadora, precioDividido));
            tableModel.addRow(new Object[]{servicio, trabajadora, new DecimalFormat("#.##").format(precioDividido)});
        } else {
            serviciosAgregados.add(new VentaServicio(servicio, trabajadora, precioOriginal));
            tableModel.addRow(new Object[]{servicio, trabajadora, new DecimalFormat("#.##").format(precioOriginal)});
        }
        actualizarTotales();
    }

    private void eliminarServicioSeleccionado() {
        int selectedRow = serviciosTable.getSelectedRow();

        if (selectedRow >= 0) {
      
            serviciosAgregados.remove(selectedRow);
        
            tableModel.removeRow(selectedRow);
       
            actualizarTotales();
        } else {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un servicio de la tabla para eliminar.", "Ningún Servicio Seleccionado", JOptionPane.WARNING_MESSAGE);
        }
    }
 

    private JPanel crearPanelTabla() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Servicios a Facturar"));

        serviciosTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(serviciosTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 2) {
                int row = e.getFirstRow();
                try {
                    double nuevoPrecio = Double.parseDouble(tableModel.getValueAt(row, 2).toString().replace(",", "."));
                    serviciosAgregados.get(row).setPrecio(nuevoPrecio);
                    actualizarTotales();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Por favor, ingrese un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        return panel;
    }

    private JPanel crearPanelDerechoInferior() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Total y Pago"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JPanel totalesPanel = new JPanel(new GridLayout(4, 2));
        totalesPanel.setBorder(new TitledBorder("Resumen"));
        JLabel subtotalLabel = new JLabel("Subtotal ($): 0.00");
        JLabel descuentoLabel = new JLabel("Descuento ($): 0.00");
        totalLabel = new JLabel("Total ($): 0.00");
        JLabel propinaLabelGUI = new JLabel("Propina ($): 0.00");
        totalesPanel.add(subtotalLabel);
        totalesPanel.add(descuentoLabel);
        totalesPanel.add(propinaLabelGUI);
        totalesPanel.add(totalLabel);

        JPanel descuentoPanel = new JPanel(new BorderLayout());
        descuentoPanel.setBorder(new TitledBorder("Descuento"));
        descuentoComboBox = new JComboBox<>(tiposDescuento.toArray(new String[0]));
        descuentoPanel.add(descuentoComboBox, BorderLayout.CENTER);

        JPanel propinaPanel = new JPanel(new GridBagLayout());
        propinaPanel.setBorder(new TitledBorder("Propina"));
        GridBagConstraints gbcPropina = new GridBagConstraints();
        gbcPropina.insets = new Insets(5, 5, 5, 5);
        gbcPropina.fill = GridBagConstraints.HORIZONTAL;
        gbcPropina.gridx = 0;
        gbcPropina.gridy = 0;
        propinaPanel.add(new JLabel("Monto ($):"), gbcPropina);
        propinaField = new JTextField("0.00");
        gbcPropina.gridx = 1;
        gbcPropina.gridy = 0;
        gbcPropina.weightx = 1.0;
        propinaPanel.add(propinaField, gbcPropina);
        gbcPropina.gridx = 0;
        gbcPropina.gridy = 1;
        propinaPanel.add(new JLabel("Para:"), gbcPropina);
        List<String> propinaDestinatarios = new ArrayList<>(trabajadorasNombres);
        propinaDestinatarios.add("Salón");
        propinaTrabajadoraComboBox = new JComboBox<>(propinaDestinatarios.toArray(new String[0]));
        gbcPropina.gridx = 1;
        gbcPropina.gridy = 1;
        propinaPanel.add(propinaTrabajadoraComboBox, gbcPropina);

        JPanel pagoPanel = new JPanel(new GridBagLayout());
        pagoPanel.setBorder(new TitledBorder("Opciones de Pago"));
        GridBagConstraints gbcPago = new GridBagConstraints();
        gbcPago.insets = new Insets(5, 5, 5, 5);
        gbcPago.fill = GridBagConstraints.HORIZONTAL;
        gbcPago.gridx = 0;
        gbcPago.gridy = 0;
        pagoPanel.add(new JLabel("Moneda:"), gbcPago);
        monedaBs = new JRadioButton("Bs");
        monedaDolar = new JRadioButton("$", true);
        ButtonGroup monedaGroup = new ButtonGroup();
        monedaGroup.add(monedaBs);
        monedaGroup.add(monedaDolar);
        JPanel monedaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        monedaPanel.add(monedaDolar);
        monedaPanel.add(monedaBs);
        gbcPago.gridx = 1;
        gbcPago.gridy = 0;
        pagoPanel.add(monedaPanel, gbcPago);

        pagoPanel.setBorder(new TitledBorder("Opciones de Pago"));
        gbcPago.insets = new Insets(5, 5, 5, 5);
        gbcPago.fill = GridBagConstraints.HORIZONTAL;

        gbcPago.gridx = 0;
        gbcPago.gridy = 1;
        pagoPanel.add(new JLabel("Método:"), gbcPago);
        pagoComboBox = new JComboBox<>(metodosPago.toArray(new String[0]));
        gbcPago.gridx = 1;
        gbcPago.gridy = 1;
        gbcPago.weightx = 1.0;
        pagoPanel.add(pagoComboBox, gbcPago);

        pagoMovilPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pagoMovilCapelliRadio = new JRadioButton("Capelli", true);
        pagoMovilRosaRadio = new JRadioButton("Rosa");
        pagoMovilDestinoGroup = new ButtonGroup();
        pagoMovilDestinoGroup.add(pagoMovilCapelliRadio);
        pagoMovilDestinoGroup.add(pagoMovilRosaRadio);
        pagoMovilPanel.add(new JLabel("Destino:"));
        pagoMovilPanel.add(pagoMovilCapelliRadio);
        pagoMovilPanel.add(pagoMovilRosaRadio);
        pagoMovilPanel.setVisible(false);

        gbcPago.gridx = 1;
        gbcPago.gridy = 2; 
        pagoPanel.add(pagoMovilPanel, gbcPago);
      

        gbcPago.gridx = 0;
        gbcPago.gridy = 3; 
        pagoPanel.add(new JLabel("Monto Pagado:"), gbcPago);
        montoPagadoField = new JTextField("0.00");
        gbcPago.gridx = 1;
        gbcPago.gridy = 3; 
        gbcPago.weightx = 1.0;
        pagoPanel.add(montoPagadoField, gbcPago);

        gbcPago.gridx = 0;
        gbcPago.gridy = 4;
        pagoPanel.add(new JLabel("Vuelto:"), gbcPago);
        vueltoLabel = new JLabel("0.00");
        gbcPago.gridx = 1;
        gbcPago.gridy = 4; // Se ajusta el gridy
        gbcPago.weightx = 1.0;
        pagoPanel.add(vueltoLabel, gbcPago);

        ActionListener updateListener = e -> actualizarTotales();
        descuentoComboBox.addActionListener(updateListener);
        propinaField.getDocument().addDocumentListener(new SimpleDocumentListener(this::actualizarTotales));
        montoPagadoField.getDocument().addDocumentListener(new SimpleDocumentListener(this::actualizarTotales));
        monedaBs.addActionListener(updateListener);
        monedaDolar.addActionListener(updateListener);

      
        pagoComboBox.addActionListener(e -> {
            String selectedMethod = (String) pagoComboBox.getSelectedItem();
            pagoMovilPanel.setVisible("Pago Movil".equals(selectedMethod));
        });
     

        JButton facturarBtn = new JButton("Generar Factura");
        facturarBtn.setFont(new Font("Arial", Font.BOLD, 16));
        facturarBtn.addActionListener(e -> generarFactura());

   
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(descuentoPanel, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(propinaPanel, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(totalesPanel, gbc);
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(pagoPanel, gbc);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(facturarBtn, gbc);

        return panel;
    }

    private void actualizarTotales() {
        double subtotal = serviciosAgregados.stream().mapToDouble(VentaServicio::getPrecio).sum();
        double propina = 0.0;
        try {
            propina = Double.parseDouble(propinaField.getText());
        } catch (NumberFormatException ignored) {
        }

        double descuento = 0.0;
        String tipoDesc = Objects.requireNonNull(descuentoComboBox.getSelectedItem()).toString();

  
        if (tipoDesc.equals("Promoción")) {
            descuento = subtotal * AppConfig.getPromoDiscountPercentage();
            LOGGER.fine("Descuento por promoción aplicado: " + descuento);
        }

        double total = subtotal - descuento + propina;
        DecimalFormat df = new DecimalFormat("#,##0.00");
        double montoPagado = 0.0;
        try {
            montoPagado = Double.parseDouble(montoPagadoField.getText());
        } catch (NumberFormatException ignored) {
        }

        if (monedaBs.isSelected()) {
            double tasa = tasaBcv;

            totalLabel.setText("Total (Bs): " + df.format(total * tasa));
            vueltoLabel.setText(df.format(montoPagado - (total * tasa)));
        } else {

            totalLabel.setText("Total ($): " + df.format(total));
            vueltoLabel.setText(df.format(montoPagado - total));
        }
    }

    private void generarFactura() {
        if (serviciosAgregados.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe agregar al menos un servicio.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

 
        double subtotal = serviciosAgregados.stream().mapToDouble(VentaServicio::getPrecio).sum();
        double propina = 0.0;
        try {
            propina = Double.parseDouble(propinaField.getText());
        } catch (NumberFormatException e) {

        }
        String tipoDesc = Objects.requireNonNull(descuentoComboBox.getSelectedItem()).toString();
        double descuento = tipoDesc.equals("Promoción") ? subtotal * 0.20 : 0.0;
        double total = subtotal - descuento + propina;


        Connection conn = null;
        long saleId = -1; 

        try {
            conn = Database.connect();
            conn.setAutoCommit(false); 


            String sqlSale = "INSERT INTO sales(client_id, subtotal, discount_type, discount_amount, total, payment_method, currency, payment_destination) VALUES(?,?,?,?,?,?,?,?)";
            try (PreparedStatement pstmtSale = conn.prepareStatement(sqlSale)) {
                if (clienteActual != null) {
                    pstmtSale.setInt(1, clienteActual.getId());
                } else {
                    pstmtSale.setNull(1, java.sql.Types.INTEGER);
                }
                pstmtSale.setDouble(2, subtotal);
                pstmtSale.setString(3, tipoDesc);
                pstmtSale.setDouble(4, descuento);
                pstmtSale.setDouble(5, total);
                
                String metodoPago = Objects.requireNonNull(pagoComboBox.getSelectedItem()).toString();
                pstmtSale.setString(6, metodoPago);
                pstmtSale.setString(7, monedaDolar.isSelected() ? "$" : "Bs");

                if ("Pago Movil".equals(metodoPago)) {
                    pstmtSale.setString(8, pagoMovilRosaRadio.isSelected() ? "Rosa" : "Capelli");
                } else {
                    pstmtSale.setNull(8, java.sql.Types.VARCHAR);
                }

                int affectedRows = pstmtSale.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("La inserción de la venta falló, ninguna fila afectada.");
                }
            }

            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    saleId = rs.getLong(1);
                } else {
                    throw new SQLException("No se pudo obtener el ID de la venta después de la inserción.");
                }
            }

            String sqlItem = "INSERT INTO sale_items(sale_id, service_id, employee_id, price_at_sale) VALUES (?, (SELECT service_id FROM services WHERE name = ?), ?, ?)";
            try (PreparedStatement pstmtItem = conn.prepareStatement(sqlItem)) {
                for (VentaServicio servicio : serviciosAgregados) {
                    

                    String nombreTrabajadoraSeleccionada = servicio.getTrabajadora();
                    int trabajadoraId = -1;
                    
                    for(Trabajadora t : trabajadorasList) {
                        if(t.getNombreCompleto().equals(nombreTrabajadoraSeleccionada)){
                            trabajadoraId = t.getId();
                            break;
                        }
                    }

                    if (trabajadoraId == -1) {
                        throw new SQLException("No se pudo encontrar el ID de la trabajadora: " + nombreTrabajadoraSeleccionada);
                    }

                    pstmtItem.setLong(1, saleId);
                    pstmtItem.setString(2, servicio.getServicio());
                    pstmtItem.setInt(3, trabajadoraId);
                    pstmtItem.setDouble(4, servicio.getPrecio());
                    pstmtItem.executeUpdate();
                }
            }

            if (propina > 0) {
                String sqlTip = "INSERT INTO tips(sale_id, recipient_name, amount) VALUES(?,?,?)";
                try (PreparedStatement pstmtTip = conn.prepareStatement(sqlTip)) {
                    pstmtTip.setLong(1, saleId);
                    pstmtTip.setString(2, Objects.requireNonNull(propinaTrabajadoraComboBox.getSelectedItem()).toString());
                    pstmtTip.setDouble(3, propina);
                    pstmtTip.executeUpdate();
                }
            }

            conn.commit(); 
            JOptionPane.showMessageDialog(this, "Venta registrada en la base de datos con éxito (ID: " + saleId + ").", "Factura Generada", JOptionPane.INFORMATION_MESSAGE);
            limpiarVentana();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al registrar la venta: " + e.getMessage(), "Error de Transacción", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.out.println("Error al hacer rollback: " + ex.getMessage());
            }
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println("Error al cerrar conexión: " + ex.getMessage());
            }
        }
    }

    private void limpiarVentana() {
        clienteActual = null;
        cedulaField.setText("");
        nombreClienteLabel.setText("Nombre: No cargado");
        serviciosAgregados.clear();
        tableModel.setRowCount(0);
        propinaField.setText("0.00");
        montoPagadoField.setText("0.00");
        descuentoComboBox.setSelectedIndex(0);
        actualizarTotales();
    }

    private static class Cliente {

        private final int id;
        private final String cedula;
        private final String nombre;

        public Cliente(int id, String cedula, String nombre) {
            this.id = id;
            this.cedula = cedula;
            this.nombre = nombre;
        }

        public int getId() {
            return id;
        }

        public String getCedula() {
            return cedula;
        }

        public String getNombre() {
            return nombre;
        }
    }

    private static class VentaServicio {

        private final String servicio;
        private final String trabajadora;
        private double precio;

        public VentaServicio(String servicio, String trabajadora, double precio) {
            this.servicio = servicio;
            this.trabajadora = trabajadora;
            this.precio = precio;
        }

        public String getServicio() {
            return servicio;
        }

        public String getTrabajadora() {
            return trabajadora;
        }

        public double getPrecio() {
            return precio;
        }

        public void setPrecio(double precio) {
            this.precio = precio;
        }
    }

    public static void main(String[] args) {
        LOGGER.info("=== INICIANDO APLICACIÓN CAPELLI ===");
        AppConfig.printConfiguration(); // Para debug

        Database.initialize();

        try {
            if (AppConfig.isDarkModeDefault()) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                LOGGER.info("Tema oscuro aplicado");
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
                LOGGER.info("Tema claro aplicado");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error al inicializar Look and Feel", ex);
        }

        SwingUtilities.invokeLater(() -> {
            CapelliSalesWindow window = new CapelliSalesWindow();
            window.setVisible(true);
            LOGGER.info("Ventana principal mostrada");
        });
    }

    private static class SimpleDocumentListener implements javax.swing.event.DocumentListener {

        private final Runnable callback;

        public SimpleDocumentListener(Runnable callback) {
            this.callback = callback;
        }

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            callback.run();
        }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            callback.run();
        }

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            callback.run();
        }
    }
}