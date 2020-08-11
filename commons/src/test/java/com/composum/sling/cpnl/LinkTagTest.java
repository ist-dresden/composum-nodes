package com.composum.sling.cpnl;

import com.composum.sling.core.util.ServiceHandle;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.xss.XSSAPI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.stubbing.answers.ThrowsException;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.util.Locale;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Some tests for {@link LinkTag}.
 */
public class LinkTagTest {

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    @Mock
    protected SlingHttpServletRequest request;

    @Mock
    protected ResourceResolver resolver;

    @Mock
    protected PageContext pageContext;

    @Mock
    protected XSSAPI xssapi;

    @Mock
    protected JspWriter jspWriter;

    protected StringBuilder result = new StringBuilder();

    @Before
    public void setup() throws IOException, IllegalAccessException {
        MockitoAnnotations.initMocks(this);
        jspWriter = mock(JspWriter.class, new ThrowsException(new IllegalStateException("Not mocked")));
        when(pageContext.getOut()).thenReturn(jspWriter);
        when(pageContext.getRequest()).thenReturn(request);
        when(request.getResourceResolver()).thenReturn(resolver);
        when(request.getLocale()).thenReturn(Locale.GERMANY);
        when(resolver.map(ArgumentMatchers.any(HttpServletRequest.class), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        Mockito.doAnswer(
                invocation -> result.append(invocation.getArgument(0, String.class))
        ).when(jspWriter).write(anyString());

        xssapi = mock(XSSAPI.class, new ThrowsException(new IllegalStateException("Not mocked")));
        Mockito.doAnswer(invocation -> invocation.getArgument(0)).when(xssapi).encodeForHTML(anyString());
        Mockito.doAnswer(invocation -> invocation.getArgument(0)).when(xssapi).getValidHref(anyString());
        ServiceHandle xssapihandle = (ServiceHandle) FieldUtils.readStaticField(com.composum.sling.core.util.XSS.class, "XSSAPI_HANDLE", true);
        FieldUtils.writeField(xssapihandle, "service", xssapi, true);
    }

    @Test
    public void testHref() throws JspException {
        ec.checkThat(render("?foo=bar"), is("<a href=\"?foo=bar\"></a>"));
        ec.checkThat(render("/something"), is("<a href=\"/something\"></a>"));
        ec.checkThat(render("http://somewhere.net/"), is("<a href=\"http://somewhere.net/\"></a>"));
    }

    @Test
    public void testMostAttributes() throws JspException {
        LinkTag tag = new LinkTag();
        tag.setPageContext(pageContext);
        tag.setHref("http://go.gl/bla");
        tag.setTarget("tgt");
        tag.setClasses("clses");
        tag.setMap(false);
        tag.setRole("rle");
        tag.setTagName("tg");
        tag.setUrlAttr("ul");
        tag.setDynamicAttribute("jcr", "title", "tit");
        tag.setTest(true);
        tag.setId("id");
        ec.checkThat(render(tag), is("<tg ul=\"http://go.gl/bla\" role=\"rle\" class=\"clses\" jcr:title=\"tit\" target=\"tgt\"></tg>"));
    }

    @Test
    public void testFormat() throws JspException {
        LinkTag tag = new LinkTag();
        tag.setPageContext(pageContext);
        tag.setUrl("foo");
        tag.setFormat("/bin/something.html/{}");
        ec.checkThat(render(tag), is("<a href=\"/bin/something.html/foo\"></a>"));
    }

    @Nonnull
    protected String render(String href) throws JspException {
        LinkTag tag = new LinkTag();
        tag.setPageContext(pageContext);
        tag.setHref(href);
        return render(tag);
    }

    @Nonnull
    private String render(LinkTag tag) throws JspException {
        result.setLength(0);
        tag.doStartTag();
        tag.doInitBody();
        tag.doAfterBody();
        tag.doEndTag();
        return result.toString();
    }

}
