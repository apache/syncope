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

import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.syncope.rest.user.jaxb.Attributes;

@Path("/user/update/${userId}")
@Component
@Scope("request")
public class Update {

    final static Logger logger = Logger.getLogger(Update.class.getName());

    /**
     * TODO: call syncope-core
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String updateUser(@PathParam("userId") String userId,
            Attributes userAttributes,
            @DefaultValue("FALSE") @QueryParam("test") boolean test) {

        if ("error".equals(userId)) {
            throw new WebApplicationException(
                    new Exception("Wrong userId: " + userId));
        }

        if (test) {
            return Boolean.TRUE.toString();
        }

        logger.info("searchUser() called with userId " + userId
                + " and attributes " + userAttributes);

        return Boolean.TRUE.toString();
    }
}
