package com.sbm.keycloak.admin.client.demo;


import com.sbm.keycloak.admin.client.demo.exceptions.ClientAppRepresentationNotFoundException;
import com.sbm.keycloak.admin.client.demo.exceptions.KCServerConnexionException;
import com.sbm.keycloak.admin.client.demo.exceptions.UserRepresentationNotFoundException;

import com.sbm.keycloak.admin.client.demo.util.CustomMapper;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

//@Slf4j
public class KeycloakAdminClientDemo {

    private static Logger log = LoggerFactory.getLogger(KeycloakAdminClientDemo.class);


    Keycloak keycloak = null;
    RealmResource realmResource = null;
    UsersResource usersResource = null;

    public KeycloakAdminClientDemo() {
        keycloak = KeycloakService.getInstance();
        getUsersResource();
    }

    // Get realm
    public RealmResource getRealmResource() throws KCServerConnexionException {
        if (keycloak == null) {
            throw new KCServerConnexionException("Realm Resource ...");
        }
        log.info("Realm : " + keycloak.realms().realm("demo").toString());
        realmResource = keycloak.realm(KeycloakService.realm);
        return realmResource;
    }

    // Get users
    public UsersResource getUsersResource() {
        if (realmResource == null) {
            try {
                getRealmResource();
            } catch (KCServerConnexionException e) {
                log.error("Error when getting users resource");
            }
        }
        usersResource = realmResource.users();
        return usersResource;
    }

    /***
     * Retrieve a user by UUID
     * @param uuid
     * @return the correspondent UserRepresentation or a new empty one as fallback
     */
    public UserRepresentation findUserByUUID(String uuid) {
        Optional<UserResource> userResource = Optional.of(usersResource.get(uuid));
        /**
         *  Todo : Enhance Exception handling with fallback
         */
        UserRepresentation userRepresentationFallback = new UserRepresentation();
        if (userResource.isPresent()) {
            try {
                Optional.of(userResource.get().toRepresentation()).orElseThrow(() -> new UserRepresentationNotFoundException(String.format("User with UUID : %s not found!", uuid).toString()));
            } catch (javax.ws.rs.NotFoundException ex) {
                log.error("{}:findUserByUUID {}", this.getClass().getCanonicalName(), ex.getMessage());
                return userRepresentationFallback;
            } catch (UserRepresentationNotFoundException e) {
                log.error("{}:findUserByUUID {}", this.getClass().getCanonicalName(), e.getMessage());
            }
        }
        return userResource.get().toRepresentation();
    }

    /***
     * Create User with eventual Credential (basically password credential)
     * @param userToAdd : The User representation in the KC terminology
     * @param credentialRepresentation : The Credential representation in the KC terminology
     * @return the new UserRepresentation after has been created successfully
     */
    public UserRepresentation addUserWithCredentials(UserRepresentation userToAdd, CredentialRepresentation credentialRepresentation) {
        if (credentialRepresentation != null) {
            // Define password credential
            credentialRepresentation.setTemporary(false);
            credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
            // Set password credential
            userToAdd.setCredentials(Arrays.asList(credentialRepresentation));
        }

        // Create user (requires manage-users role)
        Response response = usersResource.create(userToAdd);
        if (response != null) {
            log.info("Repsonse Code: " + response.getStatusInfo());
            if (response.getStatusInfo().equals(Response.Status.CREATED)) {
                log.info("Repsonse Location: " + response.getLocation());
            }
        }
        // Todo : resolve Conflict response code
        // Todo : retrieve created userId from response Uri path
        // Refer @ https://issues.jboss.org/browse/KEYCLOAK-4255
        StringBuilder builder = new StringBuilder();
        if (usersResource != null) {
            usersResource.list()
                    .stream()
                    .map(ur -> new ImmutablePair<>(ur.getUsername(), ur.getId()))
                    .filter(pair -> userToAdd.getUsername().equalsIgnoreCase(pair.getKey().toString()))
                    .forEach(pair -> {
                        log.info("{}:{} \n", pair.getKey(), pair.getValue());
                        builder.append(pair.getValue().toString());
                    });
        }
        String createdUserId = builder.toString();

        String userId = (response.getLocation() == null) ? createdUserId : response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
        log.info("User created with userId: {} \n", userId);
        return findUserByUUID(userId);
    }


    public UserRepresentation updateUserWithCredentials(UserRepresentation userToUpdate, CredentialRepresentation credentialRepresentation) {
        String userId = (userToUpdate.getId() == null || userToUpdate.getId().isEmpty()) ? usersResource.search(userToUpdate.getUsername()).get(0).getId() : userToUpdate.getId();
        UserResource userResource = realmResource.users().get(userId);

        UserRepresentation userBeforeUpdate = userResource.toRepresentation();
        if (credentialRepresentation != null) {
            // Define password credential
            credentialRepresentation.setTemporary(false);
            credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
            // Set password credential
            userBeforeUpdate.setCredentials(Arrays.asList(credentialRepresentation));
        }
        userBeforeUpdate = CustomMapper.customMap(userToUpdate);
        userResource.update(userBeforeUpdate);
        log.info("User updated with basic infos ! \n");
        return findUserByUUID(userId);
    }

    /**
     * Add Realm roles to an user
     *
     * @param uuid  User Id
     * @param roles List of roles in their String representation
     * @return the UserRepresentation after update
     */
    public void addRealmRolesToUser(String uuid, List<String> roles) {
        // Retrieve User
        UserRepresentation user = this.findUserByUUID(uuid);
        UserResource userResource = realmResource.users().get(uuid);
        // Get realm role (requires view-realm role)
        // TODO : Check roles existence, otherwise create it ?
        addRealmRoles(roles);

        List<RoleRepresentation> rolesRepresentationsToAdded = roles.stream()
                .map(r -> {
                    RoleRepresentation rp = realmResource.roles().get(r).toRepresentation();
                    return rp;
                })
                .collect(Collectors.toList());
        // Assign realm role to user
        /*
         * TODO: can be done with the following
         user.setRealmRoles(roles);
         userResource.update(user);
        */
        userResource.roles().realmLevel().add(rolesRepresentationsToAdded);
        log.info("User updated with Realm Roles {} \n", roles);
    }

    /**
     * Add Client roles to an user
     *
     * @param uuid     User Id
     * @param clientId AppClient Id
     * @param roles    List of roles in their String representation
     * @return the UserRepresentation after update
     */
    public void addClientRolesToUser(String uuid, String clientId, List<String> roles) {
        // Retrieve Client
        findClientAppByUUID(clientId);
        // Retrieve User
        UserRepresentation user = this.findUserByUUID(uuid);
        UserResource userResource = realmResource.users().get(uuid);
        // Get client level role (requires view-clients role)
        // TODO : Check roles existence, otherwise create it ?
        addClientRoles(clientId, roles);
        List<RoleRepresentation> rolesRepresentationsToAdded = roles.stream()
                .map(r -> {
                    RoleRepresentation rp = realmResource.clients().get(clientId).roles().get(r).toRepresentation();
                    return rp;
                })
                .collect(Collectors.toList());
        // Assign client role to user
        /*
         * TODO: can be done with the following
        HashMap<String, List<String>> clientRoles = new HashMap<>();
        clientRoles.put(clientId, roles);
        user.setClientRoles();
        */
        RoleScopeResource roleScopeResource = userResource.roles().clientLevel(clientId);
        roleScopeResource.add(rolesRepresentationsToAdded);
        log.info("User updated with Client Roles\n");
    }

    /**
     * Delete a user bu UUID
     *
     * @param uuid Check log if remove fail
     * @return Remove Status : True/False
     */
    // Delete User
    public boolean deleteUserByUUID(String uuid) {
        if (this.findUserByUUID(uuid).getId() == null || this.findUserByUUID(uuid).getId().isEmpty()) {
            try {
                throw new UserRepresentationNotFoundException("User not found!");
            } catch (UserRepresentationNotFoundException e) {
                log.error("{}:deleteUserByUUID: {}", this.getClass().getCanonicalName(), e.getMessage());
            }
            return false;
        }
        Response.StatusType status = usersResource.delete(uuid).getStatusInfo();
        if (status.getStatusCode() == Response.Status.NO_CONTENT.getStatusCode()) {
            log.info("User has been removed ! \n");
        } else {
            log.error("Failed to remove user , Reason : {} \n", status.getReasonPhrase());
        }
        return status.getStatusCode() == Response.Status.NO_CONTENT.getStatusCode();
    }

    /**
     * Add roles to the Realm
     *
     * @param roles Roles to add
     * @return result of add operation  : success/failed
     */
    public boolean addRealmRoles(List<String> roles) {
        List<RoleRepresentation> realmRoles = new ArrayList<>();
        // TODO : Check realm role existence, otherwise create it or not ?
        // filter existing roles
        List<String> existingRoles = realmResource.roles()
                .list()
                .stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());
        List<String> rolesToAdded = roles.stream()
                .filter(r -> !existingRoles.contains(r))
                .collect(Collectors.toList());
        // add remaining ones
        List<RoleRepresentation> rolesRepresentationsToAdded = rolesToAdded.stream()
                .map(r -> {
                    RoleRepresentation rp = new RoleRepresentation();
                    rp.setName(r);
                    return rp;
                })
                .collect(Collectors.toList());
        try {
            rolesRepresentationsToAdded.stream()
                    .forEach(rp -> realmResource.roles().create(rp));
            log.info("Add Realm Roles Succeeded with roles : {} \n", roles);
        } catch (Exception e) {
            log.error("Add Realm Roles Failed, Cause : {} \n", e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Add roles to ClientApp
     *
     * @param clientId AppClient UUID
     * @param roles    Roles to add
     * @return result of add operation  : success/failed
     */
    public boolean addClientRoles(String clientId, List<String> roles) {
        if (!findClientAppByUUID(clientId)) return false;
        List<RoleRepresentation> clientRoles = new ArrayList<>();
        // TODO : Check client role existence, otherwise create it
        // filter existing roles
        List<String> existingRoles = realmResource.clients().get(clientId).roles()
                .list()
                .stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());
        List<String> rolesToAdded = roles.stream()
                .filter(r -> !existingRoles.contains(r))
                .collect(Collectors.toList());
        // add remaining ones
        List<RoleRepresentation> rolesRepresentationsToAdded = rolesToAdded.stream()
                .map(r -> {
                    RoleRepresentation rp = new RoleRepresentation();
                    rp.setName(r);
                    return rp;
                })
                .collect(Collectors.toList());
        try {
            rolesRepresentationsToAdded.stream()
                    .forEach(rp -> realmResource.clients().get(clientId).roles().create(rp));
            log.info("Add Client Roles Succeeded with roles : {} \n", rolesToAdded);
        } catch (Exception e) {
            log.error("Add Client Roles Failed, Cause : {} \n", e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Check if ClientApp exist
     *
     * @param clientId ClientApp UUID
     * @return
     */
    private boolean findClientAppByUUID(String clientId) {
        if (realmResource.clients().get(clientId) == null) {
            try {
                throw new ClientAppRepresentationNotFoundException(String.format("Client with UUID : %s not found!", clientId).toString());
            } catch (ClientAppRepresentationNotFoundException e) {
                log.error("{}:findClientAppByUUID {}", this.getClass().getCanonicalName(), e.getMessage());
            }
            return false;
        }
        return true;
    }
}
