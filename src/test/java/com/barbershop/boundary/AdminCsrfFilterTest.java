package com.barbershop.boundary;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.*;

class AdminCsrfFilterTest {

    @Test
    void adminPostWithoutCsrfIsForbidden() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/appointments");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("admin", "admin");
        session.setAttribute("admin_csrf", "expected-token");
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new AdminCsrfFilter().doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void adminPostWithValidTokenPasses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/appointments");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("admin", "admin");
        session.setAttribute("admin_csrf", "expected-token");
        request.setSession(session);
        request.addHeader("X-CSRF-Token", "expected-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new AdminCsrfFilter().doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
    }
}
