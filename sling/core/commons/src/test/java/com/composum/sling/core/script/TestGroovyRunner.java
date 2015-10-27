package com.composum.sling.core.script;

import junit.framework.TestCase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rw on 06.10.15.
 */
public class TestGroovyRunner extends TestCase {

    private static final Logger LOG = LoggerFactory.getLogger(TestGroovyRunner.class);

    @Test
    public void testSimpleScript() throws Exception {
        StringWriter out = new StringWriter();
        GroovyRunner runner = new GroovyRunner(null, new PrintWriter(out));
        Map<String,Object> variables = new HashMap();
        variables.put("serviceclass", BigDecimal.class);
        runner.run(new StringReader("info 'serviceclass: ', serviceclass"), variables);
        LOG.info("out\n" + out);
    }

    @Test
    public void testScriptResource() throws Exception {
        StringWriter out = new StringWriter();
        GroovyRunner runner = new GroovyRunner(null, new PrintWriter(out), "script/testSetup.groovy");
        Map<String,Object> variables = new HashMap();
        runner.run("script/test.groovy", variables);
        LOG.info("out\n" + out);
    }
}
