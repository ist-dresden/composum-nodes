package com.composum.sling.nodes.update;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.jcr.RepositoryException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Component(
        label = "Composum Source Update Service",
        description = "service to update content trees from XML"
)
@Service(SourceUpdateService.class)
public class SourceUpdateServiceImpl implements SourceUpdateService {

    private static final Logger LOG = LoggerFactory.getLogger(SourceUpdateServiceImpl.class);

    private SAXParserFactory saxParserFactory;

    @Activate
    private void activate(final BundleContext bundleContext) {
        saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
    }

    @Override
    public void updateFromXml(Resource resource, InputStream inputStream) throws RepositoryException, IOException, ParserConfigurationException, SAXException {
        // FIXME remove logging output and use stream directly.
        String content = IOUtils.toString(inputStream, "UTF-8");
        LOG.info("Trying to update {} to \n{}", resource.getPath(), content);
        SAXParser saxParser = saxParserFactory.newSAXParser();
        File file = new File("test.xml");
        saxParser.parse(new ByteArrayInputStream(content.getBytes("UTF-8")), new UpdateHandler());
    }

    private class UpdateHandler extends DefaultHandler {

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < attributes.getLength(); ++i) {
                if (!"CDATA".equals(attributes.getType(i))) {
                    throw new SAXException("Unknown attribute type " + attributes.getType(i) + " of " + attributes.getQName(i) + "=" + attributes.getValue(i));
                }
                buf.append(attributes.getQName(i)).append("=").append(attributes.getValue(i)).append(" ");
            }
            LOG.info("startElement({},{}", qName, buf);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            LOG.info("endElement({})", qName);
        }

        @Override
        public void startDocument() throws SAXException {
            LOG.info("startDocument");
        }

        @Override
        public void endDocument() throws SAXException {
            LOG.info("endDocument");
        }

        // error handling stuff

        @Override
        public void error(SAXParseException e) throws SAXException {
            // better abort on any errors - we don't expect any.
            throw e;
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            String chars = new String(ch, start, length);
            // LOG.info("ignorableWhitespace: '{}'", chars);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            String chars = new String(ch, start, length);
            // LOG.info("characters: '{}'", chars);
            if (StringUtils.isNotBlank(chars)) {
                // That's not normal - don't know how to deal with that.
                throw new SAXException("Unexpected characters: " + chars);
            }
        }

    }
}
