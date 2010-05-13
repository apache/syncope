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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/user/passwordReset/{userId}")
@Component
@Scope("request")
public class PasswordReset {

    final static Logger logger = Logger.getLogger(PasswordReset.class.getName());

    /**
     * TODO: call syncope-core
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getPasswordResetToken(@PathParam("userId") String userId,
            @QueryParam("passwordResetFormURL") String passwordResetFormURL) {

        if ("error".equals(userId)) {
            logger.severe("Entered in the error condition, going ahead...");

            throw new WebApplicationException(
                    new Exception("Wrong userId: " + userId));
        }

        logger.info("getPasswordResetToken called: " + passwordResetFormURL);

        return "passwordResetTokenId";
    }

    /**
     * TODO: call syncope-core
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String passwordReset(@QueryParam("tokenId") String tokenId,
            @PathParam("userId") String userId,
            @QueryParam("newPassword") String newPassword) {

        if ("error".equals(userId)) {
            logger.severe("Entered in the error condition, going ahead...");

            throw new WebApplicationException(
                    new Exception("Wrong userId: " + userId));
        }

        logger.info("passwordReset called: " + tokenId + " / " + newPassword);

        return Boolean.TRUE.toString();
    }
}
