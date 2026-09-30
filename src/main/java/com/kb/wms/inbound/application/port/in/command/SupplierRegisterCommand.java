package com.kb.wms.inbound.application.port.in.command;

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
        String address
) {
}
