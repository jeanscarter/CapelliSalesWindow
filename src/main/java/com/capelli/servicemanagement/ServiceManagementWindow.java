package com.capelli.servicemanagement;

import com.capelli.database.ServiceDAO;
import com.capelli.model.Service;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;
import net.miginfocom.swing.MigLayout;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

public class ServiceManagementWindow extends JFrame {

    private final ServiceDAO serviceDAO;
    private final DefaultTableModel tableModel;
    private final JTable serviceTable;
    private final JTextField nameField;
    // Campos para precios existentes
    private final JTextField priceCortoField;
    private final JTextField priceMedioField;
    private final JTextField priceLargoField;
    private final JTextField priceExtField;
    // NUEVOS CAMPOS
    private final JCheckBox permiteClienteCheck; // NUEVO CheckBox
    private final JTextField priceClienteProductoField; // NUEVO Campo de precio
    
    private Service currentService = null;

    public ServiceManagementWindow() {
        super("Gestión de Servicios");
        serviceDAO = new ServiceDAO();

        try {
            FlatLightLaf.setup();
        } catch (Exception ex) {
            System.err.println("Error al establecer FlatLaf: " + ex);
        }

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // Aumentamos el tamaño para acomodar más campos
        setSize(950, 600); // Aumento de ancho para nuevas columnas
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Formulario: Modifica el formPanel para usar MigLayout, que es más flexible
        // wrap 4: 4 columnas antes de un salto de línea. [right]10[grow,fill]: La etiqueta a la derecha, el campo crece y se rellena.
        JPanel formPanel = new JPanel(new MigLayout("wrap 4, fillx", "[right]10[grow,fill]"));
        formPanel.setBorder(new TitledBorder("Datos del Servicio"));

        nameField = new JTextField();
        // Inicializamos los campos de precio existentes
        priceCortoField = new JTextField("0.0");
        priceMedioField = new JTextField("0.0");
        priceLargoField = new JTextField("0.0");
        priceExtField = new JTextField("0.0");
        // Inicializamos los NUEVOS campos
        permiteClienteCheck = new JCheckBox("Permite que cliente traiga producto");
        priceClienteProductoField = new JTextField("0.0");

        // Fila 1: Nombre (ocupa 4 columnas)
        formPanel.add(new JLabel("Nombre:"));
        formPanel.add(nameField, "span, growx");

        // Fila 2: P. Corto y P. Medio
        formPanel.add(new JLabel("P. Corto ($):"));
        formPanel.add(priceCortoField, "growx");

        formPanel.add(new JLabel("P. Medio ($):"));
        formPanel.add(priceMedioField, "growx");

        // Fila 3: P. Largo y P. Ext.
        formPanel.add(new JLabel("P. Largo ($):"));
        formPanel.add(priceLargoField, "growx");

        formPanel.add(new JLabel("P. Ext. ($):"));
        formPanel.add(priceExtField, "growx");

        // Fila 4: Checkbox y P. Cliente
        formPanel.add(permiteClienteCheck, "span 2"); // Checkbox ocupa 2 columnas
        formPanel.add(new JLabel("P. Cliente ($):")); // Etiqueta para el nuevo precio
        formPanel.add(priceClienteProductoField, "growx"); // Campo para el nuevo precio

        // Botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("Agregar");
        JButton updateButton = new JButton("Actualizar");
        JButton deleteButton = new JButton("Eliminar");
        JButton clearButton = new JButton("Limpiar");
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(formPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Tabla: Actualizamos el encabezado para las nuevas columnas
        String[] columnNames = {"ID", "Nombre", "P. Corto", "P. Medio", "P. Largo", "P. Ext.", "Permite Cliente?", "P. Cliente"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override // Sobreescribimos para que la columna 6 (Permite Cliente?) muestre booleanos
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 6) {
                    return Boolean.class;
                }
                return super.getColumnClass(columnIndex);
            }
        };
        serviceTable = new JTable(tableModel);
        serviceTable.getColumnModel().getColumn(0).setMaxWidth(50); // ID column width
        serviceTable.getColumnModel().getColumn(6).setMaxWidth(100); // Checkbox visual width

        // Opcional: Renderizador para el booleano como CheckBox en la tabla (no necesario si se sobrescribe getColumnClass)
        // serviceTable.getColumnModel().getColumn(6).setCellRenderer(serviceTable.getDefaultRenderer(Boolean.class));

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(serviceTable), BorderLayout.CENTER);

        add(mainPanel);

        // Listeners
        addButton.addActionListener(e -> saveService(true));
        updateButton.addActionListener(e -> saveService(false));
        deleteButton.addActionListener(e -> deleteService());
        clearButton.addActionListener(e -> clearFields());

        // Actualiza el listener de la tabla para cargar los nuevos campos
        serviceTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int selectedRow = serviceTable.getSelectedRow();
                if (selectedRow >= 0) {
                    try {
                        int serviceId = Integer.parseInt(tableModel.getValueAt(selectedRow, 0).toString());
                        // Buscamos el servicio completo
                        Service service = serviceDAO.getAll().stream()
                                .filter(s -> s.getId() == serviceId)
                                .findFirst()
                                .orElse(null);

                        if (service != null) {
                            currentService = service;
                            nameField.setText(service.getName());
                            priceCortoField.setText(String.valueOf(service.getPrice_corto()));
                            priceMedioField.setText(String.valueOf(service.getPrice_medio()));
                            priceLargoField.setText(String.valueOf(service.getPrice_largo()));
                            priceExtField.setText(String.valueOf(service.getPrice_ext()));
                            // Cargar nuevos campos
                            permiteClienteCheck.setSelected(service.isPermiteClienteProducto());
                            priceClienteProductoField.setText(String.valueOf(service.getPriceClienteProducto()));
                        }
                    } catch (SQLException | NumberFormatException ex) {
                        JOptionPane.showMessageDialog(ServiceManagementWindow.this, "Error al seleccionar el servicio: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        loadServices();
    }

    private void loadServices() {
        try {
            tableModel.setRowCount(0);
            List<Service> services = serviceDAO.getAll();
            // Formato con punto y dos decimales, o coma si es la convención local
            DecimalFormat df = new DecimalFormat("#,##0.00"); 
            for (Service service : services) {
                // Agregamos todos los campos a la fila de la tabla
                tableModel.addRow(new Object[]{
                    service.getId(),
                    service.getName(),
                    df.format(service.getPrice_corto()),
                    df.format(service.getPrice_medio()),
                    df.format(service.getPrice_largo()),
                    df.format(service.getPrice_ext()),
                    service.isPermiteClienteProducto(), // Nuevo: valor booleano
                    df.format(service.getPriceClienteProducto()) // Nuevo: Precio Cliente
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar servicios: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveService(boolean isNew) {
        String name = nameField.getText().trim();
        boolean permiteCliente = permiteClienteCheck.isSelected(); // Obtener estado del checkbox

        // Creamos una lista de los campos de precio y sus nombres para validación
        JTextField[] priceFields = {priceCortoField, priceMedioField, priceLargoField, priceExtField, priceClienteProductoField}; // 5 campos
        String[] priceNames = {"P. Corto", "P. Medio", "P. Largo", "P. Ext.", "P. Cliente"}; // 5 nombres
        double[] prices = new double[5];

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validación de todos los campos de precio
        try {
            for (int i = 0; i < priceFields.length; i++) {
                // Si estamos en el campo "P. Cliente" (índice 4) y el checkbox NO está marcado, su valor es 0.0 y saltamos la validación
                if (i == 4 && !permiteCliente) {
                    prices[i] = 0.0;
                    continue;
                }

                String priceStr = priceFields[i].getText().trim().replace(',', '.'); // Reemplazar coma por punto para el parseo
                
                if (priceStr.isEmpty()) {
                    // Si es cualquier precio normal (índice < 4), es obligatorio.
                    if (i < 4){
                        JOptionPane.showMessageDialog(this, priceNames[i] + " es obligatorio.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    } 
                    // Si es P. Cliente (índice 4) y *permite*, asumimos 0.0 si está vacío.
                    else { 
                        prices[i] = 0.0;
                        continue;
                    }
                }

                double price = Double.parseDouble(priceStr);
                if (price < 0) {
                    JOptionPane.showMessageDialog(this, priceNames[i] + " no puede ser negativo.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                prices[i] = price;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Uno o más precios son inválidos. Use punto (.) para decimales.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (isNew) {
            // Creamos el nuevo objeto Service con los 5 precios y el booleano
            currentService = new Service(0, name, prices[0], prices[1], prices[2], prices[3], permiteCliente, prices[4]);
        } else {
            if (currentService == null) {
                JOptionPane.showMessageDialog(this, "Seleccione un servicio de la tabla para actualizar.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Actualizamos todos los campos del objeto existente
            currentService.setName(name);
            currentService.setPrice_corto(prices[0]);
            currentService.setPrice_medio(prices[1]);
            currentService.setPrice_largo(prices[2]);
            currentService.setPrice_ext(prices[3]);
            currentService.setPermiteClienteProducto(permiteCliente); // Nuevo: booleano
            currentService.setPriceClienteProducto(prices[4]); // Nuevo: precio cliente
        }

        try {
            serviceDAO.save(currentService);
            JOptionPane.showMessageDialog(this, "Servicio guardado con éxito.");
            loadServices();
            clearFields();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al guardar el servicio: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteService() {
        if (currentService == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un servicio para eliminar.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int response = JOptionPane.showConfirmDialog(this, "¿Está seguro de que desea eliminar este servicio?", "Confirmar Eliminación", JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            try {
                serviceDAO.delete(currentService.getId());
                JOptionPane.showMessageDialog(this, "Servicio eliminado con éxito.");
                loadServices();
                clearFields();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al eliminar el servicio: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearFields() {
        currentService = null;
        nameField.setText("");
        // Limpiamos los 4 campos de precio existentes
        priceCortoField.setText("0.0");
        priceMedioField.setText("0.0");
        priceLargoField.setText("0.0");
        priceExtField.setText("0.0");
        // Limpiamos los nuevos campos
        permiteClienteCheck.setSelected(false);
        priceClienteProductoField.setText("0.0");
        serviceTable.clearSelection();
    }

    // Método main (opcional, si no está en otra clase)
    /*
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ServiceManagementWindow().setVisible(true);
        });
    }
    */
}