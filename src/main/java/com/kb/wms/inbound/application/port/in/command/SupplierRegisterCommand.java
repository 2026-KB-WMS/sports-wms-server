package com.kb.wms.inbound.application.port.in.command;

import java.math.BigDecimal;

/**
 * POST /api/v1/suppliers 요청.
 *
 * @param email, address 선택
 */
public record SupplierRegisterCommand(
        String supplierCode,
        String name,
        String managerName,
        String contactNumber,
        String email,
        String address,
        BigDecimal latitude,
        BigDecimal longitude
) {

    public SupplierRegisterCommand(String supplierCode, String name, String managerName, String contactNumber,
                                   String email, String address) {
        this(supplierCode, name, managerName, contactNumber, email, address, null, null);
    }
}
