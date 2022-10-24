package com.eventfully.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adaptables = Resource.class)
public class Attendee {

  @ValueMapValue
  private String name;

  @ValueMapValue
  private String email;

  @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
  private boolean confirmed;
  
  public String getName() {
    return this.name;
  }
  
  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return this.email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public boolean isConfirmed() {
    return this.confirmed;
  }

  public void setConfirmed(boolean confirmed) {
    this.confirmed = confirmed;
  }  
}  
