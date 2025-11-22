package com.capelli.model;

import javax.swing.ImageIcon;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Trabajadora {
    private int id;
    private String nombres;
    private String apellidos;
    private String tipoCi;
    private String numeroCi;
    private String telefono;
    private String correoElectronico;
    private ImageIcon foto;
    private List<CuentaBancaria> cuentas;

    // NUEVOS CAMPOS PARA EL BONO
    private boolean bonoActivo;
    private double montoBono;
    private String razonBono;

    public Trabajadora() {
        this.cuentas = new ArrayList<>();
        // Inicializar valores por defecto del bono
        this.bonoActivo = false;
        this.montoBono = 0.0;
        this.razonBono = "";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }
    
    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }

    public String getTipoCi() {
        return tipoCi;
    }

    public void setTipoCi(String tipoCi) {
        this.tipoCi = tipoCi;
    }

    public String getNumeroCi() {
        return numeroCi;
    }
    
    public String getCiCompleta() {
        return tipoCi + "-" + numeroCi;
    }

    public void setNumeroCi(String numeroCi) {
        this.numeroCi = numeroCi;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico;
    }

    public ImageIcon getFoto() {
        return foto;
    }

    public void setFoto(ImageIcon foto) {
        this.foto = foto;
    }

    public List<CuentaBancaria> getCuentas() {
        return cuentas;
    }

    public void setCuentas(List<CuentaBancaria> cuentas) {
        this.cuentas = cuentas;
    }

    public Optional<CuentaBancaria> getCuentaPrincipal() {
        return cuentas.stream().filter(CuentaBancaria::isEsPrincipal).findFirst();
    }

    // ===== MÉTODOS GETTER Y SETTER PARA BONOS =====

    public boolean isBonoActivo() {
        return bonoActivo;
    }

    public void setBonoActivo(boolean bonoActivo) {
        this.bonoActivo = bonoActivo;
    }

    public double getMontoBono() {
        return montoBono;
    }

    public void setMontoBono(double montoBono) {
        this.montoBono = montoBono;
    }

    public String getRazonBono() {
        return razonBono;
    }

    public void setRazonBono(String razonBono) {
        this.razonBono = razonBono;
    }
}