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
package org.apache.syncope.fit.server.reference;

import static org.apache.syncope.fit.server.reference.AbstractITCase.syncopeService;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.apache.syncope.common.lib.to.CamelRouteTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.SubjectType;
import org.junit.Assume;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class CamelRouteITCase extends AbstractITCase {

    @Test
    public void userRoutes() {
        Assume.assumeTrue(CamelDetector.isCamelEnabledForUsers(syncopeService));

        List<CamelRouteTO> userRoutes = camelRouteService.list(SubjectType.USER);
        assertNotNull(userRoutes);
        assertEquals(15, userRoutes.size());
        for (CamelRouteTO route : userRoutes) {
            assertNotNull(route.getContent());
        }
    }

    @Test
    public void roleRoutes() {
        Assume.assumeTrue(CamelDetector.isCamelEnabledForRoles(syncopeService));

        List<CamelRouteTO> roleRoutes = camelRouteService.list(SubjectType.ROLE);
        assertNotNull(roleRoutes);
        assertEquals(7, roleRoutes.size());
        for (CamelRouteTO route : roleRoutes) {
            assertNotNull(route.getContent());
        }
    }

    private CamelRouteTO doUpdate(final String key, String content) {
        CamelRouteTO route = camelRouteService.read(key);
        route.setContent(content);
        camelRouteService.update(route.getKey(), route);
        //getting new route definition
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
                + "    <process ref=\"userCreateProcessor\" />\n"
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
        //updating route content including new attribute management
        String routeContent = "<route id=\"createUser\">\n"
                + "  <from uri=\"direct:createUser\"/>\n"
                + "  <setProperty propertyName=\"actual\">\n"
                + "    <simple>${body}</simple>\n"
                + "  </setProperty>\n"
                + "  <setBody>\n"
                + "   <groovy>\n"
                + "       request.body.getPlainAttrs().get(3).getValues().set(0,\"true\")\n"
                + "       return request.body\n"
                + "   </groovy>\n"
                + "  </setBody>\n"
                + "  <doTry>\n"
                + "      <bean ref=\"uwfAdapter\" method=\"create(${body},${property.disablePwdPolicyCheck},\n"
                + "                            ${property.enabled},${property.storePassword})\"/>\n"
                + "      <process ref=\"userCreateProcessor\" />\n"
                + "      <to uri=\"direct:createPort\"/>\n"
                + "      <doCatch>        \n"
                + "      <exception>java.lang.RuntimeException</exception>\n"
                + "          <handled>\n"
                + "           <constant>false</constant>\n"
                + "          </handled>\n"
                + "      <to uri=\"direct:createPort\"/>\n"
                + "      </doCatch>\n"
                + "   </doTry>\n"
                + "</route>";
        try {
            doUpdate("createUser", routeContent);

            //creating new schema attribute for user
            PlainSchemaTO schemaTO = new PlainSchemaTO();
            schemaTO.setKey("camelAttribute");
            schemaTO.setType(AttrSchemaType.String);
            createSchema(AttributableType.USER, SchemaType.PLAIN, schemaTO);

            UserTO userTO = new UserTO();
            String userId = getUUIDString() + "camelUser@syncope.apache.org";
            userTO.setUsername(userId);
            userTO.setPassword("password");
            userTO.getPlainAttrs().add(attrTO("userId", userId));
            userTO.getPlainAttrs().add(attrTO("fullname", userId));
            userTO.getPlainAttrs().add(attrTO("surname", userId));
            userTO.getPlainAttrs().add(attrTO("camelAttribute", "false"));

            userTO = createUser(userTO);
            assertNotNull(userTO);
            assertEquals("true", userTO.getPlainAttrs().get(3).getValues().get(0));
        } finally {
            doUpdate(oldRoute.getKey(), oldRoute.getContent());
        }
    }

}
