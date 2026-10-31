package com.mycomplex.observability.infrastructure.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.mycomplex.observability.api.CorrelationIds;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void preservesValidCorrelationIdAndClearsMdc() throws Exception {
        String expected = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIds.HEADER_NAME, expected);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationIds.HEADER_NAME))
                .isEqualTo(expected);
        assertThat(MDC.get(CorrelationIds.MDC_KEY)).isNull();
    }

    @Test
    void replacesUnsafeCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(
                CorrelationIds.HEADER_NAME,
                "unsafe-value\r\nforged-log-entry"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String generated = response.getHeader(CorrelationIds.HEADER_NAME);
        assertThat(UUID.fromString(generated).toString()).isEqualTo(generated);
        assertThat(generated).doesNotContain("unsafe", "\r", "\n");
    }
}
