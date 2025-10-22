package com.capelli.model;

public class Service {
    private int id;
    private String name;
    private double price_corto; 
    private double price_medio; 
    private double price_largo; 
    private double price_ext;   

    // Constructores y otros getters/setters...
    public Service() {}
    
    public Service(int id, String name, double price_corto, double price_medio, double price_largo, double price_ext) {
        this.id = id;
        this.name = name;
        this.price_corto = price_corto;
        this.price_medio = price_medio;
        this.price_largo = price_largo;
        this.price_ext = price_ext;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // Getters y Setters para los nuevos precios
    public double getPrice_corto() { return price_corto; }
    public void setPrice_corto(double price_corto) { this.price_corto = price_corto; }
    public double getPrice_medio() { return price_medio; }
    public void setPrice_medio(double price_medio) { this.price_medio = price_medio; }
    public double getPrice_largo() { return price_largo; }
    public void setPrice_largo(double price_largo) { this.price_largo = price_largo; }
    public double getPrice_ext() { return price_ext; }
    public void setPrice_ext(double price_ext) { this.price_ext = price_ext; }
}