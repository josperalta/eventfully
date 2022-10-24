package com.eventfully.core.models;

import java.util.List;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adaptables = Resource.class)
public class Rsvp {

  @Inject
  private List<Attendee> attendees;

  @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
  @Default(values="Confirmed attendees")
  private String title;
  
  public List<Attendee> getAttendees() {
    return this.attendees;
  }
  
  public void setAttendees(List<Attendee> attendees) {
    this.attendees = attendees;
  }
  
  public String getTitle() {
    return this.title;
  }

  public void setTitle(String title) {
    this.title = title;
  }
}
