package com.sbm.keycloak.admin.client.demo.util;

import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.factory.Mappers;

public class MapperUtils {

    static UserRepresentationMapper mapper = Mappers.getMapper(UserRepresentationMapper.class);

    public static UserRepresentation map(UserRepresentation source) {
      return  mapper.destination(source);
    }

     
}
