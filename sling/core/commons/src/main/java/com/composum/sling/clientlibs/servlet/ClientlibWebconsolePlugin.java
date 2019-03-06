package com.composum.sling.clientlibs.servlet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Rudimentary webconsole plugin for displaying clientlib stati.
 */
@Component(label = "Composum Clientlib Webconsole Plugin", description = "Delivers some debugging informations about the clientlibs through the Felix Webconsole")
@Service(value = Servlet.class)
@Properties({
        @Property(name = "felix.webconsole.label", value = "clientlibs"),
        @Property(name = "felix.webconsole.title", value = "Composum Client Libraries"),
        @Property(name = "felix.webconsole.category", value = "Status")
})
public class ClientlibWebconsolePlugin extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String contextPath = req.getContextPath();
        String uri = req.getRequestURI();
        StringBuffer url = req.getRequestURL();
        String path = ClientlibDebugServlet.PATH + ".all.html";
        RequestDispatcher requestDispatcher = getServletContext().getRequestDispatcher(path);
        if (requestDispatcher != null) {
            requestDispatcher.include(req, resp);
        }
    }
}
