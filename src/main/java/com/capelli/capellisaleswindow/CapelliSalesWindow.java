package com.capelli.capellisaleswindow;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CapelliSalesWindow extends JFrame {

    private boolean isDarkMode = true;

    // Modelos y componentes
    private final DefaultTableModel tableModel;
    private JTable serviciosTable;
    
    // Componentes de cliente
    private JTextField cedulaField;
    private JLabel nombreClienteLabel;
    private JLabel tasaLabel;
    
    // Componentes de servicios
    private JComboBox<String> serviciosComboBox;
    private JComboBox<String> trabajadorasComboBox;
    
    // Componentes de descuentos y propinas
    private JComboBox<String> descuentoComboBox;
    private JTextField propinaField;
    private JComboBox<String> propinaTrabajadoraComboBox;
    
    // Componentes de pago
    private JTextField montoPagadoField;
    private JLabel vueltoLabel;
    private JRadioButton monedaBs, monedaDolar;
    private JComboBox<String> pagoComboBox;
    private JLabel totalLabel;
    
    // Panel de pago móvil
    private JRadioButton pagoMovilCapelliRadio;
    private JRadioButton pagoMovilRosaRadio;
    private ButtonGroup pagoMovilDestinoGroup;
    private JPanel pagoMovilPanel;

    // Datos de ejemplo para la UI
    private final List<String> tiposDescuento = new ArrayList<>(Arrays.asList(
        "Ninguno", "Promoción", "Cortesía"
    ));
    
    private final List<String> metodosPago = new ArrayList<>(Arrays.asList(
        "Efectivo", "Tarjeta", "Pago Movil", "Zelle", "Binance"
    ));
    
    private final List<String> serviciosEjemplo = new ArrayList<>(Arrays.asList(
        "Corte", "Tinte", "Mechas", "Keratina", "Brushing", "Manicure", "Pedicure"
    ));
    
    private final List<String> trabajadorasEjemplo = new ArrayList<>(Arrays.asList(
        "María González", "Ana Rodríguez", "Carmen López", "Rosa Martínez"
    ));

    public CapelliSalesWindow() {
        super("Capelli - Sistema de Ventas");

        // Configurar modelo de tabla
        tableModel = new DefaultTableModel(
            new String[]{"Servicio", "Trabajador(a)", "Precio ($)"}, 
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2; // Solo precio es editable
            }
        };

        // Configuración de la ventana
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 800);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Crear panel principal
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        // Panel izquierdo (Información de venta)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        mainPanel.add(crearPanelIzquierdo(), gbc);

        // Panel superior derecho (Tabla de servicios)
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        mainPanel.add(crearPanelTabla(), gbc);

        // Panel inferior derecho (Total y pago)
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        mainPanel.add(crearPanelDerechoInferior(), gbc);

        add(mainPanel);
    }

    private JPanel crearPanelIzquierdo() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Información de Venta"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ===== SECCIÓN: DATOS DE CLIENTE =====
        JPanel clientePanel = new JPanel(new GridBagLayout());
        clientePanel.setBorder(new TitledBorder("Datos de la Cliente"));
        GridBagConstraints gbcCliente = new GridBagConstraints();
        gbcCliente.insets = new Insets(5, 5, 5, 5);
        gbcCliente.fill = GridBagConstraints.HORIZONTAL;

        // Campo de cédula
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 0;
        clientePanel.add(new JLabel("Cédula:"), gbcCliente);
        
        cedulaField = new JTextField(15);
        gbcCliente.gridx = 1;
        gbcCliente.gridy = 0;
        gbcCliente.gridwidth = 2;
        gbcCliente.weightx = 1.0;
        clientePanel.add(cedulaField, gbcCliente);

        // Botones de búsqueda y gestión
        JButton buscarClienteBtn = new JButton("Buscar Cliente");
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 1;
        gbcCliente.gridwidth = 1;
        gbcCliente.weightx = 0.5;
        clientePanel.add(buscarClienteBtn, gbcCliente);

        JButton gestionarClientesBtn = new JButton("Gestionar Clientes");
        gbcCliente.gridx = 1;
        gbcCliente.gridy = 1;
        gbcCliente.gridwidth = 1;
        gbcCliente.weightx = 0.5;
        clientePanel.add(gestionarClientesBtn, gbcCliente);

        JButton gestionarTrabajadorasBtn = new JButton("Gestionar Trabajadoras");
        gbcCliente.gridx = 2;
        gbcCliente.gridy = 1;
        gbcCliente.gridwidth = 1;
        gbcCliente.weightx = 0.5;
        clientePanel.add(gestionarTrabajadorasBtn, gbcCliente);

        // Nombre del cliente
        nombreClienteLabel = new JLabel("Nombre: No cargado");
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 2;
        gbcCliente.gridwidth = 2;
        clientePanel.add(nombreClienteLabel, gbcCliente);

        // Botón gestionar servicios
        JButton gestionarServiciosBtn = new JButton("Gestionar Servicios");
        gbcCliente.gridx = 2;
        gbcCliente.gridy = 2;
        gbcCliente.gridwidth = 1;
        clientePanel.add(gestionarServiciosBtn, gbcCliente);

        // Botón ver reportes
        JButton verReportesBtn = new JButton("Ver Reportes de Ventas");
        verReportesBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 3;
        gbcCliente.gridwidth = 2;
        gbcCliente.insets = new Insets(15, 5, 5, 5);
        clientePanel.add(verReportesBtn, gbcCliente);

        // Tasa BCV
        tasaLabel = new JLabel("Tasa BCV: Cargando...");
        tasaLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 4;
        gbcCliente.gridwidth = 2;
        gbcCliente.anchor = GridBagConstraints.CENTER;
        clientePanel.add(tasaLabel, gbcCliente);

        // Botón reporte diario
        JButton verReporteDiarioBtn = new JButton("Ver Reporte del Día");
        gbcCliente.gridx = 0;
        gbcCliente.gridy = 5;
        gbcCliente.gridwidth = 2;
        gbcCliente.anchor = GridBagConstraints.CENTER;
        clientePanel.add(verReporteDiarioBtn, gbcCliente);

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(clientePanel, gbc);

        // ===== SECCIÓN: SERVICIOS =====
        JPanel serviciosPanel = new JPanel(new GridBagLayout());
        serviciosPanel.setBorder(new TitledBorder("Servicios"));
        GridBagConstraints gbcServicios = new GridBagConstraints();
        gbcServicios.insets = new Insets(5, 5, 5, 5);
        gbcServicios.fill = GridBagConstraints.HORIZONTAL;

        // ComboBox de servicios
        gbcServicios.gridx = 0;
        gbcServicios.gridy = 0;
        serviciosPanel.add(new JLabel("Servicio:"), gbcServicios);
        
        serviciosComboBox = new JComboBox<>(serviciosEjemplo.toArray(new String[0]));
        gbcServicios.gridx = 1;
        gbcServicios.gridy = 0;
        gbcServicios.weightx = 1.0;
        serviciosPanel.add(serviciosComboBox, gbcServicios);

        // ComboBox de trabajadoras
        gbcServicios.gridx = 0;
        gbcServicios.gridy = 1;
        serviciosPanel.add(new JLabel("Trabajador(a):"), gbcServicios);
        
        trabajadorasComboBox = new JComboBox<>(trabajadorasEjemplo.toArray(new String[0]));
        gbcServicios.gridx = 1;
        gbcServicios.gridy = 1;
        gbcServicios.weightx = 1.0;
        serviciosPanel.add(trabajadorasComboBox, gbcServicios);

        // Botones de acción
        JButton agregarBtn = new JButton("Agregar Servicio");
        gbcServicios.gridx = 0;
        gbcServicios.gridy = 2;
        gbcServicios.gridwidth = 1;
        gbcServicios.weightx = 0.5;
        serviciosPanel.add(agregarBtn, gbcServicios);

        JButton eliminarBtn = new JButton("Eliminar Servicio");
        gbcServicios.gridx = 1;
        gbcServicios.gridy = 2;
        gbcServicios.gridwidth = 1;
        gbcServicios.weightx = 0.5;
        serviciosPanel.add(eliminarBtn, gbcServicios);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(serviciosPanel, gbc);

        // ===== SECCIÓN: TOGGLE DE TEMA =====
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

    private JPanel crearPanelTabla() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Servicios a Facturar"));

        serviciosTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(serviciosTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelDerechoInferior() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Total y Pago"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ===== SECCIÓN: DESCUENTO =====
        JPanel descuentoPanel = new JPanel(new BorderLayout());
        descuentoPanel.setBorder(new TitledBorder("Descuento"));
        descuentoComboBox = new JComboBox<>(tiposDescuento.toArray(new String[0]));
        descuentoPanel.add(descuentoComboBox, BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(descuentoPanel, gbc);

        // ===== SECCIÓN: PROPINA =====
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
        
        List<String> propinaDestinatarios = new ArrayList<>(trabajadorasEjemplo);
        propinaDestinatarios.add("Salón");
        propinaTrabajadoraComboBox = new JComboBox<>(propinaDestinatarios.toArray(new String[0]));
        gbcPropina.gridx = 1;
        gbcPropina.gridy = 1;
        propinaPanel.add(propinaTrabajadoraComboBox, gbcPropina);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(propinaPanel, gbc);

        // ===== SECCIÓN: RESUMEN DE TOTALES =====
        JPanel totalesPanel = new JPanel(new GridLayout(4, 2));
        totalesPanel.setBorder(new TitledBorder("Resumen"));
        
        JLabel subtotalLabel = new JLabel("Subtotal ($): 0.00");
        JLabel descuentoLabel = new JLabel("Descuento ($): 0.00");
        JLabel propinaLabelGUI = new JLabel("Propina ($): 0.00");
        totalLabel = new JLabel("Total ($): 0.00");
        
        totalesPanel.add(subtotalLabel);
        totalesPanel.add(descuentoLabel);
        totalesPanel.add(propinaLabelGUI);
        totalesPanel.add(totalLabel);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(totalesPanel, gbc);

        // ===== SECCIÓN: OPCIONES DE PAGO =====
        JPanel pagoPanel = new JPanel(new GridBagLayout());
        pagoPanel.setBorder(new TitledBorder("Opciones de Pago"));
        GridBagConstraints gbcPago = new GridBagConstraints();
        gbcPago.insets = new Insets(5, 5, 5, 5);
        gbcPago.fill = GridBagConstraints.HORIZONTAL;

        // Selección de moneda
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

        // Método de pago
        gbcPago.gridx = 0;
        gbcPago.gridy = 1;
        pagoPanel.add(new JLabel("Método:"), gbcPago);
        
        pagoComboBox = new JComboBox<>(metodosPago.toArray(new String[0]));
        gbcPago.gridx = 1;
        gbcPago.gridy = 1;
        gbcPago.weightx = 1.0;
        pagoPanel.add(pagoComboBox, gbcPago);

        // Panel de Pago Móvil (inicialmente oculto)
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

        // Listener para mostrar/ocultar panel de pago móvil
        pagoComboBox.addActionListener(e -> {
            String selectedMethod = (String) pagoComboBox.getSelectedItem();
            pagoMovilPanel.setVisible("Pago Movil".equals(selectedMethod));
        });

        // Monto pagado
        gbcPago.gridx = 0;
        gbcPago.gridy = 3;
        pagoPanel.add(new JLabel("Monto Pagado:"), gbcPago);
        
        montoPagadoField = new JTextField("0.00");
        gbcPago.gridx = 1;
        gbcPago.gridy = 3;
        gbcPago.weightx = 1.0;
        pagoPanel.add(montoPagadoField, gbcPago);

        // Vuelto
        gbcPago.gridx = 0;
        gbcPago.gridy = 4;
        pagoPanel.add(new JLabel("Vuelto:"), gbcPago);
        
        vueltoLabel = new JLabel("0.00");
        gbcPago.gridx = 1;
        gbcPago.gridy = 4;
        gbcPago.weightx = 1.0;
        pagoPanel.add(vueltoLabel, gbcPago);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(pagoPanel, gbc);

        // ===== BOTÓN PRINCIPAL: GENERAR FACTURA =====
        JButton facturarBtn = new JButton("Generar Factura");
        facturarBtn.setFont(new Font("Arial", Font.BOLD, 16));
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(facturarBtn, gbc);

        return panel;
    }

    public static void main(String[] args) {
        // Aplicar tema oscuro por defecto
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            CapelliSalesWindow window = new CapelliSalesWindow();
            window.setVisible(true);
        });
    }
}