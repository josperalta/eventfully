package com.eventfully.core.services;

import com.google.gson.JsonObject;

public interface ThirdParty {

  JsonObject registerUser(String eventId, String name, String email, String notes);
}
