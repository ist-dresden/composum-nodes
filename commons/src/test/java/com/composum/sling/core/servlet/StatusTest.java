package com.composum.sling.core.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.annotation.Nonnull;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

/** Checks JSON serialization of {@link Status}. */
public class StatusTest {

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(request.getResourceBundle(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(new PropertyResourceBundle(new StringReader("")));
    }

    @Test
    public void testSerialization() throws Exception {
        Gson gson = new GsonBuilder().create();

        Status status = new Status(request, response);
        status.setStatus(213);
        status.setTitle("thetitle");
        status.warn("hello to {} from {}", "franz", 17);
        List<Map<String, Object>> list1 = status.list("list1");
        list1.add(Collections.singletonMap("mlkey", 42));
        list1.add(new HashMap() {{
            put("mlkey1", 23);
            put("mlkey2", 54);
        }});
        // status.reference("bla", );
        Map<String, Object> dat1 = status.data("dat1");
        dat1.put("dk1", 15);
        dat1.put("dk2", 18);

        Writer stringWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        status.toJson(jsonWriter);

        assertEquals("{\"status\":213,\"success\":true,\"warning\":false,\"title\":\"thetitle\"," +
                "\"messages\":[{\"level\":\"warn\",\"text\":\"hello to franz from 17\"}]," +
                "\"dat1\":{\"dk1\":15,\"dk2\":18}," +
                "\"list1\":[{\"mlkey\":42},{\"mlkey1\":23,\"mlkey2\":54}]}", stringWriter.toString());

        TestStatusWithDataImpl readback = gson.fromJson(stringWriter.toString(), TestStatusWithDataImpl.class);
        assertEquals(213, readback.getStatus());
        assertEquals("thetitle", readback.getTitle());
        assertEquals("hello to franz from 17", readback.messages.get(0).text);
        assertNotNull(readback.dat1);
        assertNotNull(readback.list1);
        assertEquals(54.0, readback.list1.get(1).get("mlkey2"));
        assertEquals(18.0, readback.dat1.get("dk2"));
    }

    /**
     * This is one way to read back status data from JSON: declare the data / list members as attributes.
     * If they are nonstandard, one could declare a @JsonAdapter with a corresponding TypeAdapter to read them back.
     */
    private static class TestStatusWithDataImpl extends Status {

        public TestStatusWithDataImpl(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) {
            super(request, response);
        }

        Map<String, Object> dat1;

        List<Map<String, Object>> list1;
    }

}
