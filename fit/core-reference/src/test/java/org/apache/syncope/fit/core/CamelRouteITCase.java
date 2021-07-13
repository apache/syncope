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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.CamelRouteTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CamelRouteITCase extends AbstractITCase {

    @BeforeEach
    public void check() {
        assumeTrue(adminClient.platform().getProvisioningInfo().getUserProvisioningManager().contains("Camel"));
    }

    @Test
    public void userRoutes() {
        List<CamelRouteTO> userRoutes = camelRouteService.list(AnyTypeKind.USER);
        assertNotNull(userRoutes);
        assertEquals(16, userRoutes.size());
        userRoutes.forEach(route -> assertNotNull(route.getContent()));
    }

    @Test
    public void groupRoutes() {
        List<CamelRouteTO> groupRoutes = camelRouteService.list(AnyTypeKind.GROUP);
        assertNotNull(groupRoutes);
        assertEquals(8, groupRoutes.size());
        groupRoutes.forEach(route -> assertNotNull(route.getContent()));
    }

    private static CamelRouteTO doUpdate(final AnyTypeKind anyTypeKind, final String key, final String content) {
        CamelRouteTO route = camelRouteService.read(anyTypeKind, key);
        route.setContent(content);
        camelRouteService.update(anyTypeKind, route);
        // getting new route definition
        return camelRouteService.read(anyTypeKind, key);
    }

    @Test
    public void update() {
        CamelRouteTO oldRoute = camelRouteService.read(AnyTypeKind.USER, "createUser");
        assertNotNull(oldRoute);
        String routeContent = "<route id=\"createUser\">\n"
                + "  <from uri=\"direct:createUser\"/>\n"
                + "  <setProperty name=\"actual\">\n"
                + "    <simple>${body}</simple>\n"
                + "  </setProperty>\n"
                + "  <doTry>\n"
                + "    <bean ref=\"uwfAdapter\" method=\"create(${body},${exchangeProperty.disablePwdPolicyCheck},\n"
                + "                             ${exchangeProperty.enabled})\"/>\n"
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
            CamelRouteTO route = doUpdate(AnyTypeKind.USER, "createUser", routeContent);
            assertEquals(routeContent, route.getContent());
        } finally {
            doUpdate(AnyTypeKind.USER, oldRoute.getKey(), oldRoute.getContent());
        }
    }

    @Test
    public void scriptingUpdate() {
        CamelRouteTO oldRoute = camelRouteService.read(AnyTypeKind.USER, "createUser");
        // updating route content including new attribute management

        String routeContent = ""
                + "  <route id=\"createUser\">\n"
                + "    <from uri=\"direct:createUser\"/>\n"
                + "    <setProperty name=\"actual\">\n"
                + "      <simple>${body}</simple>\n"
                + "    </setProperty>\n"
                + "    <setBody>\n"
                + "     <groovy>\n"
                + "request.body.getPlainAttr(\"camelAttribute\").get().getValues().set(0,\"true\")\n"
                + "       return request.body\n"
                + "     </groovy>\n"
                + "    </setBody>\n"
                + "    <doTry>\n"
                + "      <bean ref=\"uwfAdapter\" method=\"create(${body},${exchangeProperty.disablePwdPolicyCheck},\n"
                + "                                     ${exchangeProperty.enabled})\"/>\n"
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
            doUpdate(AnyTypeKind.USER, "createUser", routeContent);

            // creating new schema attribute for user
            PlainSchemaTO schemaTO = new PlainSchemaTO();
            schemaTO.setKey("camelAttribute");
            schemaTO.setType(AttrSchemaType.String);
            createSchema(SchemaType.PLAIN, schemaTO);

            AnyTypeClassTO typeClass = new AnyTypeClassTO();
            typeClass.setKey("camelAttribute");
            typeClass.getPlainSchemas().add(schemaTO.getKey());
            anyTypeClassService.create(typeClass);

            UserCR userCR = new UserCR();
            userCR.setRealm(SyncopeConstants.ROOT_REALM);
            userCR.getAuxClasses().add(typeClass.getKey());
            String userId = getUUIDString() + "camelUser@syncope.apache.org";
            userCR.setUsername(userId);
            userCR.setPassword("password123");
            userCR.getPlainAttrs().add(attr("userId", userId));
            userCR.getPlainAttrs().add(attr("fullname", userId));
            userCR.getPlainAttrs().add(attr("surname", userId));
            userCR.getPlainAttrs().add(attr("camelAttribute", "false"));

            UserTO userTO = createUser(userCR).getEntity();
            assertNotNull(userTO);
            assertEquals("true", userTO.getPlainAttr("camelAttribute").get().getValues().get(0));
        } finally {
            doUpdate(AnyTypeKind.USER, oldRoute.getKey(), oldRoute.getContent());
        }
    }

    @Test
    public void issueSYNCOPE931() {
        CamelRouteTO oldRoute = camelRouteService.read(AnyTypeKind.USER, "createUser");
        assertNotNull(oldRoute);
        String routeContent = "<route id=\"createUser\">\n"
                + "  <from uri=\"direct:createUser\"/>\n"
                + "  <setProperty name=\"actual\">\n"
                + "    <simple>${body}</simple>\n"
                + "  </setProperty>\n"
                + "  <doTry>\n"
                + "    <bean ref=\"uwfAdapter\" method=\"create(${body},${exchangeProperty.disablePwdPolicyCheck},\n"
                + "                             ${exchangeProperty.enabled})\"/>\n"
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
            doUpdate(AnyTypeKind.USER, "createUser", routeContent);
            fail("Error expected on an incorrect propagation type");
        } catch (Exception ex) {
            // Expected
        }

        // Now update the route again with the correct propagation type
        routeContent = routeContent.replaceFirst("create123", "create");
        try {
            CamelRouteTO route = doUpdate(AnyTypeKind.USER, "createUser", routeContent);
            assertEquals(routeContent, route.getContent());
        } finally {
            doUpdate(AnyTypeKind.USER, oldRoute.getKey(), oldRoute.getContent());
        }
    }
}
