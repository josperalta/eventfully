package com.eventfully.core.servlets;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;

import com.google.gson.JsonObject;

@Component(service = { Servlet.class },
property = {
  "service.description=" + "Mocked Third Party",
  "sling.servlet.methods=" + HttpConstants.METHOD_POST,
  "sling.servlet.paths=" + "/bin/eventfully/mocked-end-point"
})
public class MockedEndpointServlet extends SlingAllMethodsServlet {
  
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException 
  {
    JsonObject jsonResponse = new JsonObject();
    jsonResponse.addProperty("status", "success");
  }
}
