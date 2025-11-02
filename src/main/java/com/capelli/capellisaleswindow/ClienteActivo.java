package com.capelli.capellisaleswindow;

public class ClienteActivo {

    private final int id;
    private final String cedula;
    private final String nombre;
    private final String hairType;

    public ClienteActivo(int id, String cedula, String nombre, String hairType) {
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
    }
}