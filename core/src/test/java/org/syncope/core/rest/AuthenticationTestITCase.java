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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.CommonsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.NodeCond;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.RoleTO;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.to.UserTO;
import org.syncope.core.util.EntitlementUtil;
import org.syncope.types.SchemaType;

public class AuthenticationTestITCase extends AbstractTest {

    @Test
    public void testEntitlements() {
        ((CommonsClientHttpRequestFactory) restTemplate.getRequestFactory()).
                getHttpClient().getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("user1", "password"));

        String[] entsArray = restTemplate.getForObject(BASE_URL
                + "auth/entitlements.json", String[].class);
        Set<String> authEnts = new HashSet<String>(entsArray.length);
        authEnts.addAll(Arrays.asList(entsArray));

        // This call will return only the roles that the calling user
        // has right to administer
        List<RoleTO> roleTOs = Arrays.asList(restTemplate.getForObject(
                BASE_URL + "role/list.json", RoleTO[].class));
        assertNotNull(roleTOs);

        // reset admin credentials for restTemplate
        super.setupRestTemplate();

        Set<String> dbEnts = new HashSet<String>();
        dbEnts.add("base");
        dbEnts.add("advanced");
        dbEnts.add("SCHEMA_READ");
        dbEnts.add("USER_READ");
        dbEnts.add("USER_LIST");
        dbEnts.add("ROLE_LIST");
        dbEnts.add("WORKFLOW_FORM_CLAIM");
        for (RoleTO roleTO : roleTOs) {
            dbEnts.add(EntitlementUtil.getEntitlementName(roleTO.getId()));
        }

        assertEquals(authEnts, dbEnts);
    }

    @Test
    public void testUserSchemaAuthorization() {
        // 0. create a role that can only read schemas
        RoleTO authRoleTO = new RoleTO();
        authRoleTO.setName("authRole");
        authRoleTO.setParent(8L);
        authRoleTO.addEntitlement("SCHEMA_READ");

        authRoleTO = restTemplate.postForObject(BASE_URL + "role/create",
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
        UserTO userTO = UserTestITCase.getSampleTO("auth@test.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(authRoleTO.getId());
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        userTO = restTemplate.postForObject(
                BASE_URL + "user/create", userTO, UserTO.class);
        assertNotNull(userTO);

        // 3. read the schema created above (as admin) - success
        schemaTO = restTemplate.getForObject(BASE_URL
                + "schema/user/read/authTestSchema.json", SchemaTO.class);
        assertNotNull(schemaTO);

        // 4. read the schema created above (as user) - success
        ((CommonsClientHttpRequestFactory) restTemplate.getRequestFactory()).
                getHttpClient().getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(
                userTO.getUsername(), "password123"));

        schemaTO = restTemplate.getForObject(BASE_URL
                + "schema/user/read/authTestSchema.json", SchemaTO.class);
        assertNotNull(schemaTO);

        // 5. update the schema create above (as user) - failure
        HttpClientErrorException exception = null;
        try {
            restTemplate.postForObject(BASE_URL
                    + "schema/role/update", schemaTO, SchemaTO.class);
        } catch (HttpClientErrorException e) {
            exception = e;
        }
        assertNotNull(exception);
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());

        // reset admin credentials for restTemplate
        super.setupRestTemplate();

        userTO = restTemplate.getForObject(BASE_URL + "user/read/{userId}.json",
                UserTO.class, userTO.getId());

        assertNotNull(userTO);
        assertNotNull(userTO.getLastLoginDate());
        assertEquals(new Integer(0), userTO.getFailedLogins());
    }

    @Test
    public void testUserRead() {
        UserTO userTO = UserTestITCase.getSampleTO("testuserread@test.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        userTO = restTemplate.postForObject(
                BASE_URL + "user/create", userTO, UserTO.class);
        assertNotNull(userTO);

        ((CommonsClientHttpRequestFactory) restTemplate.getRequestFactory()).
                getHttpClient().getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(
                userTO.getUsername(), "password123"));

        UserTO readUserTO = restTemplate.getForObject(
                BASE_URL + "user/read/{userId}.json", UserTO.class, 1);
        assertNotNull(readUserTO);

        ((CommonsClientHttpRequestFactory) restTemplate.getRequestFactory()).
                getHttpClient().getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("user2", "password"));

        HttpClientErrorException exception = null;
        try {
            restTemplate.getForObject(
                    BASE_URL + "user/read/{userId}.json", UserTO.class, 1);
        } catch (HttpClientErrorException e) {
            exception = e;
        }
        assertNotNull(exception);
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        // reset admin credentials for restTemplate
        super.setupRestTemplate();
    }

    @Test
    public void testUserSearch() {
        UserTO userTO = UserTestITCase.getSampleTO("testusersearch@test.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        userTO = restTemplate.postForObject(BASE_URL + "user/create",
                userTO, UserTO.class);
        assertNotNull(userTO);

        ((CommonsClientHttpRequestFactory) restTemplate.getRequestFactory()).
                getHttpClient().getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(
                userTO.getUsername(), "password123"));

        AttributeCond isNullCond = new AttributeCond(
                AttributeCond.Type.ISNOTNULL);
        isNullCond.setSchema("loginDate");
        NodeCond searchCondition = NodeCond.getLeafCond(isNullCond);

        List<UserTO> matchedUsers = Arrays.asList(
                restTemplate.postForObject(BASE_URL + "user/search",
                searchCondition, UserTO[].class));
        assertNotNull(matchedUsers);
        assertFalse(matchedUsers.isEmpty());
        Set<Long> userIds = new HashSet<Long>(matchedUsers.size());
        for (UserTO user : matchedUsers) {
            userIds.add(user.getId());
        }
        assertTrue(userIds.contains(1L));

        ((CommonsClientHttpRequestFactory) restTemplate.getRequestFactory()).
                getHttpClient().getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("user2", "password"));

        matchedUsers = Arrays.asList(
                restTemplate.postForObject(BASE_URL + "user/search",
                searchCondition, UserTO[].class));
        assertNotNull(matchedUsers);
        userIds = new HashSet<Long>(matchedUsers.size());
        for (UserTO user : matchedUsers) {
            userIds.add(user.getId());
        }
        assertFalse(userIds.contains(1L));

        // reset admin credentials for restTemplate
        super.setupRestTemplate();
    }

    @Test
    public void checkFailedLogins() {
        UserTO userTO = UserTestITCase.getSampleTO("checkFailedLogin@test.org");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);
        AttributeTO testAttributeTO = new AttributeTO();
        testAttributeTO.setSchema("testAttribute");
        testAttributeTO.addValue("a value");
        membershipTO.addAttribute(testAttributeTO);
        userTO.addMembership(membershipTO);

        userTO = restTemplate.postForObject(
                BASE_URL + "user/create", userTO, UserTO.class);
        assertNotNull(userTO);

        ((CommonsClientHttpRequestFactory) restTemplate.getRequestFactory()).
                getHttpClient().getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(
                userTO.getUsername(), "password123"));

        UserTO readUserTO = restTemplate.getForObject(
                BASE_URL + "user/read/{userId}.json",
                UserTO.class, userTO.getId());

        assertNotNull(readUserTO);
        assertNotNull(readUserTO.getFailedLogins());
        assertEquals(new Integer(0), readUserTO.getFailedLogins());

        // authentications failed ...

        ((CommonsClientHttpRequestFactory) restTemplate.getRequestFactory()).
                getHttpClient().getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(
                userTO.getUsername(), "wrongpwd1"));

        Throwable t = null;

        try {
            restTemplate.getForObject(
                    BASE_URL + "user/read/{userId}.json",
                    UserTO.class, userTO.getId());
            assertNotNull(readUserTO);
        } catch (Exception e) {
            t = e;
        }

        assertNotNull(t);
        t = null;

        ((CommonsClientHttpRequestFactory) restTemplate.getRequestFactory()).
                getHttpClient().getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(
                userTO.getUsername(), "wrongpwd2"));

        try {
            restTemplate.getForObject(
                    BASE_URL + "user/read/{userId}.json",
                    UserTO.class, userTO.getId());
            assertNotNull(readUserTO);
        } catch (Exception e) {
            t = e;
        }

        // reset admin credentials for restTemplate
        super.setupRestTemplate();

        readUserTO = restTemplate.getForObject(
                BASE_URL + "user/read/{userId}.json",
                UserTO.class, userTO.getId());
        assertNotNull(readUserTO);
        assertNotNull(readUserTO.getFailedLogins());
        assertEquals(new Integer(2), readUserTO.getFailedLogins());

        ((CommonsClientHttpRequestFactory) restTemplate.getRequestFactory()).
                getHttpClient().getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(
                userTO.getUsername(), "password123"));

        readUserTO = restTemplate.getForObject(
                BASE_URL + "user/read/{userId}.json",
                UserTO.class, userTO.getId());
        assertNotNull(readUserTO);
        assertNotNull(readUserTO.getFailedLogins());
        assertEquals(new Integer(0), readUserTO.getFailedLogins());
    }
}
