package com.capelli.payroll;

import com.capelli.model.CuentaBancaria;
import com.capelli.model.Trabajadora;

public record PayrollResult(
    Trabajadora trabajadora,
    double amountToPayBank,  // Comisiones normales (para transferencia)
    double amountToPayCash,  // Pagos manuales (para efectivo $)
    CuentaBancaria primaryAccount
) {}