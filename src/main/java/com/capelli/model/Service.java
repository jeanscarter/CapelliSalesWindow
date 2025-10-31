package com.capelli.model;

public class Service {
    private int id;
    private String name;
    private double price_corto; 
    private double price_medio; 
    private double price_largo; 
    private double price_ext;
    private boolean permite_cliente_producto; 
    private double price_cliente_producto;  

    // Constructores y otros getters/setters...
    public Service() {}
    
   public Service(int id, String name, double price_corto, double price_medio, double price_largo, double price_ext, boolean permite_cliente_producto, double price_cliente_producto) {
         this.id = id;
         this.name = name;
         this.price_corto = price_corto;
         this.price_medio = price_medio;
         this.price_largo = price_largo;
         this.price_ext = price_ext;
         this.permite_cliente_producto = permite_cliente_producto; 
         this.price_cliente_producto = price_cliente_producto;    
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
    
    public boolean isPermiteClienteProducto() {
         return permite_cliente_producto;
     }

     public void setPermiteClienteProducto(boolean permite_cliente_producto) {
         this.permite_cliente_producto = permite_cliente_producto;
     }

     public double getPriceClienteProducto() {
         return price_cliente_producto;
     }

     public void setPriceClienteProducto(double price_cliente_producto) {
         this.price_cliente_producto = price_cliente_producto;
     }
    
}