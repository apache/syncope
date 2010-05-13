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

import java.util.Collections;
import java.util.logging.Logger;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.syncope.rest.user.jaxb.AttributeValues;
import org.syncope.rest.user.jaxb.Attributes;

@Path("/user/read/{userId}")
@Component
@Scope("request")
public class Read {

    final static Logger logger = Logger.getLogger(Read.class.getName());

    public static Attributes getTestValue(String userId) {
        Attributes attributes = new Attributes();

        attributes.addUserAttribute("userId",
                new AttributeValues(Collections.singleton(userId)));

        return attributes;
    }

    /**
     * TODO: read actual values for the corresponding userId via syncope-core
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Attributes readUser(@PathParam("userId") String userId,
            @DefaultValue("FALSE") @QueryParam("test") boolean test) {

        if ("error".equals(userId)) {
            logger.severe("Entered in the error condition, going ahead...");

            throw new WebApplicationException(
                    new Exception("Wrong userId: " + userId));
        }

        if (test) {
            return getTestValue(userId);
        }

        logger.info("readUser called");

        Attributes attributes = new Attributes();

        attributes.addUserAttribute("userId",
                new AttributeValues(Collections.singleton(userId)));

        AttributeValues values1 = new AttributeValues("attribute1");
        values1.addAttributeValue("value1.1");
        values1.addAttributeValue("value1.2");
        attributes.addUserAttribute("attribute1", values1);

        AttributeValues values2 = new AttributeValues("attribute2");
        values2.addAttributeValue("value2.1");
        attributes.addUserAttribute("attribute2", values2);

        return attributes;
    }
}
