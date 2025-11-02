package com.capelli.capellisaleswindow;

public class VentaServicio {

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