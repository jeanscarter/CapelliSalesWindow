package com.capelli.model;

public class CuentaBancaria {
    private String banco;
    private String tipoDeCuenta;
    private String numeroDeCuenta;
    private boolean esPrincipal;

    public CuentaBancaria(String banco, String tipoDeCuenta, String numeroDeCuenta, boolean esPrincipal) {
        this.banco = banco;
        this.tipoDeCuenta = tipoDeCuenta;
        this.numeroDeCuenta = numeroDeCuenta;
        this.esPrincipal = esPrincipal;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getTipoDeCuenta() {
        return tipoDeCuenta;
    }

    public void setTipoDeCuenta(String tipoDeCuenta) {
        this.tipoDeCuenta = tipoDeCuenta;
    }

    public String getNumeroDeCuenta() {
        return numeroDeCuenta;
    }

    public void setNumeroDeCuenta(String numeroDeCuenta) {
        this.numeroDeCuenta = numeroDeCuenta;
    }

    public boolean isEsPrincipal() {
        return esPrincipal;
    }

    public void setEsPrincipal(boolean esPrincipal) {
        this.esPrincipal = esPrincipal;
    }

    @Override
    public String toString() {
        return numeroDeCuenta + " (" + banco + ")" + (esPrincipal ? " [Principal]" : "");
    }
}