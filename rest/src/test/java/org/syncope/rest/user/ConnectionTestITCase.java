/*
 *  Copyright 2010 ilgrosso.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.rest.user;

import static org.junit.Assert.*;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.io.IOException;
import java.lang.String;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.syncope.rest.user.jaxb.Attributes;
import org.syncope.rest.user.jaxb.SearchParameters;
import org.syncope.rest.user.jaxb.SearchResults;

public class ConnectionTestITCase {

    final private static String userId = "test";
    final static Logger logger = Logger.getLogger(ConnectionTestITCase.class.getName());
    private static String BASE_URL;
    private static Client jerseyClient;

    static {
        URL fromMavenPropertiesURL =
                ConnectionTestITCase.class.getClassLoader().getResource("from_maven.properties");
        Properties fromMavenProperties = new Properties();
        try {
            fromMavenProperties.load(fromMavenPropertiesURL.openStream());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "When reading properties file", e);
        }

        String jettyPort = fromMavenProperties.getProperty("jetty.port", "8080");
        BASE_URL = "http://localhost:" + jettyPort + "/syncope-rest/user/";
    }

    @BeforeClass
    public static void setUp() throws Exception {
        jerseyClient = Client.create();
    }

    @AfterClass
    public static void tearDown() {
        jerseyClient.destroy();
    }

    @Test
    public void create() {
        WebResource webResource = jerseyClient.resource(
                BASE_URL + "create/" + userId + "?test=TRUE");

        Attributes userAttributes = new Attributes();
        userAttributes.addUserAttribute("userId", Collections.singleton(userId));

        String result = webResource.type(MediaType.APPLICATION_JSON).put(
                String.class, userAttributes);

        assertEquals(userId, result);
    }

    @Test
    public void delete() {
        WebResource webResource = jerseyClient.resource(
                BASE_URL + "delete/" + userId + "?test=TRUE");

        String result = webResource.delete(String.class);

        assertTrue(Boolean.valueOf(result));
    }

    @Test
    public void passwordReset() {
        WebResource webResource = jerseyClient.resource(
                BASE_URL + "passwordReset/" + userId
                + "?passwordResetFormURL=http%3A%2F%2Fcode.google.com"
                + "%2Fp%2Fsyncope%2F&gotoURL=http%3A%2F%2Fwww.google.com&test=TRUE");

        String reference1 = PasswordReset.getTestValue();
        String result1 = webResource.get(String.class);

        assertEquals(reference1, result1);

        MultivaluedMap formData = new MultivaluedMapImpl();
        formData.add("tokenId", result1);
        formData.add("newPassword", "newPassword");
        formData.add("test", "TRUE");

        webResource = jerseyClient.resource(
                BASE_URL + "passwordReset/" + userId);

        String result2 = webResource.type(
                MediaType.APPLICATION_FORM_URLENCODED).post(
                String.class, formData);

        assertTrue(Boolean.valueOf(result2));
    }

    @Test
    public void read() {
        WebResource webResource = jerseyClient.resource(
                BASE_URL + "read/" + userId + "?test=TRUE");

        Attributes reference = Read.getTestValue(userId);
        Attributes result = webResource.get(Attributes.class);

        assertEquals(reference, result);
    }

    @Test
    public void search() {
        WebResource webResource = jerseyClient.resource(
                BASE_URL + "search/?test=TRUE");

        SearchResults reference = Search.getTestValue();
        SearchResults result = webResource.type(MediaType.APPLICATION_JSON).post(
                SearchResults.class, new SearchParameters());

        assertEquals(reference, result);
    }

    @Test
    public void update() {
        WebResource webResource = jerseyClient.resource(
                BASE_URL + "update/" + userId + "?test=TRUE");

        Attributes userAttributes = new Attributes();
        userAttributes.addUserAttribute("userId", Collections.singleton(userId));

        String result = webResource.type(MediaType.APPLICATION_JSON).post(
                String.class, userAttributes);

        assertTrue(Boolean.valueOf(result));
    }
}
