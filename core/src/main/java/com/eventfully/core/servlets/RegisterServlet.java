package com.eventfully.core.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eventfully.core.services.ThirdParty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.apache.sling.query.SlingQuery.$;

@Component(service = { Servlet.class },
property = {
  "service.description=" + "Event submit",
  "sling.servlet.methods=" + HttpConstants.METHOD_POST,
  "sling.servlet.paths=" + "/bin/eventfully/submit"
})
public class RegisterServlet extends SlingAllMethodsServlet {

  private final Logger log = LoggerFactory.getLogger(getClass());

  // ASSUMPTION:  this is the location of the rsvp component
  private static final String RSVP_PARENT = "jcr:content/root/container";
  private static final String RSVP_COMPONENT = "eventfully/components/rsvp";
  private static final String SYSTEM_USER = "rsvp-user";
  private static final String NODE_RSVP = "rsvp";
  private static final String NODE_ATTENDEES = "attendees";
  private static final String NODE_ITEM = "item_";


  private static final String ERROR_PAGE = "/content/eventfully/us/en/register/error";

  private static final String STATUS_ERROR = "error";
  private static final String PARAM_EVENT = "event";
  private static final String PARAM_NAME = "name";
  private static final String PARAM_EMAIL = "email";
  private static final String PARAM_NOTES = "notes";

  @Reference
  private ThirdParty thirdPartyService;

  @Reference
	public ResourceResolverFactory resourceResolverFactory;


  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException 
  {
    JsonParser jsonParser = new JsonParser();
    JsonObject data = (JsonObject)jsonParser.parse(request.getReader());

    // Validate data
    if (!data.has(PARAM_EVENT) || !data.has(PARAM_NAME) || !data.has(PARAM_EMAIL)) {
      log.error("Missing parameters");
      response.sendRedirect(ERROR_PAGE);
    }

    String eventPath = data.get(PARAM_EVENT).getAsString();
    String name = data.get(PARAM_NAME).getAsString();
    String email = data.get(PARAM_EMAIL).getAsString();
    String notes = data.has(PARAM_NOTES) ? 
      StringUtils.defaultString(data.get(PARAM_NOTES).getAsString()) : 
      StringUtils.EMPTY;

    // Submit data to third party
    JsonObject thirdPartyResponse = thirdPartyService.registerUser(
      eventPath,
      name,
      email,
      notes
    );

    // ASSUMPTION: response will always have the status field.
    if (thirdPartyResponse == null || 
        STATUS_ERROR.equalsIgnoreCase(thirdPartyResponse.get("status").getAsString())) 
    {
      String errorMsg = "";
      if (thirdPartyResponse != null && thirdPartyResponse.has("errorMessage")) {
        errorMsg = thirdPartyResponse.get("errorMessage").getAsString();
      }
      log.error("Third party service returned an error: " + errorMsg);
      response.sendRedirect(ERROR_PAGE);
    }

    // Add user to confirm attendee list
    Map<String, Object> param = new HashMap<String, Object>();
		param.put(ResourceResolverFactory.SUBSERVICE, SYSTEM_USER);

    ResourceResolver resourceResolver = null;
    try {
      resourceResolver = resourceResolverFactory.getServiceResourceResolver(param);
      markAttendeeAsConfirmed(resourceResolver, eventPath, email, name);
      resourceResolver.commit();
    } catch (PersistenceException | LoginException e) {
      log.error("Unable to save user rsvp confirmation", e);
      response.sendRedirect(ERROR_PAGE);
    } finally {
      if (resourceResolver != null) {
        resourceResolver.close();
      }
    }

    log.info("User: " + email + " confirmed to attend: " + eventPath);
  }

  /**
   * Find the attendee in the RSVP component for the given event and change the confirmed status to true
   * 
   * @param resourceResolver  the resource resolver
   * @param eventPath the path to the event page
   * @param email the email of the attendee
   * @param name  the name of the attendee
   * @throws PersistenceException
   */
  private void markAttendeeAsConfirmed(ResourceResolver resourceResolver, String eventPath, String email, String name) 
    throws PersistenceException 
  {
    Resource container = resourceResolver.getResource(eventPath + RSVP_PARENT);
    Iterator<Resource> resourceIterator = $(container).children(RSVP_COMPONENT).iterator();
    Resource rsvpComponent = null;

    // If the resource does not exist then create it
    rsvpComponent = resourceIterator.hasNext() ?
      resourceIterator.next() :
      createResource(resourceResolver, container, NODE_RSVP, RSVP_COMPONENT, null);

    // if attendees resource does not exist then create it
    Resource attendeesListResource = rsvpComponent.getChild(NODE_ATTENDEES);
    if (attendeesListResource == null) {
      attendeesListResource = createResource(resourceResolver, rsvpComponent, NODE_ATTENDEES, null, null);
    }

    // fetch the matching resource attendee
    resourceIterator = $(attendeesListResource).children("nt:unstructured[email=" + email + "]").iterator();

    // if attendee is not on the list then add it, otherwise just update the confirmed status.
    if (resourceIterator.hasNext()) {
      ModifiableValueMap attendeeProperties = resourceIterator.next().adaptTo(ModifiableValueMap.class);
      attendeeProperties.put("confirmed", "true");
    } else {
      Map<String, Object> attendeeProperties = new HashMap<>();
      attendeeProperties.put("name", name);
      attendeeProperties.put("email", email);
      attendeeProperties.put("confirmed", "true");

      String resourceName = NODE_ITEM + System.currentTimeMillis();
      createResource(resourceResolver, attendeesListResource, resourceName, null, attendeeProperties);
    }
  }

  /**
   * Create a node under the provided resource parent
   * 
   * @param resourceResolver the resource resolver
   * @param parent  the parent resource
   * @param name  the name of the resource
   * @param resourceType  the resource type
   * @param extraProperties any extra properties to be added to the created resource
   * @return  the created resource
   * @throws PersistenceException
   */
  private Resource createResource(
    ResourceResolver resourceResolver, Resource parent, String name, 
    String resourceType, Map<String, Object> extraProperties) throws PersistenceException 
  {
    Map<String, Object> properties = new HashMap<>();
    if (resourceType != null) {
      properties.put("sling:resourceType", resourceType);
    }
    if (extraProperties != null) {
      properties.putAll(extraProperties);
    }
    return resourceResolver.create(parent, name, properties);
  }
}
