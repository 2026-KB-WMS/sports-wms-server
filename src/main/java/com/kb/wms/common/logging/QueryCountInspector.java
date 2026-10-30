package com.kb.wms.common.logging;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * 요청 스레드별로 Hibernate가 실행한 SQL 개수를 센다.
 * 요청 하나는 한 스레드에서 처리되므로 ThreadLocal로 충분하다.
 * {@code spring.jpa.properties.hibernate.session_factory.statement_inspector}로 등록한다.
 */
public class QueryCountInspector implements StatementInspector {

    private static final ThreadLocal<int[]> COUNT = ThreadLocal.withInitial(() -> new int[1]);

    @Override
    public String inspect(String sql) {
        COUNT.get()[0]++;
        return sql;
    }

    static void reset() {
        COUNT.get()[0] = 0;
    }

    static int count() {
        return COUNT.get()[0];
    }
}
