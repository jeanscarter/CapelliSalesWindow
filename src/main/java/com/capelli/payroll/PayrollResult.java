package com.capelli.payroll;

import com.capelli.model.CuentaBancaria;
import com.capelli.model.Trabajadora;

public record PayrollResult(
    Trabajadora trabajadora,
    double amountToPay,
    CuentaBancaria primaryAccount
) {}