package com.sbm.keycloak.admin.client.demo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AdminClientDemoApplicationTests {

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


		UserRepresentation userAfterCreation = adminClient.addUserWithCredentials(userToBeCreated, passwordCred);
		assertNotNull(userAfterCreation.getId());
		//Todo : check created user credentials
//		assertNotNull(userAfterCreation.getCredentials().get(0));

		assertUserRepresentationEquals(userToBeCreated, userAfterCreation);
	}

	@Test
	public void should_retrieve_existing_user(){
		// Define user
		UserRepresentation userToBeCreated = new UserRepresentation();
		long l = currentTimeMillis();
		userToBeCreated.setUsername("test_user_");
		userToBeCreated.setFirstName("Test");
		userToBeCreated.setLastName("User");
		// Create a User
		UserRepresentation userAfterAdded = adminClient.addUserWithCredentials(userToBeCreated, null);
		// Get user
		UserRepresentation userRetrieved = adminClient.findUserByUUID(userAfterAdded.getId());

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
        adminClient.addUserWithCredentials(userToBeCreated, null);
        // Update user
        UserRepresentation userToBeUpdated = new UserRepresentation();
        userToBeUpdated.setFirstName("Test_Updated");
        userToBeUpdated.setLastName("User_Updated");
        UserRepresentation userAfterUpdate = adminClient.updateUserWithCredentials(userToBeUpdated, null);
        //Check
        assertUserRepresentationEquals(userToBeUpdated, userAfterUpdate);
	}

	@Test
	public void should_remove_existing_user(){


	}

	@Test
	public void should_throw_exception_when_removing_unexisting_user(){

	}
	private void assertUserRepresentationEquals(UserRepresentation userToBeCreated, UserRepresentation userAfterCreation) {
		assertNotNull("Expected returned user is null!", userAfterCreation);
		assertEquals(userToBeCreated.getUsername(), userAfterCreation.getUsername());
		assertEquals(userToBeCreated.getFirstName(), userAfterCreation.getFirstName());
		assertEquals(userToBeCreated.getLastName(), userAfterCreation.getLastName());
		assertEquals(userToBeCreated.getEmail(), userAfterCreation.getEmail());
		//Todo : enhance credentials comparison
//		assertThat(userToBeCreated.getCredentials(), CoreMatchers.hasItem(userAfterCreation.getCredentials().get(0)) );
	}



}
