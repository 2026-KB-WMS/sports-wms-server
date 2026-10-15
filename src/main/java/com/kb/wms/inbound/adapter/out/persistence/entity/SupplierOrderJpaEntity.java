package com.kb.wms.inbound.adapter.out.persistence.entity;

import com.kb.wms.common.persistence.BaseTimeEntity;
import com.kb.wms.inbound.domain.entity.SupplierOrder;
import com.kb.wms.inbound.domain.enums.SupplierOrderStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "supplier_order",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_supplier_order_no", columnNames = "supplier_order_no"),
                @UniqueConstraint(name = "uk_supplier_order_po_supplier",
                        columnNames = {"purchase_order_id", "supplier_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupplierOrderJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_order_id")
    private Long supplierOrderId;

    @Column(name = "supplier_order_no", nullable = false, length = 50)
    private String supplierOrderNo;

    @Column(name = "purchase_order_id", nullable = false)
    private Long purchaseOrderId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SupplierOrderStatus status;

    @Builder
    private SupplierOrderJpaEntity(Long supplierOrderId, String supplierOrderNo, Long purchaseOrderId,
                                   Long supplierId, SupplierOrderStatus status) {
        this.supplierOrderId = supplierOrderId;
        this.supplierOrderNo = supplierOrderNo;
        this.purchaseOrderId = purchaseOrderId;
        this.supplierId = supplierId;
        this.status = status;
    }

    public static SupplierOrderJpaEntity fromDomain(SupplierOrder supplierOrder) {
        return SupplierOrderJpaEntity.builder()
                .supplierOrderId(supplierOrder.getSupplierOrderId())
                .supplierOrderNo(supplierOrder.getSupplierOrderNo())
                .purchaseOrderId(supplierOrder.getPurchaseOrderId())
                .supplierId(supplierOrder.getSupplierId())
                .status(supplierOrder.getStatus())
                .build();
    }

    public SupplierOrder toDomain() {
        return SupplierOrder.builder()
                .supplierOrderId(supplierOrderId)
                .supplierOrderNo(supplierOrderNo)
                .purchaseOrderId(purchaseOrderId)
                .supplierId(supplierId)
                .status(status)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }
}
