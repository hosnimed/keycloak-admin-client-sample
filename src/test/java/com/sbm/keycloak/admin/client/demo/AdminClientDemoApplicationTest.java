package com.sbm.keycloak.admin.client.demo;

import com.sbm.keycloak.admin.client.demo.exceptions.KCServerConnexionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


import java.util.Arrays;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest

public class AdminClientDemoApplicationTest {

	private static final Logger log = LoggerFactory.getLogger(AdminClientDemoApplicationTest.class);

	KeycloakAdminClientDemo adminClient = null;
	@Before
	public void setup(){
		adminClient = new KeycloakAdminClientDemo();
	}

	@Test
	public void contextLoads() {
	}

	@Test
	public void should_create_new_user(){
		long l = currentTimeMillis();
		// Define password credential
		CredentialRepresentation passwordCred = new CredentialRepresentation();
		passwordCred.setValue("test");

		// Define user
		UserRepresentation userToBeCreated = new UserRepresentation();
		userToBeCreated.setUsername("test_user_"+l);
		userToBeCreated.setFirstName("Test");
		userToBeCreated.setLastName("User");
        // Add a user
		UserRepresentation userAfterCreation = adminClient.addUserWithCredentials(userToBeCreated, passwordCred);
		// Check
		assertNotNull(userAfterCreation.getId());
		//Todo : check created user credential
        // 		assertNotNull(userAfterCreation.getCredentials().get(0));

		assertUserRepresentationEquals(userToBeCreated, userAfterCreation);
	}

	@Test
	public void should_retrieve_existing_user(){
		// Define user
		UserRepresentation userToBeCreated = new UserRepresentation();
		long l = currentTimeMillis();
		// Create a User
        userToBeCreated.setUsername("test_user_"+l);
        userToBeCreated.setFirstName("Test");
		userToBeCreated.setLastName("User");
        UserRepresentation userAfterAdded = adminClient.addUserWithCredentials(userToBeCreated, null);
		// Get user
		UserRepresentation userRetrieved = adminClient.findUserByUUID(userAfterAdded.getId());
        // Check
		assertUserRepresentationEquals(userAfterAdded, userRetrieved);
	}

	@Test
	public void should_update_existing_user(){
        long l = currentTimeMillis();
        // Define user
        UserRepresentation userToBeCreated = new UserRepresentation();
        // Create a User
        userToBeCreated.setUsername("test_user_"+l);
        userToBeCreated.setFirstName("Test");
		userToBeCreated.setLastName("User");
		UserRepresentation userToBeUpdated = adminClient.addUserWithCredentials(userToBeCreated, null);
        // Update user
        userToBeUpdated.setFirstName("Test_Updated");
        userToBeUpdated.setLastName("User_Updated");
        UserRepresentation userAfterUpdate = adminClient.updateUserWithCredentials(userToBeUpdated, null);
        //Check
		assertNotNull("Expected returned user is null!", userAfterUpdate);
		assertEquals(userToBeCreated.getUsername(), userAfterUpdate.getUsername());
		assertEquals(userAfterUpdate.getFirstName(), "Test_Updated");
		assertEquals(userAfterUpdate.getLastName(), "User_Updated");
	}

	@Test
	public void should_remove_existing_user(){
        // Define user
        UserRepresentation userToBeCreated = new UserRepresentation();
        long l = currentTimeMillis();
        // Create a User
        userToBeCreated.setUsername("test_user_"+l);
        userToBeCreated.setFirstName("Test");
        userToBeCreated.setLastName("User");
        UserRepresentation userAfterAdded = adminClient.addUserWithCredentials(userToBeCreated, null);
	    //Delete a user
        assertTrue("User has been removed!", adminClient.deleteUserByUUID(userAfterAdded.getId()));
	}

	/**
	 * TODO : change to should_throw_exception_when_removing_unexisting_user
 	 */
	@Test
	public void should_return_false_when_removing_unexisting_user(){
		// Define user
		UserRepresentation userToBeCreated = new UserRepresentation();
		long l = currentTimeMillis();
		// Create a User
		userToBeCreated.setUsername("test_user_"+l);
		userToBeCreated.setFirstName("Test");
		userToBeCreated.setLastName("User");
		UserRepresentation userAfterAdded = adminClient.addUserWithCredentials(userToBeCreated, null);
		//Delete a user
		assertTrue("User has been removed", adminClient.deleteUserByUUID(userAfterAdded.getId()));
		// Check
		assertFalse(adminClient.deleteUserByUUID(userAfterAdded.getId()));
		// Java 8 exception assertion, standard style ...
//		Assertions.assertThatExceptionThrownBy(() -> {throw new UserRepresentationNotFoundException("User not found!") ; }).hasMessage("User not found");
	}

	/**
	 * TODO : change to AssertAdminEvent in Keycloak TestSuite
	 */
	@Test
	public void should_add_realm_role(){
		long l = currentTimeMillis();
		//Define a realm role
		String role = "test_realm_role_"+l;
		// Check
//		assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.roleResourcePath("role-a"), role, ResourceType.REALM_ROLE);
		assertTrue(adminClient.addRealmRoles(Arrays.asList(role)));

	}

	@Test
	public void should_add_client_role(){
		long l = currentTimeMillis();
		String clientId = "demo-client";
		// Retrieve a client id
		String id = adminClient.realmResource.clients().findByClientId(clientId).get(0).getId();
		//	check uuid
		assertNotNull("Client UUID is null",id);
		//Define a client role
		String role = "test_client_role_"+l;
		// Check
		assertTrue(adminClient.addClientRoles(id,Arrays.asList(role)));
	}


	@Test
	public void should_add_realm_role_to_user(){
		// Define user
		UserRepresentation userToBeCreated = new UserRepresentation();
		long l = currentTimeMillis();
		// Create a User
		userToBeCreated.setUsername("test_user_"+l);
		userToBeCreated.setFirstName("Test");
		userToBeCreated.setLastName("User");
		UserRepresentation userAfterAdded = adminClient.addUserWithCredentials(userToBeCreated, null);
		//Define a realm role
		String role = "test_realm_role_"+l;
		// Add Realm Role
		adminClient.addRealmRoles(Arrays.asList(role));

		// Check
		//roles before update
		int sizeBefore = adminClient.realmResource.roles().get(role).getRoleUserMembers().size();
		//roles after update
		adminClient.addRealmRolesToUser(userAfterAdded.getId(), Arrays.asList(role));
		int sizeAfter = adminClient.realmResource.roles().get(role).getRoleUserMembers().size();
		assertTrue(sizeBefore < sizeAfter);
	}

	@Test
	public void should_add_client_role_to_user(){
		// Define user
		UserRepresentation userToBeCreated = new UserRepresentation();
		long l = currentTimeMillis();
		// Create a User
		userToBeCreated.setUsername("test_user_"+l);
		userToBeCreated.setFirstName("Test");
		userToBeCreated.setLastName("User");
		UserRepresentation userAfterAdded = adminClient.addUserWithCredentials(userToBeCreated, null);
		// Define client id
		String clientId = "demo-client";
		// Retrieve a client uuid
		String id = adminClient.realmResource.clients().findByClientId(clientId).get(0).getId();
		//Define a client role
		String role = "test_client_role_"+l;
		// Add Client Role
		adminClient.addClientRoles(id, Arrays.asList(role));

		// Check
		//roles before update
		int sizeBefore = adminClient.realmResource.clients().get(id).roles().get(role).getRoleUserMembers().size();
		//roles after update
		adminClient.addClientRolesToUser(userAfterAdded.getId(), id, Arrays.asList(role));
		int sizeAfter = adminClient.realmResource.clients().get(id).roles().get(role).getRoleUserMembers().size();
		assertTrue(sizeBefore < sizeAfter);


	}

	private void assertUserRepresentationEquals(UserRepresentation expectedUser, UserRepresentation actualUser) {
		assertNotNull("Expected returned user is null!", actualUser);
		assertEquals(expectedUser.getUsername(), actualUser.getUsername());
		assertEquals(expectedUser.getFirstName(), actualUser.getFirstName());
		assertEquals(expectedUser.getLastName(), actualUser.getLastName());
		assertEquals(expectedUser.getEmail(), actualUser.getEmail());
		//Todo : enhance credentials comparison
//		assertThat(userToBeCreated.getCredentials(), CoreMatchers.hasItem(userAfterCreation.getCredentials().get(0)) );
	}



}
