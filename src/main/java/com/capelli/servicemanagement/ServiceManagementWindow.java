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
import net.miginfocom.swing.MigLayout; // Importamos MigLayout
import com.formdev.flatlaf.FlatLaf; // Importamos FlatLaf (aunque solo se usa para el setup, se incluye la importación)
import com.formdev.flatlaf.FlatLightLaf;

public class ServiceManagementWindow extends JFrame {

    private final ServiceDAO serviceDAO;
    private final DefaultTableModel tableModel;
    private final JTable serviceTable;
    private final JTextField nameField;
    // Creamos campos para cada precio
    private final JTextField priceCortoField;
    private final JTextField priceMedioField;
    private final JTextField priceLargoField;
    private final JTextField priceExtField;
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
        setSize(800, 600);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Formulario: Modifica el formPanel para usar MigLayout, que es más flexible
        // wrap 4: 4 columnas antes de un salto de línea. [right]10[grow,fill]: La etiqueta a la derecha, el campo crece y se rellena.
        JPanel formPanel = new JPanel(new MigLayout("wrap 4, fillx", "[right]10[grow,fill]"));
        formPanel.setBorder(new TitledBorder("Datos del Servicio"));

        nameField = new JTextField();
        // Inicializamos los campos de precio
        priceCortoField = new JTextField("0.0");
        priceMedioField = new JTextField("0.0");
        priceLargoField = new JTextField("0.0");
        priceExtField = new JTextField("0.0");

        // Usamos span para que el nombre ocupe dos columnas (etiqueta + campo)
        formPanel.add(new JLabel("Nombre:"));
        formPanel.add(nameField, "span, growx");

        // Campos de precios organizados en dos filas, dos pares por fila
        formPanel.add(new JLabel("P. Corto ($):"));
        formPanel.add(priceCortoField, "growx");

        formPanel.add(new JLabel("P. Medio ($):"));
        formPanel.add(priceMedioField, "growx");

        formPanel.add(new JLabel("P. Largo ($):"));
        formPanel.add(priceLargoField, "growx");

        formPanel.add(new JLabel("P. Ext. ($):"));
        formPanel.add(priceExtField, "growx");

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
        String[] columnNames = {"ID", "Nombre", "P. Corto", "P. Medio", "P. Largo", "P. Ext."};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        serviceTable = new JTable(tableModel);
        serviceTable.getColumnModel().getColumn(0).setMaxWidth(50); // ID column width

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(serviceTable), BorderLayout.CENTER);

        add(mainPanel);

        // Listeners
        addButton.addActionListener(e -> saveService(true));
        updateButton.addActionListener(e -> saveService(false));
        deleteButton.addActionListener(e -> deleteService());
        clearButton.addActionListener(e -> clearFields());

        // Actualiza el listener de la tabla
        serviceTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int selectedRow = serviceTable.getSelectedRow();
                if (selectedRow >= 0) {
                    try {
                        int serviceId = Integer.parseInt(tableModel.getValueAt(selectedRow, 0).toString());
                        // Buscamos el servicio completo en la lista para obtener todos los precios
                        // Esta es una solución simple, pero ineficiente, ya que llama a getAll()
                        // por cada clic. La mejor práctica sería almacenar la lista 'services' de 'loadServices'.
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
            DecimalFormat df = new DecimalFormat("#.##");
            for (Service service : services) {
                // Agregamos los 4 precios a la fila de la tabla
                tableModel.addRow(new Object[]{
                    service.getId(),
                    service.getName(),
                    df.format(service.getPrice_corto()),
                    df.format(service.getPrice_medio()),
                    df.format(service.getPrice_largo()),
                    df.format(service.getPrice_ext())
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar servicios: " + e.getMessage(), "Error DB", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveService(boolean isNew) {
        String name = nameField.getText().trim();

        // Creamos una lista de los campos de precio y sus nombres para validación
        JTextField[] priceFields = {priceCortoField, priceMedioField, priceLargoField, priceExtField};
        String[] priceNames = {"Precio Corto", "Precio Medio", "Precio Largo", "Precio Extensiones"};
        double[] prices = new double[4];

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validación de todos los campos de precio
        try {
            for (int i = 0; i < priceFields.length; i++) {
                String priceStr = priceFields[i].getText().trim();
                if (priceStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, priceNames[i] + " es obligatorio.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                double price = Double.parseDouble(priceStr);
                if (price < 0) {
                    JOptionPane.showMessageDialog(this, priceNames[i] + " no puede ser negativo.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                prices[i] = price;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Uno o más precios son inválidos. Debe ser un número.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (isNew) {
            // Creamos el nuevo objeto Service con los 4 precios
            currentService = new Service(0, name, prices[0], prices[1], prices[2], prices[3]);
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
        // Limpiamos los 4 campos de precio
        priceCortoField.setText("0.0");
        priceMedioField.setText("0.0");
        priceLargoField.setText("0.0");
        priceExtField.setText("0.0");
        serviceTable.clearSelection();
    }

}
