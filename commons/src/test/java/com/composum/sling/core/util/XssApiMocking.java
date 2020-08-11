package com.composum.sling.core.util;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.sling.xss.XSSAPI;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.ThrowsException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

public class XssApiMocking {

    /**
     * Setup XSSAPI mock(s) for XSS. Extend as neccessary.
     */
    public static void setupXssMock() {
        try {
            XSSAPI xssapi = mock(XSSAPI.class, new ThrowsException(new IllegalStateException("Not mocked")));
            Mockito.doAnswer(invocation -> invocation.getArgument(0)).when(xssapi).getValidHref(anyString());
            ServiceHandle xssapihandle = null;
            xssapihandle = (ServiceHandle) FieldUtils.readStaticField(XSS.class, "XSSAPI_HANDLE", true);
            FieldUtils.writeField(xssapihandle, "service", xssapi, true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
