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
package org.syncope.rest.user;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/user/delete/{userId}")
@Component
@Scope("request")
public class Delete {

    private static final Logger log = LoggerFactory.getLogger(Delete.class);

    /**
     * TODO: call syncope-core
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public String deleteUser(@PathParam("userId") String userId,
            @DefaultValue("FALSE") @QueryParam("test") boolean test) {

        if ("error".equals(userId)) {
            throw new WebApplicationException(
                    new Exception("Wrong userId: " + userId));
        }

        if (test) {
            return Boolean.TRUE.toString();
        }

        log.info("deleteUser() called for " + userId);

        return Boolean.TRUE.toString();
    }
}
