package com.kb.wms.product.application.port.out;

/**
 * 상품 코드 일련번호 채번 아웃바운드 포트. 브랜드+카테고리 조합별로 마지막 발급 번호를 보관한다.
 */
public interface ProductCodeSequenceRepository {

    /**
     * 조합의 다음 번호를 발급한다. 같은 번호를 두 번 돌려주지 않는다.
     * 같은 브랜드의 호출이 겹치지 않도록 호출자가 브랜드 행을 먼저 잠가야 한다.
     */
    long next(Long brandId, Long categoryId);
}
