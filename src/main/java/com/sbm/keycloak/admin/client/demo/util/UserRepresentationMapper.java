package com.sbm.keycloak.admin.client.demo.util;

import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.Mapper;

@Mapper
public interface UserRepresentationMapper {

    UserRepresentation destination (UserRepresentation source);
}

