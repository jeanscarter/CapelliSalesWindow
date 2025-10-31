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
import com.capelli.database.ServiceDAO;
import com.capelli.model.Service;
import com.capelli.servicemanagement.ServiceManagementWindow;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.capelli.validation.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

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
    private JComboBox<String> cedulaTipoComboBox;
    private JTextField cedulaNumeroField;
    private JLabel nombreClienteLabel;
    private final JLabel tasaLabel = new JLabel("Tasa BCV: Cargando...");
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

    private JCheckBox clienteProductoCheck;
    private Map<String, Service> serviciosMap = new HashMap<>(); 

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

        // Validación en tiempo real para propina
        propinaField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
            ValidationHelper.resetFieldBorder(propinaField);
            try {
                double propina = Double.parseDouble(propinaField.getText());
                if (propina < 0) {
                    ValidationHelper.markFieldAsError(propinaField);
                    ValidationHelper.addErrorTooltip(propinaField, "La propina no puede ser negativa");
                } else {
                    ValidationHelper.removeErrorTooltip(propinaField);
                }
            } catch (NumberFormatException e) {
                if (!propinaField.getText().isEmpty()) {
                    ValidationHelper.markFieldAsError(propinaField);
                    ValidationHelper.addErrorTooltip(propinaField, "Debe ser un número válido");
                }
            }
            actualizarTotales();
        }));

        // Validación en tiempo real para monto pagado
        montoPagadoField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
            ValidationHelper.resetFieldBorder(montoPagadoField);
            try {
                double monto = Double.parseDouble(montoPagadoField.getText());
                if (monto < 0) {
                    ValidationHelper.markFieldAsError(montoPagadoField);
                    ValidationHelper.addErrorTooltip(montoPagadoField, "El monto no puede ser negativo");
                } else {
                    ValidationHelper.removeErrorTooltip(montoPagadoField);
                }
            } catch (NumberFormatException e) {
                if (!montoPagadoField.getText().isEmpty()) {
                    ValidationHelper.markFieldAsError(montoPagadoField);
                    ValidationHelper.addErrorTooltip(montoPagadoField, "Debe ser un número válido");
                }
            }
            actualizarTotales();
        }));

        // Validación de cédula en tiempo real
        cedulaNumeroField.getDocument().addDocumentListener(new SimpleDocumentListener(this::validateCedulaInput));
        cedulaTipoComboBox.addActionListener(e -> validateCedulaInput());
    }

    private void cargarDatosDesdeDB() {
        ServiceDAO serviceDAO = new ServiceDAO(); // Mover la instanciación aquí o hacerla variable de instancia

        try {
            serviciosMap = serviceDAO.getAllMap(); // Cargar todos los servicios en el Map
            preciosServicios.clear(); // Limpiar el mapa antiguo (si aún lo usas para algo)

            if (serviciosComboBox != null) {
                serviciosComboBox.removeAllItems();
                // Llenar el ComboBox con los nombres de los servicios del Map
                serviciosMap.keySet().stream().sorted().forEach(serviciosComboBox::addItem);
                 // Seleccionar el primer item si existe
                if (serviciosComboBox.getItemCount() > 0) {
                     serviciosComboBox.setSelectedIndex(0);
                 }
            }
             // Llenar el mapa antiguo si es necesario para compatibilidad (Opcional)
             serviciosMap.forEach((name, service) -> preciosServicios.put(name, service.getPrice_corto()));


        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar servicios: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "Error al cargar servicios desde DB", e);
        }

        // ... (código para cargar trabajadoras se mantiene igual) ...
         String sqlEmployees = "SELECT id, nombres, apellidos FROM trabajadoras ORDER BY nombres";
         try (Connection conn = Database.connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlEmployees)) {
             trabajadorasList.clear();
             trabajadorasNombres.clear();
             if (trabajadorasComboBox != null) {
                 trabajadorasComboBox.removeAllItems();
             }
             if (propinaTrabajadoraComboBox != null) {
                 propinaTrabajadoraComboBox.removeAllItems(); // Limpiar también el de propinas
             }

             List<String> propinaDestinatarios = new ArrayList<>();

             while (rs.next()) {
                 Trabajadora t = new Trabajadora();
                 t.setId(rs.getInt("id"));
                 t.setNombres(rs.getString("nombres"));
                 t.setApellidos(rs.getString("apellidos"));
                 trabajadorasList.add(t);
                 String nombreCompleto = t.getNombreCompleto();
                 trabajadorasNombres.add(nombreCompleto);
                 propinaDestinatarios.add(nombreCompleto); // Añadir a la lista de propinas

                 if (trabajadorasComboBox != null) {
                     trabajadorasComboBox.addItem(nombreCompleto);
                 }
             }

             propinaDestinatarios.add("Salón"); // Añadir "Salón" al final
             if (propinaTrabajadoraComboBox != null) {
                  propinaDestinatarios.forEach(propinaTrabajadoraComboBox::addItem);
              }

         } catch (SQLException e) {
             JOptionPane.showMessageDialog(this, "Error al cargar trabajadoras: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
              LOGGER.log(Level.SEVERE, "Error al cargar trabajadoras desde DB", e);
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
        
        // --- INICIO MODIFICACIÓN CÉDULA ---
        cedulaTipoComboBox = new JComboBox<>(new String[]{"V", "J", "G", "P"});
        cedulaNumeroField = new JTextField(15);
        // Add the numeric filter
        ((javax.swing.text.AbstractDocument) cedulaNumeroField.getDocument()).setDocumentFilter(new NumericFilter());

        JPanel cedulaPanel = new JPanel(new BorderLayout(5, 0));
        cedulaPanel.add(cedulaTipoComboBox, BorderLayout.WEST);
        cedulaPanel.add(cedulaNumeroField, BorderLayout.CENTER);

        gbcCliente.gridx = 1;
        gbcCliente.gridy = 0;
        gbcCliente.gridwidth = 2;
        gbcCliente.weightx = 1.0;
        clientePanel.add(cedulaPanel, gbcCliente);
        
        cedulaNumeroField.addActionListener(e -> buscarClienteEnDB());
        // --- FIN MODIFICACIÓN CÉDULA ---


        tasaLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 4;
        gbcCliente.gridwidth = 2;
        gbcCliente.anchor = GridBagConstraints.CENTER;
        clientePanel.add(tasaLabel, gbcCliente);

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(clientePanel, gbc);

        gbcCliente.gridx = 1;
        gbcCliente.gridy = 0;
        gbcCliente.gridwidth = 2;
        gbcCliente.weightx = 1.0;
        // clientePanel.add(cedulaField, gbcCliente); // Reemplazado por cedulaPanel

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

        JButton gestionarServiciosBtn = new JButton("Gestionar Servicios");
        gbcCliente.gridx = 2;
        gbcCliente.gridy = 2;
        gbcCliente.gridwidth = 1;
        clientePanel.add(gestionarServiciosBtn, gbcCliente);

        gestionarServiciosBtn.addActionListener(e -> {
            ServiceManagementWindow serviceWindow = new ServiceManagementWindow();
            serviceWindow.setVisible(true);
            serviceWindow.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                    cargarDatosDesdeDB();
                }
            });
        });

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
            frame.setLocationRelativeTo(null);
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

        // --- NUEVO: CheckBox Cliente Trae Producto ---
        clienteProductoCheck = new JCheckBox("Cliente trae producto");
        clienteProductoCheck.setVisible(false); // Oculto por defecto
        gbcServicios.gridx = 0;
        gbcServicios.gridy = 3; // Nueva fila para el checkbox
        gbcServicios.gridwidth = 2; // Ocupa ambas columnas
        gbcServicios.anchor = GridBagConstraints.WEST; // Alinear a la izquierda
        serviciosPanel.add(clienteProductoCheck, gbcServicios);
        // --- FIN NUEVO ---

        JButton agregarBtn = new JButton("Agregar Servicio");
        gbcServicios.gridx = 0;
        gbcServicios.gridy = 4; // Mover botones una fila abajo
        gbcServicios.gridwidth = 1;
        gbcServicios.weightx = 0.5;
        gbcServicios.anchor = GridBagConstraints.CENTER; // Resetear anchor
        serviciosPanel.add(agregarBtn, gbcServicios);
        agregarBtn.addActionListener(e -> agregarServicio());

        JButton eliminarBtn = new JButton("Eliminar Servicio");
        gbcServicios.gridx = 1;
        gbcServicios.gridy = 4; // Mover botones una fila abajo
        gbcServicios.gridwidth = 1;
        gbcServicios.weightx = 0.5;
        serviciosPanel.add(eliminarBtn, gbcServicios);
        eliminarBtn.addActionListener(e -> eliminarServicioSeleccionado());

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(serviciosPanel, gbc);

        // --- NUEVO: Listener para ComboBox de Servicios ---
        serviciosComboBox.addActionListener(e -> {
            String selectedServiceName = (String) serviciosComboBox.getSelectedItem();
            if (selectedServiceName != null) {
                Service selectedService = serviciosMap.get(selectedServiceName);
                if (selectedService != null && selectedService.isPermiteClienteProducto()) {
                    clienteProductoCheck.setVisible(true); // Mostrar checkbox
                } else {
                    clienteProductoCheck.setVisible(false); // Ocultar checkbox
                    clienteProductoCheck.setSelected(false); // Desmarcar si se oculta
                }
            } else {
                clienteProductoCheck.setVisible(false);
                clienteProductoCheck.setSelected(false);
            }
        });
        // --- FIN NUEVO LISTENER ---

        // --- NUEVO: Listener para el CheckBox ---
        clienteProductoCheck.addActionListener(e -> {
            // Podrías agregar lógica aquí si necesitaras recalcular algo
            // inmediatamente al marcar/desmarcar, pero no es estrictamente
            // necesario ya que el precio se calcula al agregar.
            LOGGER.fine("Checkbox 'Cliente trae producto' cambiado a: " + clienteProductoCheck.isSelected());
        });
        // --- FIN NUEVO LISTENER CHECKBOX ---

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

    private void validateCedulaInput() {
        String tipoCi = (String) cedulaTipoComboBox.getSelectedItem();
        String numeroCi = cedulaNumeroField.getText().trim();

        if (!numeroCi.isEmpty()) {
            String cedula = tipoCi + "-" + numeroCi;
            if (CommonValidators.isValidCedula(cedula)) {
                ValidationHelper.resetFieldBorder(cedulaNumeroField);
                ValidationHelper.removeErrorTooltip(cedulaNumeroField);
            } else {
                ValidationHelper.markFieldAsWarning(cedulaNumeroField);
                ValidationHelper.addErrorTooltip(cedulaNumeroField,
                        "Cédula incompleta o inválida (ej: 6-9 dígitos)");
            }
        } else {
            ValidationHelper.resetFieldBorder(cedulaNumeroField);
            ValidationHelper.removeErrorTooltip(cedulaNumeroField);
        }
    }

    private void buscarClienteEnDB() {
        String tipoCi = (String) cedulaTipoComboBox.getSelectedItem();
        String numeroCi = cedulaNumeroField.getText().trim();
        
        // Si el número está vacío, no hacer nada.
        if (numeroCi.isEmpty()) {
            nombreClienteLabel.setText("Nombre: No cargado");
            clienteActual = null;
            return;
        }

        String cedula = tipoCi + "-" + numeroCi;

        ValidationResult result = ClienteValidator.validateCedula(cedula);

        if (!result.isValid()) {
            ValidationHelper.showErrors(this, result);
            ValidationHelper.markFieldAsError(cedulaNumeroField);
            return;
        }

        ValidationHelper.resetFieldBorder(cedulaNumeroField);

        // MODIFICADO: Se añade hair_type a la consulta
        String sql = "SELECT client_id, full_name, hair_type FROM clients WHERE cedula = ?";
        try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cedula);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // MODIFICADO: Se crea el objeto Cliente con el nuevo dato
                clienteActual = new Cliente(
                        rs.getInt("client_id"),
                        cedula,
                        rs.getString("full_name"),
                        rs.getString("hair_type") // <-- Se obtiene el dato
                );

                // MODIFICADO: Se muestra el tipo de cabello para confirmación
                String hairTypeInfo = clienteActual.getHairType();
                String infoCliente = "Nombre: " + clienteActual.getNombre()
                        + " (Cabello: " + (hairTypeInfo != null && !hairTypeInfo.isEmpty() ? hairTypeInfo : "No definido") + ")";
                nombreClienteLabel.setText(infoCliente);

                LOGGER.info("Cliente cargado: " + clienteActual.getNombre());
                JOptionPane.showMessageDialog(this,
                        "Cliente cargado: " + clienteActual.getNombre(),
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                clienteActual = null;
                nombreClienteLabel.setText("Nombre: No encontrado");

                int response = JOptionPane.showConfirmDialog(this,
                        "Cliente no encontrado. ¿Desea registrar un nuevo cliente?",
                        "Cliente No Encontrado",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                if (response == JOptionPane.YES_OPTION) {
                    ClientManagementWindow clientWindow = new ClientManagementWindow(cedula);
                    clientWindow.setVisible(true);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al buscar cliente", ex);
            JOptionPane.showMessageDialog(this,
                    "Error al buscar cliente: " + ex.getMessage(),
                    "Error DB",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void agregarServicio() {
        String nombreServicio = (String) serviciosComboBox.getSelectedItem();
        String trabajadora = (String) trabajadorasComboBox.getSelectedItem();
        boolean clienteTraeProducto = clienteProductoCheck.isSelected() && clienteProductoCheck.isVisible();
    
        if (nombreServicio == null) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un servicio.", "Servicio no seleccionado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (trabajadora == null) {
             JOptionPane.showMessageDialog(this, "Por favor, seleccione una trabajadora.", "Trabajadora no seleccionada", JOptionPane.WARNING_MESSAGE);
             return;
        }
    
        // 1. Obtener el objeto Service completo desde el Map
        Service servicioCompleto = serviciosMap.get(nombreServicio);
        if (servicioCompleto == null) {
            JOptionPane.showMessageDialog(this, "Error: no se pudo cargar la información del servicio.", "Error Interno", JOptionPane.ERROR_MESSAGE);
            LOGGER.severe("Servicio seleccionado '" + nombreServicio + "' no encontrado en serviciosMap.");
            return;
        }
    
        double precioFinal;
    
        // 2. Determinar precio base según si el cliente trae producto
        if (clienteTraeProducto && servicioCompleto.isPermiteClienteProducto()) {
            precioFinal = servicioCompleto.getPriceClienteProducto();
            LOGGER.fine("Usando precio con producto del cliente para " + nombreServicio + ": $" + precioFinal);
        } else {
            // 3. Si no trae producto (o el servicio no lo permite), usar la lógica de tipo de cabello/extensiones
            precioFinal = servicioCompleto.getPrice_corto(); // Precio por defecto
    
            // Lógica para seleccionar el precio basado en el tipo de cabello del cliente (si existe cliente)
            if (clienteActual != null && clienteActual.getHairType() != null && !clienteActual.getHairType().isEmpty()) {
                String tipoCabello = clienteActual.getHairType();
                LOGGER.fine("Cliente tiene tipo de cabello: " + tipoCabello);
                switch (tipoCabello) {
                    case "Mediano":
                        // Si el precio para mediano es mayor a 0, úsalo. Si no, usa el de corto.
                        if (servicioCompleto.getPrice_medio() > 0) {
                            precioFinal = servicioCompleto.getPrice_medio();
                            LOGGER.fine("Aplicando precio 'Mediano': $" + precioFinal);
                        } else {
                             LOGGER.fine("Servicio no tiene precio 'Mediano', usando 'Corto': $" + precioFinal);
                        }
                        break;
                    case "Largo":
                        if (servicioCompleto.getPrice_largo() > 0) {
                            precioFinal = servicioCompleto.getPrice_largo();
                            LOGGER.fine("Aplicando precio 'Largo': $" + precioFinal);
                        } else {
                             LOGGER.fine("Servicio no tiene precio 'Largo', usando 'Corto': $" + precioFinal);
                        }
                        break;
                    default: // Incluye "Corto" o no definido
                         LOGGER.fine("Aplicando precio 'Corto' por defecto: $" + precioFinal);
                        break;
                }
            } else {
                LOGGER.fine("No hay cliente cargado o no tiene tipo de cabello, usando precio 'Corto': $" + precioFinal);
            }
    
            // Lógica para extensiones (si se detecta el servicio específico Y tiene precio > 0)
            // Ajusta la condición si el nombre exacto es diferente
            boolean esServicioExtensiones = nombreServicio.toLowerCase().contains("exten"); // Más flexible
            if (esServicioExtensiones && servicioCompleto.getPrice_ext() > 0) {
                precioFinal = servicioCompleto.getPrice_ext();
                LOGGER.fine("Aplicando precio 'Extensiones': $" + precioFinal);
            }
        }
    
    
        // 4. Modificar el nombre del servicio si el cliente trae producto para claridad en la tabla/factura
        String nombreEnTabla = nombreServicio;
        if (clienteTraeProducto) {
            nombreEnTabla += " (Cliente)"; // Añadir indicativo
        }
    
    
        // 5. Agregar a la tabla y lista interna con el precio final calculado
        serviciosAgregados.add(new VentaServicio(nombreEnTabla, trabajadora, precioFinal)); // Guardar nombre modificado
        tableModel.addRow(new Object[]{nombreEnTabla, trabajadora, new DecimalFormat("#,##0.00").format(precioFinal)}); // Mostrar nombre modificado
        LOGGER.info("Servicio agregado a la venta: " + nombreEnTabla + ", Trabajadora: " + trabajadora + ", Precio: $" + precioFinal);
    
        actualizarTotales();
    
        // Resetear CheckBox después de agregar
        clienteProductoCheck.setSelected(false);
        // Opcional: Ocultar si el siguiente servicio seleccionado no lo permite
        String nextSelectedService = (String) serviciosComboBox.getSelectedItem();
         if (nextSelectedService != null) {
             Service nextService = serviciosMap.get(nextSelectedService);
             if (nextService != null && !nextService.isPermiteClienteProducto()) {
                 clienteProductoCheck.setVisible(false);
             }
         } else {
             clienteProductoCheck.setVisible(false);
         }
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
        gbcPago.gridy = 4;
        gbcPago.weightx = 1.0;
        pagoPanel.add(vueltoLabel, gbcPago);

        ActionListener updateListener = e -> actualizarTotales();
        descuentoComboBox.addActionListener(updateListener);
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
        LOGGER.info("Iniciando validación de venta...");

        List<VentaValidator.ServicioVenta> serviciosParaValidar = new ArrayList<>();
        for (VentaServicio vs : serviciosAgregados) {
            serviciosParaValidar.add(new VentaValidator.ServicioVenta(
                    vs.getServicio(),
                    vs.getTrabajadora(),
                    vs.getPrecio()
            ));
        }

        double subtotal = serviciosAgregados.stream().mapToDouble(VentaServicio::getPrecio).sum();

        double propina = 0.0;
        try {
            propina = Double.parseDouble(propinaField.getText());
        } catch (NumberFormatException e) {
            // Se maneja en la validación
        }

        String tipoDesc = Objects.requireNonNull(descuentoComboBox.getSelectedItem()).toString();
        double descuento = tipoDesc.equals("Promoción") ? subtotal * AppConfig.getPromoDiscountPercentage() : 0.0;

        double total = subtotal - descuento + propina;

        double montoPagado = 0.0;
        try {
            montoPagado = Double.parseDouble(montoPagadoField.getText());
        } catch (NumberFormatException e) {
            // Se maneja en la validación
        }

        String metodoPago = pagoComboBox.getSelectedItem() != null
                ? pagoComboBox.getSelectedItem().toString()
                : "";

        String moneda = monedaDolar.isSelected() ? "$" : "Bs";

        // --- INICIO CORRECCIÓN DE ERROR ---
        // El total (calculado desde subtotal, descuento y propina) SIEMPRE está en Dólares.
        double totalEnDolares = total;
        double montoPagadoEnDolares = montoPagado; // Asumir que el monto pagado está en $

        if (moneda.equals("Bs")) {
            // Si la moneda es "Bs", SÓLO el monto pagado debe convertirse a Dólares
            // para la validación de pago. El 'totalEnDolares' ya es correcto.
            montoPagadoEnDolares = montoPagado / tasaBcv;
        }
        // --- FIN CORRECCIÓN DE ERROR ---


        ValidationResult result = VentaValidator.validateVenta(
                serviciosParaValidar,
                subtotal,
                descuento,
                propina,
                totalEnDolares, // <- Este es el total en $ (Esperado: 25.00)
                montoPagadoEnDolares, // <- Este es el monto pagado en $ (Actual: 25.00)
                metodoPago,
                tipoDesc
        );

        String destinoPagoMovil = null;
        if ("Pago Movil".equals(metodoPago)) {
            destinoPagoMovil = pagoMovilRosaRadio.isSelected() ? "Rosa" : "Capelli";
        }

        ValidationResult pagoResult = VentaValidator.validateMetodoPago(
                metodoPago, moneda, destinoPagoMovil
        );
        result.merge(pagoResult);

        if (propina > 0) {
            String destinatarioPropina = propinaTrabajadoraComboBox.getSelectedItem() != null
                    ? propinaTrabajadoraComboBox.getSelectedItem().toString()
                    : "";
            ValidationResult propinaResult = VentaValidator.validatePropina(
                    propina, destinatarioPropina
            );
            result.merge(propinaResult);
        }

        ValidationResult descuentoResult = VentaValidator.validateDescuento(
                tipoDesc, descuento, subtotal
        );
        result.merge(descuentoResult);

        ValidationResult duplicadosResult = VentaValidator.checkDuplicateServices(
                serviciosParaValidar
        );
        result.merge(duplicadosResult);

        if (!ValidationHelper.validateAndShow(this, result, "Validación de Venta")) {
            LOGGER.warning("Validación de venta falló o fue cancelada por el usuario");
            return;
        }

        LOGGER.info("Validación de venta exitosa, procediendo a guardar...");

        Connection conn = null;
        long saleId = -1;

        try {
            conn = Database.connect();
            conn.setAutoCommit(false);

            String sqlSale = "INSERT INTO sales (client_id, subtotal, discount_type, "
                    + "discount_amount, total, payment_method, currency, payment_destination) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            // --- INICIO CORRECCIÓN SQLITE ---
            // 1. Preparar el PreparedStatement SIN pedir generated keys
            try (PreparedStatement pstmt = conn.prepareStatement(sqlSale)) {
                if (clienteActual != null) {
                    pstmt.setInt(1, clienteActual.getId());
                } else {
                    pstmt.setNull(1, java.sql.Types.INTEGER);
                }

                pstmt.setDouble(2, subtotal);
                pstmt.setString(3, tipoDesc);
                pstmt.setDouble(4, descuento);
                pstmt.setDouble(5, totalEnDolares);

                pstmt.setString(6, metodoPago);
                pstmt.setString(7, moneda);

                if (destinoPagoMovil != null) {
                    pstmt.setString(8, destinoPagoMovil);
                } else {
                    pstmt.setNull(8, java.sql.Types.VARCHAR);
                }

                pstmt.executeUpdate();
            }

            // 2. Obtener el ID con una consulta separada
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    saleId = rs.getLong(1);
                    LOGGER.info("Venta insertada con ID: " + saleId);
                } else {
                    throw new SQLException("Error al obtener el ID de la venta generada (last_insert_rowid falló)");
                }
            }
            // --- FIN CORRECCIÓN SQLITE ---

            String sqlItems = "INSERT INTO sale_items (sale_id, service_id, employee_id, price_at_sale) "
                    + "VALUES (?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sqlItems)) {
                for (VentaServicio vs : serviciosAgregados) {
                    int serviceId = getServiceId(vs.getServicio(), conn);
                    int employeeId = getEmployeeIdByName(vs.getTrabajadora(), conn);

                    pstmt.setLong(1, saleId);
                    pstmt.setInt(2, serviceId);
                    pstmt.setInt(3, employeeId);
                    pstmt.setDouble(4, vs.getPrecio());

                    pstmt.addBatch();
                }

                int[] batchResults = pstmt.executeBatch();
                LOGGER.info("Items de venta insertados: " + batchResults.length);
            }

            if (propina > 0) {
                String sqlTip = "INSERT INTO tips (sale_id, recipient_name, amount) VALUES (?, ?, ?)";

                try (PreparedStatement pstmt = conn.prepareStatement(sqlTip)) {
                    String destinatarioPropina = propinaTrabajadoraComboBox.getSelectedItem() != null
                            ? propinaTrabajadoraComboBox.getSelectedItem().toString()
                            : "Salón";

                    pstmt.setLong(1, saleId);
                    pstmt.setString(2, destinatarioPropina);
                    pstmt.setDouble(3, propina);

                    pstmt.executeUpdate();
                    LOGGER.info("Propina insertada: $" + propina + " para " + destinatarioPropina);
                }
            }
            conn.commit();

            LOGGER.info("Venta registrada exitosamente. ID: " + saleId);

            String mensajeExito = construirMensajeExito(saleId, totalEnDolares, moneda,
                    montoPagadoEnDolares, tasaBcv);
            JOptionPane.showMessageDialog(this,
                    mensajeExito,
                    "✅ Factura Generada Exitosamente",
                    JOptionPane.INFORMATION_MESSAGE);
            limpiarVentana();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al registrar la venta", e);
            try {
                if (conn != null) {
                    conn.rollback();
                    LOGGER.info("Rollback ejecutado exitosamente");
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error al hacer rollback", ex);
            }
            String mensajeError = "Error al registrar la venta en la base de datos:\n\n"
                    + e.getMessage() + "\n\n"
                    + "Por favor, verifique los datos e intente nuevamente.";

            JOptionPane.showMessageDialog(this,
                    mensajeError,
                    "❌ Error de Transacción",
                    JOptionPane.ERROR_MESSAGE);

        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                    LOGGER.fine("Conexión a BD cerrada");
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error al cerrar conexión", ex);
            }
        }
    }

    private int getServiceId(String serviceName, Connection conn) throws SQLException {
        // Ajuste: si el nombre del servicio fue modificado (ej. "Corte (Cliente)"),
        // debemos buscar el nombre original antes de consultar la BD.
        String originalServiceName = serviceName.replace(" (Cliente)", "").trim();
        
        String sql = "SELECT service_id FROM services WHERE name = ?";
    
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, originalServiceName); // Usar el nombre original
            ResultSet rs = pstmt.executeQuery();
    
            if (rs.next()) {
                int serviceId = rs.getInt("service_id");
                LOGGER.fine("Service ID encontrado: " + serviceId + " para servicio: " + originalServiceName);
                return serviceId;
            } else {
                LOGGER.severe("Servicio no encontrado en BD: " + originalServiceName);
                throw new SQLException("Servicio no encontrado en la base de datos: " + originalServiceName);
            }
        }
    }

    // =========================================================================
    // --- MÉTODO CORREGIDO ---
    // =========================================================================
    
    /**
     * Obtiene el ID de la trabajadora basado en el nombre completo.
     * CORREGIDO: Busca en la lista 'trabajadorasList' en lugar de dividir el string.
     */
    private int getEmployeeIdByName(String nombreCompleto, Connection conn) throws SQLException {
        // Iterar sobre la lista de trabajadoras cargada en memoria
        for (Trabajadora t : trabajadorasList) {
            if (t.getNombreCompleto().equals(nombreCompleto)) {
                return t.getId(); // Encontrado
            }
        }
        
        // Si no se encuentra en la lista (esto no debería pasar si la lista está sincronizada)
        // Lanza el error que viste
        throw new SQLException("Trabajadora no encontrada: " + nombreCompleto 
                + " (Error: no se encontró en la lista 'trabajadorasList' de la aplicación)");
    }
    // =========================================================================
    // --- FIN DE LA CORRECCIÓN ---
    // =========================================================================

    private String construirMensajeExito(long saleId, double total, String moneda,
            double montoPagado, double tasaBcv) {
        DecimalFormat df = new DecimalFormat("#,##0.00");

        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Venta registrada exitosamente\n\n");
        mensaje.append("═══════════════════════════════════\n");
        mensaje.append("ID de Venta: ").append(saleId).append("\n");
        mensaje.append("───────────────────────────────────\n");

        if (moneda.equals("$")) {
            mensaje.append("Total: $").append(df.format(total)).append("\n");
            mensaje.append("Pagado: $").append(df.format(montoPagado)).append("\n");

            double vuelto = montoPagado - total;
            if (vuelto > 0) {
                mensaje.append("Vuelto: $").append(df.format(vuelto)).append("\n");
            }
        } else {
            mensaje.append("Total: Bs ").append(df.format(total * tasaBcv)).append("\n");
            mensaje.append("Pagado: Bs ").append(df.format(montoPagado * tasaBcv)).append("\n");

            double vuelto = (montoPagado * tasaBcv) - (total * tasaBcv);
            if (vuelto > 0) {
                mensaje.append("Vuelto: Bs ").append(df.format(vuelto)).append("\n");
            }

            mensaje.append("\n(Equivalente en $: ").append(df.format(total)).append(")\n");
        }

        mensaje.append("═══════════════════════════════════\n");
        mensaje.append("\n¡Gracias por usar el sistema Capelli!");

        return mensaje.toString();
    }

    // MÉTODO AUXILIAR NUEVO Y OPTIMIZADO
    private Service obtenerServicioPorNombre(String nombre) {
        // Ahora consulta el Map en memoria, que se carga al inicio.
        // Es mucho más rápido y no golpea la BD.
        Service service = serviciosMap.get(nombre);
        if (service == null) {
            LOGGER.log(Level.SEVERE, "Servicio no encontrado en el Map: " + nombre);
            JOptionPane.showMessageDialog(this, "Error: Servicio '" + nombre + "' no encontrado en la caché local.", "Error Interno", JOptionPane.ERROR_MESSAGE);
        }
        return service;
    }

    private void limpiarVentana() {
        clienteActual = null;
        cedulaTipoComboBox.setSelectedItem("V");
        cedulaNumeroField.setText("");
        nombreClienteLabel.setText("Nombre: No cargado");
        serviciosAgregados.clear();
        tableModel.setRowCount(0);
        propinaField.setText("0.00");
        montoPagadoField.setText("0.00");
        descuentoComboBox.setSelectedIndex(0);
        actualizarTotales();
    }

    // CLASE INTERNA MODIFICADA
    private static class Cliente {

        private final int id;
        private final String cedula;
        private final String nombre;
        private final String hairType; // <-- NUEVO CAMPO

        public Cliente(int id, String cedula, String nombre, String hairType) {
            this.id = id;
            this.cedula = cedula;
            this.nombre = nombre;
            this.hairType = hairType;
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

        public String getHairType() {
            return hairType;
        } // <-- NUEVO GETTER
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
        AppConfig.printConfiguration();

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
    
    /**
     * Filtro para permitir solo entradas numéricas en un JTextField.
     * Copiado de TrabajadoraDialog.
     */
    private static class NumericFilter extends javax.swing.text.DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, javax.swing.text.AttributeSet attr) throws javax.swing.text.BadLocationException {
            if (string.matches("[0-9]+")) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, javax.swing.text.AttributeSet attrs) throws javax.swing.text.BadLocationException {
            if (text.matches("[0-9]+")) {
                super.replace(fb, offset, length, text, attrs);
            }
        }
    }
}