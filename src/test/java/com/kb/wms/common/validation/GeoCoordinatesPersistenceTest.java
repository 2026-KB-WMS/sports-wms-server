package com.kb.wms.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.inbound.application.port.out.SupplierRepository;
import com.kb.wms.inbound.domain.entity.Supplier;
import com.kb.wms.store.application.port.out.StoreRepository;
import com.kb.wms.store.domain.entity.Store;
import com.kb.wms.warehouse.application.port.out.WarehouseRepository;
import com.kb.wms.warehouse.domain.entity.Warehouse;

/**
 * 창고·지점·공급처의 위도·경도가 저장·조회되고, 좌표 없이 저장한 기존 행은 null로 읽히는지 확인한다.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=false")
class GeoCoordinatesPersistenceTest {

    @Autowired WarehouseRepository warehouseRepository;
    @Autowired StoreRepository storeRepository;
    @Autowired SupplierRepository supplierRepository;

    @Test
    @DisplayName("창고 좌표를 저장하고 소수 7자리로 읽는다")
    void warehouse() {
        Warehouse withGeo = Warehouse.register("GEO-WH-1", "좌표 창고", "서울", null, BigDecimal.TEN);
        withGeo.changeCoordinates(new BigDecimal("37.56651234567"), new BigDecimal("126.9780"));
        Warehouse withoutGeo = Warehouse.register("GEO-WH-2", "무좌표 창고", "부산", null, BigDecimal.TEN);

        Warehouse saved = warehouseRepository.findById(warehouseRepository.save(withGeo).getWarehouseId()).orElseThrow();
        Warehouse savedWithout = warehouseRepository
                .findById(warehouseRepository.save(withoutGeo).getWarehouseId()).orElseThrow();

        assertThat(saved.getLatitude()).isEqualByComparingTo("37.5665123");
        assertThat(saved.getLongitude()).isEqualByComparingTo("126.9780000");
        assertThat(savedWithout.getLatitude()).isNull();
        assertThat(savedWithout.getLongitude()).isNull();
    }

    @Test
    @DisplayName("주소만 바꿔 다시 저장하면 기존 좌표를 유지한다")
    void storeKeepsCoordinatesWhenAddressChanges() {
        Store store = Store.register("GEO-ST-1", "좌표 지점", "서울", null, "02-123-4567");
        store.changeCoordinates(new BigDecimal("35.1595"), new BigDecimal("129.0756"));
        Long storeId = storeRepository.save(store).getStoreId();

        Store loaded = storeRepository.findById(storeId).orElseThrow();
        loaded.changeAddress("부산시");
        storeRepository.save(loaded);

        Store reloaded = storeRepository.findById(storeId).orElseThrow();
        assertThat(reloaded.getAddress()).isEqualTo("부산시");
        assertThat(reloaded.getLatitude()).isEqualByComparingTo("35.1595");
        assertThat(reloaded.getLongitude()).isEqualByComparingTo("129.0756");
    }

    @Test
    @DisplayName("공급처 좌표를 저장하고 조회한다")
    void supplier() {
        Supplier supplier = Supplier.register("GEO-SUP-1", "좌표 공급처", "담당", "02-123-4567", null, "서울");
        supplier.changeCoordinates(new BigDecimal("37.4979"), new BigDecimal("127.0276"));

        Supplier saved = supplierRepository.findById(supplierRepository.save(supplier).getSupplierId()).orElseThrow();

        assertThat(saved.getLatitude()).isEqualByComparingTo("37.4979");
        assertThat(saved.getLongitude()).isEqualByComparingTo("127.0276");
    }
}
