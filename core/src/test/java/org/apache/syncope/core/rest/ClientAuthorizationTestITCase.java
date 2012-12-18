package org.apache.syncope.core.rest;

import java.net.URI;

import javax.ws.rs.core.Response;

import junit.framework.Assert;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.HttpStatus;
import org.apache.syncope.to.RoleTO;
import org.junit.Before;
import org.junit.Test;

public class ClientAuthorizationTestITCase extends AbstractTest {
	@Override
	@Before
	public void setupService() {
	}

	@Test
	/**
	 *  Test checks if response has WWW-Authenticate header by unathorized request
	 */
    public void unauthorizedAccessResponseHeader() {
        RoleTO roleTO = new RoleTO();
        roleTO.setName("test");
        URI baseUri = restClientFactory.createWebClient().getBaseURI();
    	WebClient webClient = WebClient.create(baseUri).path("roles");
    	Response response = webClient.post(roleTO);
    	Assert.assertNotNull(response);
    	Assert.assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
    	Assert.assertNotNull(response.getHeaderString("WWW-Authenticate"));
    }

}
