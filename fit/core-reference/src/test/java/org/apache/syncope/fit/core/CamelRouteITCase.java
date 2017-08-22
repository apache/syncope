/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.fit.core;

import org.apache.syncope.fit.CamelDetector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.CamelRouteTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class CamelRouteITCase extends AbstractITCase {

    @Test
    public void userRoutes() {
        Assume.assumeTrue(CamelDetector.isCamelEnabledForUsers(syncopeService));

        List<CamelRouteTO> userRoutes = camelRouteService.list(AnyTypeKind.USER);
        assertNotNull(userRoutes);
        assertEquals(16, userRoutes.size());
        userRoutes.forEach(route -> assertNotNull(route.getContent()));
    }

    @Test
    public void groupRoutes() {
        Assume.assumeTrue(CamelDetector.isCamelEnabledForGroups(syncopeService));

        List<CamelRouteTO> groupRoutes = camelRouteService.list(AnyTypeKind.GROUP);
        assertNotNull(groupRoutes);
        assertEquals(8, groupRoutes.size());
        groupRoutes.forEach(route -> assertNotNull(route.getContent()));
    }

    private CamelRouteTO doUpdate(final String key, final String content) {
        CamelRouteTO route = camelRouteService.read(key);
        route.setContent(content);
        camelRouteService.update(route);
        // getting new route definition
        return camelRouteService.read(key);
    }

    @Test
    public void update() {
        Assume.assumeTrue(CamelDetector.isCamelEnabledForUsers(syncopeService));

        CamelRouteTO oldRoute = camelRouteService.read("createUser");
        assertNotNull(oldRoute);
        String routeContent = "<route id=\"createUser\">\n"
                + "  <from uri=\"direct:createUser\"/>\n"
                + "  <setProperty propertyName=\"actual\">\n"
                + "    <simple>${body}</simple>\n"
                + "  </setProperty>\n"
                + "  <doTry>\n"
                + "    <bean ref=\"uwfAdapter\" method=\"create(${body},${property.disablePwdPolicyCheck},\n"
                + "                             ${property.enabled},${property.storePassword})\"/>\n"
                + "    <to uri=\"propagate:create?anyTypeKind=USER\"/>\n"
                + "    <to uri=\"direct:createPort\"/>\n"
                + "    <to uri=\"log:myLog\"/>\n"
                + "    <doCatch>        \n"
                + "      <exception>java.lang.RuntimeException</exception>\n"
                + "      <handled>\n"
                + "        <constant>false</constant>\n"
                + "      </handled>\n"
                + "      <to uri=\"direct:createPort\"/>\n"
                + "    </doCatch>\n"
                + "  </doTry>\n"
                + "</route>";
        try {
            CamelRouteTO route = doUpdate("createUser", routeContent);
            assertEquals(routeContent, route.getContent());
        } finally {
            doUpdate(oldRoute.getKey(), oldRoute.getContent());
        }
    }

    @Test
    public void scriptingUpdate() {
        Assume.assumeTrue(CamelDetector.isCamelEnabledForUsers(syncopeService));

        CamelRouteTO oldRoute = camelRouteService.read("createUser");
        // updating route content including new attribute management

        String routeContent = ""
                + "  <route id=\"createUser\">\n"
                + "    <from uri=\"direct:createUser\"/>\n"
                + "    <setProperty propertyName=\"actual\">\n"
                + "      <simple>${body}</simple>\n"
                + "    </setProperty>\n"
                + "    <setBody>\n"
                + "     <groovy>\n"
                + "request.body.getPlainAttr(\"camelAttribute\").get().getValues().set(0,\"true\")\n"
                + "       return request.body\n"
                + "     </groovy>\n"
                + "    </setBody>\n"
                + "    <doTry>\n"
                + "      <bean ref=\"uwfAdapter\" method=\"create(${body},${property.disablePwdPolicyCheck},\n"
                + "                                     ${property.enabled},${property.storePassword})\"/>\n"
                + "      <to uri=\"propagate:create?anyTypeKind=USER\"/>\n"
                + "      <to uri=\"direct:createPort\"/>\n"
                + "      <doCatch>        \n"
                + "        <exception>java.lang.RuntimeException</exception>\n"
                + "        <handled>\n"
                + "          <constant>false</constant>\n"
                + "        </handled>\n"
                + "        <to uri=\"direct:createPort\"/>\n"
                + "      </doCatch>\n"
                + "    </doTry>\n"
                + "  </route> ";
        try {
            doUpdate("createUser", routeContent);

            // creating new schema attribute for user
            PlainSchemaTO schemaTO = new PlainSchemaTO();
            schemaTO.setKey("camelAttribute");
            schemaTO.setType(AttrSchemaType.String);
            createSchema(SchemaType.PLAIN, schemaTO);

            AnyTypeClassTO typeClass = new AnyTypeClassTO();
            typeClass.setKey("camelAttribute");
            typeClass.getPlainSchemas().add(schemaTO.getKey());
            anyTypeClassService.create(typeClass);

            UserTO userTO = new UserTO();
            userTO.setRealm(SyncopeConstants.ROOT_REALM);
            userTO.getAuxClasses().add(typeClass.getKey());
            String userId = getUUIDString() + "camelUser@syncope.apache.org";
            userTO.setUsername(userId);
            userTO.setPassword("password123");
            userTO.getPlainAttrs().add(attrTO("userId", userId));
            userTO.getPlainAttrs().add(attrTO("fullname", userId));
            userTO.getPlainAttrs().add(attrTO("surname", userId));
            userTO.getPlainAttrs().add(attrTO("camelAttribute", "false"));

            userTO = createUser(userTO).getEntity();
            assertNotNull(userTO);
            assertEquals("true", userTO.getPlainAttr("camelAttribute").get().getValues().get(0));
        } finally {
            doUpdate(oldRoute.getKey(), oldRoute.getContent());
        }
    }

    @Test
    public void issueSYNCOPE931() {
        Assume.assumeTrue(CamelDetector.isCamelEnabledForUsers(syncopeService));

        CamelRouteTO oldRoute = camelRouteService.read("createUser");
        assertNotNull(oldRoute);
        String routeContent = "<route id=\"createUser\">\n"
                + "  <from uri=\"direct:createUser\"/>\n"
                + "  <setProperty propertyName=\"actual\">\n"
                + "    <simple>${body}</simple>\n"
                + "  </setProperty>\n"
                + "  <doTry>\n"
                + "    <bean ref=\"uwfAdapter\" method=\"create(${body},${property.disablePwdPolicyCheck},\n"
                + "                             ${property.enabled},${property.storePassword})\"/>\n"
                + "    <to uri=\"propagate:create123?anyTypeKind=USER\"/>\n"
                + "    <to uri=\"direct:createPort\"/>\n"
                + "    <to uri=\"log:myLog\"/>\n"
                + "    <doCatch>        \n"
                + "      <exception>java.lang.RuntimeException</exception>\n"
                + "      <handled>\n"
                + "        <constant>false</constant>\n"
                + "      </handled>\n"
                + "      <to uri=\"direct:createPort\"/>\n"
                + "    </doCatch>\n"
                + "  </doTry>\n"
                + "</route>";

        // Try to update a route with an incorrect propagation type
        try {
            doUpdate("createUser", routeContent);
            Assert.fail("Error expected on an incorrect propagation type");
        } catch (Exception ex) {
            // Expected
        }

        // Now update the route again with the correct propagation type
        routeContent = routeContent.replaceFirst("create123", "create");
        try {
            CamelRouteTO route = doUpdate("createUser", routeContent);
            assertEquals(routeContent, route.getContent());
        } finally {
            doUpdate(oldRoute.getKey(), oldRoute.getContent());
        }
    }

}
