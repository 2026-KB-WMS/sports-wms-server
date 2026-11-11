package com.kb.wms.common.logging;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * 요청 스레드별로 Hibernate가 실행한 SQL을 모은다. 개수는 목록 크기다.
 * 요청 하나는 한 스레드에서 처리되므로 ThreadLocal로 충분하다.
 * {@code spring.jpa.properties.hibernate.session_factory.statement_inspector}로 등록한다.
 */
public class QueryCountInspector implements StatementInspector {

    private static final ThreadLocal<List<String>> STATEMENTS = ThreadLocal.withInitial(ArrayList::new);

    @Override
    public String inspect(String sql) {
        STATEMENTS.get().add(sql);
        return sql;
    }

    static void reset() {
        STATEMENTS.get().clear();
    }

    static int count() {
        return STATEMENTS.get().size();
    }

    static List<String> statements() {
        return List.copyOf(STATEMENTS.get());
    }
}
