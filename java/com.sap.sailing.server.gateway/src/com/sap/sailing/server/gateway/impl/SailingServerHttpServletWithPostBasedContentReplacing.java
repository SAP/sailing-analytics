package com.sap.sailing.server.gateway.impl;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sap.sailing.server.gateway.SailingServerHttpServlet;
import com.sap.sse.common.HttpRequestHeaderConstants;

/**
 * Offers the method {@link #writePostRefreshingHeadAndEmptyBody(HttpServletRequest, HttpServletResponse)} which implementing
 * subclasses can use in their {@link #doGet(HttpServletRequest, HttpServletResponse)} implementation to produce an empty HTML
 * response document which, after loading by the client, refreshes its content by <tt>POST</tt>ing the same request URI to the
 * server, ending up in the {@link #doPost(HttpServletRequest, HttpServletResponse)} method, however potentially on the master
 * if the previous <tt>GET</tt> request was handled by a replica. The <tt>POST</tt> request is also equipped with the
 * 
 * 
 * @author Axel Uhl (D043530)
 *
 */
public abstract class SailingServerHttpServletWithPostBasedContentReplacing extends SailingServerHttpServlet {
    private static final long serialVersionUID = -2819428093387051473L;

    protected void writePostRefreshingHeadAndBodyWithRefreshForm(HttpServletRequest req, HttpServletResponse resp, String title, String postURI) throws IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>"+title+"</title>");
        out.println("<script>\r\n" + 
                "  function loadAndReplace() {\r\n" + 
                "        var request = new XMLHttpRequest();\r\n" + 
                "        request.open(\"POST\", \"" + postURI + "\" );\r\n" + 
                "        request.setRequestHeader(\""+HttpRequestHeaderConstants.HEADER_FORWARD_TO_MASTER.getA()+"\",\""+HttpRequestHeaderConstants.HEADER_FORWARD_TO_MASTER.getB()+"\");\r\n" +
                "        request.addEventListener(\"load\", function(event) {\r\n" + 
                "                if (request.status >= 200 && request.status < 300) {\r\n" + 
                "                        document.getElementById(\""+WindStatusServlet.REFRESHING_CONTENT_DIV_ID+"\").innerHTML = request.responseText;\r\n" + 
                "                } else {\r\n" + 
                "                        document.getElementById(\""+WindStatusServlet.REFRESHING_CONTENT_DIV_ID+"\").innerHTML = \"<b>error reading data from server</b>\";\r\n" + 
                "                }\r\n" + 
                "        });\r\n" + 
                "        request.send(); \r\n" + 
                "  }\r\n" + 
                "  \r\n" + 
                "  function refresh() {\r\n" +
                "        loadAndReplace();\r\n" + 
                "        window.setInterval(loadAndReplace, 10000);\r\n" + 
                "  }\r\n" + 
                "  \r\n" + 
                "</script>");
        out.println("</head>");
        out.println("<body id=\"body\" onload=\"refresh()\" >");
        out.println("<p>Reload wind connectors with parameter "+WindStatusServlet.PARAM_RELOAD_WIND_RECEIVER+"=true:");
        out.println("This will force a connection reset and a reloading of the wind receivers. ");
        out.println("It requires permission "+WindStatusServlet.getPermissionToReload()+".</p>");
        out.println("<form action=\"/sailingserver/windStatus\" method=\"POST\">");
        out.println("  <label for=\"username\">Username:</label>");
        out.println("  <input type=\"text\" id=\"username\" name=\"username\"><br>");
        out.println("  <label for=\"password\">Password:</label>");
        out.println("  <input type=\"password\" id=\"password\" name=\"password\"><br>");
        out.println("  <input type=\"hidden\" name=\""+WindStatusServlet.PARAM_RELOAD_WIND_RECEIVER+"\" value=\"true\"><br>");
        out.println("  <input type=\"submit\" value=\"Reload\"><br>");
        out.println("</form>");
        out.println("<div id=\""+WindStatusServlet.REFRESHING_CONTENT_DIV_ID+"\"></div>");
        out.println("</body>");
        out.println("</html>");
        out.close();
    }
}
