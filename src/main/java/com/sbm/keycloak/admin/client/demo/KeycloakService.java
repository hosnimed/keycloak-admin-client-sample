package com.sbm.keycloak.admin.client.demo;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

public class KeycloakService {

    static Keycloak keycloak = null;
    final static String serverUrl = "http://localhost:8080/auth";
    final static String realm = "demo";
    final static String clientId = "idm-client";
    final static String clientSecret = "d0309200-cec2-4e5e-8c3d-54065eb4c3e4";

    public KeycloakService() {
    }

    public static Keycloak getInstance(){
        if(keycloak == null){
        keycloak = KeycloakBuilder.builder() //
                .serverUrl(serverUrl) //
                .realm(realm) //
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS) //
                .clientId(clientId) //
                .clientSecret(clientSecret) //
                .build();
        }
        return keycloak;
    }
}
