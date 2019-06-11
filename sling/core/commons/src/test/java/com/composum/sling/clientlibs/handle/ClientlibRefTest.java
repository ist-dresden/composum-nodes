package com.composum.sling.clientlibs.handle;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static com.composum.sling.clientlibs.handle.Clientlib.Type.js;
import static com.composum.sling.clientlibs.handle.Clientlib.Type.link;
import static com.composum.sling.clientlibs.handle.ClientlibLink.PROP_REL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;


/**
 * Tests for {@link ClientlibRef}.
 * Created by hps on 12.05.17.
 */
public class ClientlibRefTest {

    @Test
    public void testRule() throws Exception {
        ClientlibRef ref = new ClientlibRef(js, "jslib/([1-3]*:2.3.1)/outerembed.js",
                true, null) {
            {
                assertEquals("^.*/jslib/([1-3][^/]*)/outerembed(\\.min)?\\.js$", pattern.pattern());
            }
        };
        assertTrue(ref.isSatisfiedby(new ClientlibLink(js, ClientlibLink.Kind.FILE, "/libs/jslib/2.1.2/outerembed.js", null)));
        assertFalse(ref.isSatisfiedby(new ClientlibLink(js, ClientlibLink.Kind.FILE, "/libs/jslib/4.1.2/outerembed.js", null)));
        assertFalse(ref.isSatisfiedby(new ClientlibLink(js, ClientlibLink.Kind.EXTERNALURI, "//something/somewhere.js", null)));

        ref = new ClientlibRef(js, "jslib/([1-3]*:2.3.1)/outerembed.min.js",
                true, null) {
            {
                assertEquals("^.*/jslib/([1-3][^/]*)/outerembed(\\.min)?\\.js$", pattern.pattern());
            }
        };

        ref = new ClientlibRef(js, "some/thing.min",
                true, null) {
            {
                assertEquals("^.*/some/thing(\\.min)?$", pattern.pattern());
            }
        };
    }

    @Test
    public void testUriRef() {
        assertFalse(new ClientlibRef(link, "/somepath/something", true, null).getExternalUri());
        assertFalse(new ClientlibRef(link, "relativepath/something://nonsense", true, null).getExternalUri());
        assertFalse(new ClientlibRef(link, "nonsenseprotocol://neverland", true, null).getExternalUri());
        for (String uri : Arrays.asList("//example.net/bla/blu", "http://example.net/bluf/blaeh", "https://example.net/testsestest?hi=ho")) {
            Map<String, String> relprops = Collections.singletonMap(PROP_REL, "search");
            ClientlibRef ref = new ClientlibRef(link, uri, false, null);
            ClientlibLink lnk = new ClientlibLink(link, ClientlibLink.Kind.EXTERNALURI, uri, null);
            ClientlibRef refRel = new ClientlibRef(link, uri, false, relprops);
            ClientlibLink lnkRel = new ClientlibLink(link, ClientlibLink.Kind.EXTERNALURI, uri, relprops);
            assertEquals(lnk, lnk);
            assertEquals(lnkRel, lnkRel);
            assertEquals(ref, ref);
            assertEquals(refRel, refRel);
            assertNotEquals(lnk, lnkRel);
            assertNotEquals(ref, refRel);
            assertNotEquals(lnk, ref);
            assertTrue(ref.isSatisfiedby(lnk));
            assertTrue(refRel.isSatisfiedby(lnkRel));
            assertFalse(ref.isSatisfiedby(lnkRel));
            assertFalse(refRel.isSatisfiedby(lnk));
        }
    }

}
