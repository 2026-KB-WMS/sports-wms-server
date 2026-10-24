/**
 * 대시보드 본사 홈 조회 결과 모델. 입고·출고·재고·지점 발주 집계를 담은 읽기 전용 값으로,
 * 조회 전용 쿼리(dashboard out port)가 집계 결과를 바로 채운다.
 * 다른 도메인의 enum은 참조하지 않고 상태는 이름(String)으로 담는다.
 */
package com.kb.wms.dashboard.application.port.in.hq.result;
