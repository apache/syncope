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
package org.apache.syncope.core.rest;

import static org.apache.syncope.core.rest.AbstractTest.adminClient;
import static org.junit.Assert.assertEquals;

import java.util.List;
import org.apache.syncope.common.to.RouteTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AttributeSchemaType;
import org.apache.syncope.common.types.SchemaType;
import org.apache.syncope.common.types.SubjectType;
import org.apache.syncope.core.provisioning.camel.CamelDetector;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class CamelTestITCase extends AbstractTest{
    
    @Test
    public void isCamelEnabled(){
        assertEquals(CamelDetector.isCamelEnabledForUsers(),
                adminClient.isCamelEnabledFor(SubjectType.USER));
        
        assertEquals(CamelDetector.isCamelEnabledForRoles(),
                adminClient.isCamelEnabledFor(SubjectType.ROLE));
    }
    
    @Test
    public void userRoutes() {
        List<RouteTO> userRoutes = routeService.getRoutes(SubjectType.USER);
        Assert.assertNotNull(userRoutes);
        Assert.assertEquals(15, userRoutes.size());
        for(int i=0; i<userRoutes.size(); i++){
            Assert.assertNotNull(userRoutes.get(i).getRouteContent());
        }
    }
    
    @Test
    public void roleRoutes() {
        List<RouteTO> roleRoutes = routeService.getRoutes(SubjectType.ROLE);
        Assert.assertNotNull(roleRoutes);
        Assert.assertEquals(7, roleRoutes.size());
        for(int i=0; i<roleRoutes.size(); i++){
            Assert.assertNotNull(roleRoutes.get(i).getRouteContent());
        }
    }
   
    
    public RouteTO updateRoute(Long id, String content){
        RouteTO route = routeService.getRoute(SubjectType.USER, id);
        route.setRouteContent(content);
        routeService.importRoute(SubjectType.USER, route.getId(), route);
        //getting new route definition
        return routeService.getRoute(SubjectType.USER, id);        
    }
    
    @Test
    public void updateRoute(){
        RouteTO oldRoute = routeService.getRoute(SubjectType.USER, new Long(0));
        String routeContent =   "<route id=\"createUser\">\n" +
                                "  <from uri=\"direct:createUser\"/>\n" +
                                "  <setProperty propertyName=\"actual\">\n" +
                                "    <simple>${body}</simple>\n" +
                                "  </setProperty>\n" +
                                "  <doTry>\n" +
                                "    <bean ref=\"uwfAdapter\" method=\"create(${body},${property.disablePwdPolicyCheck},\n" +
                                "                             ${property.enabled},${property.storePassword})\"/>\n" +
                                "    <process ref=\"defaultUserCreatePropagation\" />\n" +
                                "    <to uri=\"direct:createPort\"/>\n" +
                                "    <to uri=\"log:myLog\"/>\n" +
                                "    <doCatch>        \n" +
                                "      <exception>java.lang.RuntimeException</exception>\n" +
                                "      <handled>\n" +
                                "        <constant>false</constant>\n" +
                                "      </handled>\n" +
                                "      <to uri=\"direct:createPort\"/>\n" +
                                "    </doCatch>\n" +
                                "  </doTry>\n" +
                                "</route>";
        RouteTO route =  updateRoute(new Long(0), routeContent);
        assertEquals(routeContent, route.getRouteContent());
        
        updateRoute(oldRoute.getId(), oldRoute.getRouteContent());
    }
    
    @Test
    public void checkRouteUpdate(){
        //Getting original route content
        RouteTO oldRoute = routeService.getRoute(SubjectType.USER, new Long(0));
        //updating route content including new attribute management
        String routeContent =   "<route id=\"createUser\">\n" +
                                "  <from uri=\"direct:createUser\"/>\n" +
                                "  <setProperty propertyName=\"actual\">\n" +
                                "    <simple>${body}</simple>\n" +
                                "  </setProperty>\n" +                                
                                "  <setBody>\n"+
                                "   <groovy>\n"+
                                "       request.body.getAttrs().get(3).getValues().set(0,\"true\")\n"+
                                "       return request.body\n"+
                                "   </groovy>\n"+
                                "  </setBody>\n"+
                                "  <doTry>\n" +
                                "      <bean ref=\"uwfAdapter\" method=\"create(${body},${property.disablePwdPolicyCheck},\n" +
                                "                            ${property.enabled},${property.storePassword})\"/>\n" +
                                "      <process ref=\"defaultUserCreatePropagation\" />\n" +
                                "      <to uri=\"direct:createPort\"/>\n" +
                                "      <doCatch>        \n" +
                                "      <exception>java.lang.RuntimeException</exception>\n" +
                                "          <handled>\n" +
                                "           <constant>false</constant>\n" +
                                "          </handled>\n" +
                                "      <to uri=\"direct:createPort\"/>\n" +
                                "      </doCatch>\n" +
                                "   </doTry>\n" +                                
                                "</route>";
        updateRoute(new Long(0), routeContent);
        //creating new schema attribute for user
        SchemaTO schemaTO = new SchemaTO();
        schemaTO.setName("camelAttribute");
        schemaTO.setType(AttributeSchemaType.String);       
        createSchema(AttributableType.USER, SchemaType.NORMAL, schemaTO);
        
        UserTO userTO = new UserTO();
        String userId = getUUIDString() + "camelUser@syncope.apache.org";
        userTO.setUsername(userId);
        userTO.setPassword("password");
        userTO.getAttrs().add(attributeTO("userId", userId));
        userTO.getAttrs().add(attributeTO("fullname", userId));
        userTO.getAttrs().add(attributeTO("surname", userId));
        userTO.getAttrs().add(attributeTO("camelAttribute", "false"));

        userTO = createUser(userTO);        
        Assert.assertNotNull(userTO);
        assertEquals("true",userTO.getAttrs().get(3).getValues().get(0));
        
        updateRoute(oldRoute.getId(), oldRoute.getRouteContent());
    }
    
}
