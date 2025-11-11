package com.capelli.model;

public class CommissionRule {
    private int rule_id;
    private int trabajadora_id;
    private String service_category;
    private double commission_rate;

    // Campos adicionales para mostrar en la UI
    private String trabajadora_name;

    public CommissionRule() {
    }

    public CommissionRule(int rule_id, int trabajadora_id, String service_category, double commission_rate) {
        this.rule_id = rule_id;
        this.trabajadora_id = trabajadora_id;
        this.service_category = service_category;
        this.commission_rate = commission_rate;
    }

    public int getRule_id() {
        return rule_id;
    }

    public void setRule_id(int rule_id) {
        this.rule_id = rule_id;
    }

    public int getTrabajadora_id() {
        return trabajadora_id;
    }

    public void setTrabajadora_id(int trabajadora_id) {
        this.trabajadora_id = trabajadora_id;
    }

    public String getService_category() {
        return service_category;
    }

    public void setService_category(String service_category) {
        this.service_category = service_category;
    }

    public double getCommission_rate() {
        return commission_rate;
    }

    public void setCommission_rate(double commission_rate) {
        this.commission_rate = commission_rate;
    }

    public String getTrabajadora_name() {
        return trabajadora_name;
    }

    public void setTrabajadora_name(String trabajadora_name) {
        this.trabajadora_name = trabajadora_name;
    }
}