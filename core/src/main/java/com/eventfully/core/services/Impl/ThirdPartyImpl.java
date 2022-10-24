package com.eventfully.core.services.Impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eventfully.core.services.ThirdParty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * A service representing the third party 
 * that will receive the subscriber data.
 */
@Component(
  service = ThirdParty.class,
  immediate = true
)
@ServiceDescription("Third party to receive subscription data")
@Designate(ocd = ThirdPartyImpl.Config.class)
public class ThirdPartyImpl implements ThirdParty {

	protected final Logger log = LoggerFactory.getLogger(ThirdPartyImpl.class);

  @ObjectClassDefinition(name = "Third party service configuration")
  public static @interface Config {

    @AttributeDefinition(
      name = "endpoint", 
      description = "The endpoint to submit data to.")
    String endpoint();

    @AttributeDefinition(
      name = "Timeout", 
      description = "The amount of time in milliseconds to wait for a response.")
    int timeout();

    @AttributeDefinition(
      name = "Attempts", 
      description = "The number of tryes to get a response.")
    int attempts();
  }

  private static final String UTF_8 = "UTF-8";
  private static final int SLEEP_INTERVAL = 500;

  private String endpoint;
  private int timeout;
  private int attempts;

  @Activate @Modified
  protected void activate(final Config config) {
    this.endpoint = config.endpoint();
    this.timeout = config.timeout();
    this.attempts = config.attempts();
  }

  @Override
  public JsonObject registerUser(String eventId, String name, String email, String notes) {
    JsonObject result = new JsonObject();
    try {
      HttpURLConnection connection = stablishConnection(eventId, name, email, notes);
      String response = fetchResponse(connection);

      if (response != null) {
        result = (JsonObject) new JsonParser().parse(response);
      }
    } catch (Exception e) {
      result.addProperty("status", "error");
      result.addProperty("errorMessage", e.getMessage());
      log.error("Unable to register user to event", e);
    }

    return result;
  }

  /**
   * Attempt to stablish a connection with the endpoint.
   * 
   * @param eventId The id of the event
   * @param name    The name of the user
   * @param email   The user's email
   * @param notes   Optional notes to attach
   * @return a connection
   * @throws IOException
   */
  private HttpURLConnection stablishConnection(String eventId, String name, String email, String notes) 
    throws IOException 
  {
    StringBuilder data = new StringBuilder();
    data.append("event=");
    data.append(URLEncoder.encode(eventId, UTF_8));
    data.append("&name=");
    data.append(URLEncoder.encode(name, UTF_8));
    data.append("&email=");
    data.append(URLEncoder.encode(email, UTF_8));
    if (StringUtils.isNoneEmpty(notes)) {
      data.append("&notes=");
      data.append(URLEncoder.encode(notes, UTF_8));
    }

    byte[] dataBytes = data.toString().getBytes(UTF_8);
    URL serverUrl = new URL(this.endpoint);

    HttpURLConnection urlConnection = (HttpURLConnection) serverUrl.openConnection();
    urlConnection.setRequestMethod("POST");
    urlConnection.setConnectTimeout(this.timeout);
    urlConnection.setReadTimeout(this.timeout);
    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    urlConnection.setRequestProperty("charset", UTF_8);
    urlConnection.setRequestProperty("Content-Length", Integer.toString(dataBytes.length));
    urlConnection.setDoOutput(true);
    urlConnection.getOutputStream().write(dataBytes);

    return urlConnection;
  }

  /**
   * Fetch a response from the stablished connection
   * @param connection the connection object
   * @return A String representing the response from the connection or null if no response
   *         was obtained.
   * @throws IOException
   * @throws UnsupportedEncodingException
   */
  private String fetchResponse(HttpURLConnection connection) 
    throws UnsupportedEncodingException, IOException 
  {
    String result = null;
    int attemptCounter = 0;
    BufferedReader reader = null;

    while (attemptCounter < this.attempts) {
      reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), UTF_8));
      if (connection != null && connection.getResponseCode() == 200) {
        break;
      } else {
        attemptCounter++;
        try {
          TimeUnit.MILLISECONDS.sleep(SLEEP_INTERVAL);
        } catch (Exception e) {
          log.error("Exception while attempting to connect", e);
        }
      }
    }

    if (reader != null) {
      result = reader.lines()
        .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
        .toString();

      reader.close();
    }
    return result;
  }
}
