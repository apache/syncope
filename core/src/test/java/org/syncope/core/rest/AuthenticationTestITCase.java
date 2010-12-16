/*
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
package org.syncope.core.rest;

import static org.junit.Assert.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.CommonsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.RoleTO;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.to.UserTO;
import org.syncope.types.SchemaType;

public class AuthenticationTestITCase extends AbstractTest {

    private UserTO getSampleTO(final String email) {
        UserTO userTO = new UserTO();
        userTO.setPassword("password");

        AttributeTO usernameTO = new AttributeTO();
        usernameTO.setSchema("username");
        usernameTO.addValue(email);
        userTO.addAttribute(usernameTO);

        AttributeTO firstnameTO = new AttributeTO();
        firstnameTO.setSchema("firstname");
        firstnameTO.addValue(email);
        userTO.addAttribute(firstnameTO);

        AttributeTO surnameTO = new AttributeTO();
        surnameTO.setSchema("surname");
        surnameTO.addValue("Surname");
        userTO.addAttribute(surnameTO);

        AttributeTO typeTO = new AttributeTO();
        typeTO.setSchema("type");
        typeTO.addValue("a type");
        userTO.addAttribute(typeTO);

        AttributeTO userIdTO = new AttributeTO();
        userIdTO.setSchema("userId");
        userIdTO.addValue(email);
        userTO.addAttribute(userIdTO);

        AttributeTO emailTO = new AttributeTO();
        emailTO.setSchema("email");
        emailTO.addValue(email);
        userTO.addAttribute(emailTO);

        AttributeTO loginDateTO = new AttributeTO();
        loginDateTO.setSchema("loginDate");
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        loginDateTO.addValue(sdf.format(new Date()));
        userTO.addAttribute(loginDateTO);

        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        userTO.addAttribute(testAttributeTO);

        return userTO;
    }

    @Test
    public void testEntitlements() {
        ((CommonsClientHttpRequestFactory) restTemplate.getRequestFactory()).
                getHttpClient().getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("1", "password"));

        List<String> authEnts = Arrays.asList(
                restTemplate.getForObject(BASE_URL
                + "auth/entitlements.json", String[].class));

        List<String> dbEnts = new ArrayList<String>();
        dbEnts.add("base");
        dbEnts.add("advanced");
        dbEnts.add("SCHEMA_READ");
        assertEquals(dbEnts, authEnts);

        // reset admin credentials for restTemplate
        super.setupRestTemplate();
    }

    @Test
    public void testAuthorization() {
        // 0. create a role that can only read schemas
        RoleTO authRoleTO = new RoleTO();
        authRoleTO.setName("authRole");
        authRoleTO.setParent(8L);
        authRoleTO.addEntitlement("SCHEMA_READ");

        authRoleTO = restTemplate.postForObject(
                BASE_URL + "role/create",
                authRoleTO, RoleTO.class);
        assertNotNull(authRoleTO);

        // 1. create a schema (as admin)
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("authTestSchema");
        schemaTO.setMandatoryCondition("false");
        schemaTO.setType(SchemaType.String);

        SchemaTO newSchemaTO = restTemplate.postForObject(BASE_URL
                + "schema/user/create", schemaTO, SchemaTO.class);
        assertEquals(schemaTO, newSchemaTO);

        // 2. create an user with the role created above (as admin)
        UserTO userTO = getSampleTO("auth@test.org");
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(authRoleTO.getId());
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        userTO = restTemplate.postForObject(
                BASE_URL + "user/create",
                userTO, UserTO.class);
        assertNotNull(userTO);

        // 3. read the schema created above (as admin) - success
        schemaTO = restTemplate.getForObject(BASE_URL
                + "schema/user/read/authTestSchema.json", SchemaTO.class);
        assertNotNull(schemaTO);

        // 4. read the schema created above (as user) - success
        ((CommonsClientHttpRequestFactory) restTemplate.getRequestFactory()).
                getHttpClient().getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(
                String.valueOf(userTO.getId()), "password"));

        schemaTO = restTemplate.getForObject(BASE_URL
                + "schema/user/read/authTestSchema.json", SchemaTO.class);
        assertNotNull(schemaTO);

        // 5. update the schema create above (as user) - failure
        HttpClientErrorException exception = null;
        try {
            schemaTO.setVirtual(true);
            restTemplate.postForObject(BASE_URL
                    + "schema/role/update", schemaTO, SchemaTO.class);
        } catch (HttpClientErrorException e) {
            exception = e;
        }
        assertNotNull(exception);
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        // reset admin credentials for restTemplate
        super.setupRestTemplate();
    }
}
