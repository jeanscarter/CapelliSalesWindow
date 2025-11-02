package com.capelli.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.logging.Logger;

public class Cliente implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(Cliente.class.getName());
    
    private int clientId;
    private String cedula;
    private String fullName;
    private String address;
    private String phone;
    
    private String hairType;
    private LocalDate birthDate;
    private LocalDate lastDyeDate;
    private LocalDate lastChemicalDate;

    private LocalDate lastKeratinaDate;  
    
    private String extensionsType;
    private LocalDate lastExtensionsMaintenanceDate;
    
    public Cliente() {
    }
    
    public Cliente(int clientId, String cedula, String fullName, String hairType) {
        this.clientId = clientId;
        this.cedula = cedula;
        this.fullName = fullName;
        this.hairType = hairType;
    }
    
    public Cliente(
            int clientId,
            String cedula,
            String fullName,
            String address,
            String phone,
            String hairType,
            LocalDate birthDate,
            LocalDate lastDyeDate,
            LocalDate lastChemicalDate,
            LocalDate lastKeratinaDate, 
            String extensionsType,
            LocalDate lastExtensionsMaintenanceDate) {
        
        this.clientId = clientId;
        this.cedula = cedula;
        this.fullName = fullName;
        this.address = address;
        this.phone = phone;
        this.hairType = hairType;
        this.birthDate = birthDate;
        this.lastDyeDate = lastDyeDate;
        this.lastChemicalDate = lastChemicalDate;
        this.lastKeratinaDate = lastKeratinaDate;  
        this.extensionsType = extensionsType;
        this.lastExtensionsMaintenanceDate = lastExtensionsMaintenanceDate;
    }
    
    public int getClientId() {
        return clientId;
    }
    
    public String getCedula() {
        return cedula;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public String getAddress() {
        return address;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public String getHairType() {
        return hairType;
    }
    
    public LocalDate getBirthDate() {
        return birthDate;
    }
    
    public LocalDate getLastDyeDate() {
        return lastDyeDate;
    }
    
    public LocalDate getLastChemicalDate() {
        return lastChemicalDate;
    }
    

    public LocalDate getLastKeratinaDate() {
        return lastKeratinaDate;  
    }
    
    public String getExtensionsType() {
        return extensionsType;
    }
    
    public LocalDate getLastExtensionsMaintenanceDate() {
        return lastExtensionsMaintenanceDate;
    }
    
    
    public void setClientId(int clientId) {
        this.clientId = clientId;
    }
    
    public void setCedula(String cedula) {
        this.cedula = cedula;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public void setHairType(String hairType) {
        if (hairType != null && 
            !hairType.isEmpty() &&
            !hairType.equals("Corto") &&
            !hairType.equals("Mediano") &&
            !hairType.equals("Largo")) {
            LOGGER.warning("Tipo de cabello inválido: " + hairType);
        }
        this.hairType = hairType;
    }
    
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
    
    public void setLastDyeDate(LocalDate lastDyeDate) {
        this.lastDyeDate = lastDyeDate;
    }
    
    public void setLastChemicalDate(LocalDate lastChemicalDate) {
        this.lastChemicalDate = lastChemicalDate;
    }
    
    public void setLastKeratinaDate(LocalDate lastKeratinaDate) {
        this.lastKeratinaDate = lastKeratinaDate;  // ← Ahora coincide
    }
    
    public void setExtensionsType(String extensionsType) {
        this.extensionsType = extensionsType;
    }
    
    public void setLastExtensionsMaintenanceDate(LocalDate lastExtensionsMaintenanceDate) {
        this.lastExtensionsMaintenanceDate = lastExtensionsMaintenanceDate;
    }
    
    
    @Override
    public String toString() {
        return "Cliente{" +
                "id=" + clientId +
                ", cedula='" + cedula + '\'' +
                ", nombre='" + fullName + '\'' +
                ", cabello='" + hairType + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Cliente cliente = (Cliente) o;
        return clientId == cliente.clientId &&
               Objects.equals(cedula, cliente.cedula);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(clientId, cedula);
    }
    
    public boolean isNew() {
        return clientId == 0;
    }
    
    public boolean isComplete() {
        return clientId > 0 &&
               cedula != null && !cedula.isEmpty() &&
               fullName != null && !fullName.isEmpty() &&
               hairType != null && !hairType.isEmpty();
    }
    
    public String getDescripcionDetallada() {
        StringBuilder sb = new StringBuilder();
        sb.append("Cliente: ").append(fullName).append("\n");
        sb.append("Cédula: ").append(cedula).append("\n");
        sb.append("Teléfono: ").append(phone != null ? phone : "N/A").append("\n");
        sb.append("Dirección: ").append(address != null ? address : "N/A").append("\n");
        sb.append("Tipo de Cabello: ").append(hairType != null ? hairType : "No definido").append("\n");
        
        if (lastDyeDate != null) {
            sb.append("Último Tinte: ").append(lastDyeDate).append("\n");
        }
        if (lastChemicalDate != null) {
            sb.append("Último Químico: ").append(lastChemicalDate).append("\n");
        }
        // ✅ CORRECCIÓN: Ahora se puede acceder correctamente
        if (lastKeratinaDate != null) {
            sb.append("Última Keratina: ").append(lastKeratinaDate).append("\n");
        }
        if (extensionsType != null && !extensionsType.isEmpty()) {
            sb.append("Extensiones: ").append(extensionsType).append("\n");
        }
        
        return sb.toString();
    }
    
    public String getCedulaFormateada() {
        if (cedula == null || cedula.isEmpty()) {
            return "";
        }
        
        if (cedula.contains("-")) {
            return cedula;
        }
        
        if (cedula.length() > 1) {
            return cedula.substring(0, 1) + "-" + cedula.substring(1);
        }
        
        return cedula;
    }
    
    public boolean needsDyeService() {
        if (lastDyeDate == null) {
            return true;
        }
        
        LocalDate twoMonthsAgo = LocalDate.now().minusMonths(2);
        return lastDyeDate.isBefore(twoMonthsAgo);
    }
    
    public boolean needsExtensionsMaintenance() {
        if (extensionsType == null || extensionsType.isEmpty()) {
            return false;
        }
        
        if (lastExtensionsMaintenanceDate == null) {
            return true;
        }
        
        LocalDate fourWeeksAgo = LocalDate.now().minusWeeks(4);
        return lastExtensionsMaintenanceDate.isBefore(fourWeeksAgo);
    }
    
    public int getAge() {
        if (birthDate == null) {
            return -1;
        }
        return LocalDate.now().getYear() - birthDate.getYear();
    }
    
    public boolean isBirthdayToday() {
        if (birthDate == null) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        return birthDate.getMonthValue() == today.getMonthValue() &&
               birthDate.getDayOfMonth() == today.getDayOfMonth();
    }
    
    public void updateLastDyeDate() {
        this.lastDyeDate = LocalDate.now();
        LOGGER.info("Último tinte actualizado para cliente: " + fullName);
    }
    
    public void updateLastChemicalDate() {
        this.lastChemicalDate = LocalDate.now();
        LOGGER.info("Último químico actualizado para cliente: " + fullName);
    }

    public void updateLastKeratinaDate() {
        this.lastKeratinaDate = LocalDate.now();
        LOGGER.info("Última keratina actualizada para cliente: " + fullName);
    }
    
    public void updateLastExtensionsMaintenanceDate() {
        this.lastExtensionsMaintenanceDate = LocalDate.now();
        LOGGER.info("Último mantenimiento de extensiones actualizado para cliente: " + fullName);
    }
    
    public boolean validateBasicData() {
        return cedula != null && !cedula.isEmpty() &&
               fullName != null && !fullName.isEmpty();
    }
    
    public boolean validateHairType() {
        if (hairType == null || hairType.isEmpty()) {
            return false;
        }
        
        return hairType.equals("Corto") ||
               hairType.equals("Mediano") ||
               hairType.equals("Largo");
    }
    
    public String getFirstName() {
        if (fullName == null || fullName.isEmpty()) {
            return "";
        }
        
        String[] parts = fullName.split(" ");
        return parts[0];
    }
    
    public String getLastName() {
        if (fullName == null || fullName.isEmpty()) {
            return "";
        }
        
        String[] parts = fullName.split(" ");
        if (parts.length > 1) {
            return parts[parts.length - 1];
        }
        return "";
    }
    
    public boolean matchesSearchQuery(String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        
        String lowerQuery = query.toLowerCase();
        
        return (fullName != null && fullName.toLowerCase().contains(lowerQuery)) ||
               (cedula != null && cedula.toLowerCase().contains(lowerQuery)) ||
               (phone != null && phone.contains(query));
    }
    
    public boolean belongsToHairType(String type) {
        if (type == null || hairType == null) {
            return false;
        }
        return hairType.equals(type);
    }
    
    public boolean usesExtensions() {
        return extensionsType != null && !extensionsType.isEmpty();
    }
}


class ClienteException extends Exception {
    public ClienteException(String message) {
        super(message);
    }
    
    public ClienteException(String message, Throwable cause) {
        super(message, cause);
    }
}

class ClienteNotFoundException extends ClienteException {
    public ClienteNotFoundException(String cedula) {
        super("Cliente no encontrado: " + cedula);
    }
}

class ClienteDuplicadoException extends ClienteException {
    public ClienteDuplicadoException(String cedula) {
        super("Ya existe un cliente con cédula: " + cedula);
    }
}