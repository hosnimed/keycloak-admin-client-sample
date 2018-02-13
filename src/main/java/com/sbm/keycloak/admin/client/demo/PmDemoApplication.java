package com.sbm.keycloak.admin.client.demo;

import javafx.util.Pair;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.in;
import static java.lang.System.out;

@SpringBootApplication
public class PmDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(PmDemoApplication.class, args);



		// User "idm-admin" needs at least "manage-users, view-clients, view-realm, view-users" roles for "realm-management"
	/*
			Keycloak keycloak = Keycloak.getInstance(
				"http://localhost:8080/auth",
				"master", // the realm to log in to
				"admin", "admin",  // the user
				"security-admin-console");

	*/
	Keycloak keycloak = KeycloakService.getInstance();

			if(keycloak != null){
				out.println("Realm : "+	keycloak.realms().realm("demo").toString() );
			}


		// Get realm
		RealmResource realmResource = keycloak.realm(KeycloakService.realm);
		// Get users
		UsersResource usersResource = realmResource.users();


		// Define user
		long l = currentTimeMillis();
		CredentialRepresentation credential = new CredentialRepresentation();
		credential.setType(CredentialRepresentation.PASSWORD);
		credential.setValue("test_"+l);

		UserRepresentation user = new UserRepresentation();
		user.setUsername("test_user_"+l);
		user.setFirstName("Test");
		user.setLastName("User");
		user.setCredentials(Arrays.asList(credential));

		usersResource.create(user);
		StringBuilder builder = new StringBuilder();
		out.println("Users :: ");
		if(usersResource != null){
			usersResource.list()
					.stream()
					.map(ur -> new Pair(ur.getUsername(), ur.getId()))
					.filter(pair -> user.getUsername().equalsIgnoreCase(pair.getKey().toString()))
					.forEach(pair -> {
						out.printf("%s:%s \n",pair.getKey(),pair.getValue());
						builder.append(pair.getValue().toString());
					});
		}
		String createdUserId = builder.toString();

		// Create user (requires manage-users role)
		Response response = usersResource.create(user);
		if (response != null){
			response.getStringHeaders()
					.forEach((k,v)-> out.printf("K:%s \t V:%s \n",k,v));

		out.println("Repsonse Code: " + response.getStatusInfo());
		if(response.getStatusInfo().getFamily().equals(Response.Status.CREATED)	){
			out.println("Repsonse Location: " +response. getLocation());
		}
		}
		String userId = (response.getLocation()==null) ? createdUserId :response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
		out.printf("User created with userId: %s \n", userId);

		//Update existing user
		UserResource userResource = realmResource.users().get(userId);
		UserRepresentation userToUpdate = userResource.toRepresentation();
		userToUpdate.setEnabled(true);
		userToUpdate.setUsername(userToUpdate.getUsername()+"_updated");
		userToUpdate.setFirstName(userToUpdate.getFirstName()+"_updated");
		userToUpdate.setLastName(userToUpdate.getLastName()+"_updated");
		userToUpdate.setEmail(userToUpdate.getEmail()+"_updated");

		// Define password credential
		CredentialRepresentation passwordCred = new CredentialRepresentation();
		passwordCred.setTemporary(false);
		passwordCred.setType(CredentialRepresentation.PASSWORD);
		passwordCred.setValue("test");
		// Set password credential
		userResource.resetPassword(passwordCred);

		// Update User
		userResource.update(userToUpdate);
		userResource = realmResource.users().get(userId);
		UserRepresentation userAfterUpdate = userResource.toRepresentation();
		out.printf("User updated with basic infos ! %s \n",PmDemoApplication.toString(userAfterUpdate));

		// Get realm role "tester" (requires view-realm role)
		RoleRepresentation testerRealmRole = realmResource.roles()//
							.get("tester").toRepresentation();

		// Assign realm role tester to user
		userResource.roles().realmLevel() //
				.add(Arrays.asList(testerRealmRole));

		// Get client
		ClientRepresentation app1Client = realmResource.clients() //
				.findByClientId("demo-client").get(0);

		// Get client level role (requires view-clients role)
		RoleRepresentation userClientRole = realmResource.clients().get(app1Client.getId()) //
				.roles().get("user").toRepresentation();

		// Assign client level role to user
		userResource.roles() //
				.clientLevel(app1Client.getId()).add(Arrays.asList(userClientRole));

		// Update User
		userResource.update(userToUpdate);
		userAfterUpdate = realmResource.users().get(userId).toRepresentation();
		List<RoleRepresentation> realmRoles = realmResource.users().get(userId).roles().realmLevel().listEffective();
		List<RoleRepresentation> clientsRoles = realmResource.users().get(userId).roles().clientLevel(app1Client.getId()).listAll();

		out.printf("User updated with roles ! %s \n",PmDemoApplication.toString(userResource.toRepresentation()));
		out.printf("Realm Roles %s \n", realmRoles);
		out.printf("Client Roles %s \n", clientsRoles);

		// Delete User
		Scanner sc =new Scanner(in);
		sc.next();
		userResource.remove();
		out.printf("User has been removed ! \n");

	}


	private static String toString(UserRepresentation userRepresentation) {
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append("Username:");
		builder.append(userRepresentation.getUsername());
		builder.append(",Firstname:");
		builder.append(userRepresentation.getFirstName());
		builder.append(",Lastname:");
		builder.append(userRepresentation.getLastName());
		builder.append(",Email:");
		builder.append(userRepresentation.getEmail());
		if(userRepresentation.getRealmRoles() !=null ) {
			builder.append(",Realms Roles :");
			builder.append(userRepresentation.getRealmRoles().toString());
		}
		if(userRepresentation.getClientRoles() != null){
			builder.append(",Clients Roles:");
			builder.append(userRepresentation.getClientRoles().toString());
		}
		builder.append("}");

		return builder.toString();
	}
}
