package com.sbm.keycloak.admin.client.demo.util;

import org.keycloak.representations.idm.UserRepresentation;

public class CustomMapper {

    // Todo : change to native bean mapper
    public static UserRepresentation customMap(UserRepresentation source) {
      UserRepresentation destination =new UserRepresentation();
      destination.setId(source.getId());
      destination.setUsername(source.getUsername());
      destination.setFirstName(source.getFirstName());
      destination.setLastName(source.getLastName());
      destination.setEmail(source.getEmail());
      destination.setCredentials(source.getCredentials());

      return destination;
    }
    
}
