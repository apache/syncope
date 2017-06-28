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
package org.apache.syncope.common.rest.api.service;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.ext.PATCH;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.UserTO;

/**
 * REST operations for user self-management.
 */
@Path("users/self")
public interface UserSelfService extends JAXRSService {

    /**
     * Returns the user making the service call.
     *
     * @return calling user data, including owned entitlements as header value
     * {@link org.apache.syncope.common.rest.api.RESTHeaders#OWNED_ENTITLEMENTS}
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response read();

    /**
     * Self-registration for new user.
     *
     * @param userTO user to be created
     * @param storePassword whether password shall be stored internally
     * @return Response object featuring Location header of self-registered user as well as the user
     * itself - ProvisioningResult as Entity
     */
    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response create(@NotNull UserTO userTO,
            @DefaultValue("true") @QueryParam("storePassword") boolean storePassword);

    /**
     * Self-updates user.
     *
     * @param patch modification to be applied to self
     * @return Response object featuring the updated user - ProvisioningResult as Entity
     */
    @PATCH
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response update(@NotNull UserPatch patch);

    /**
     * Self-updates user.
     *
     * @param user complete update
     * @return Response object featuring the updated user - ProvisioningResult as Entity
     */
    @PUT
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response update(@NotNull UserTO user);

    /**
     * Self-deletes user.
     *
     * @return Response object featuring the deleted user - ProvisioningResult as Entity
     */
    @DELETE
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response delete();

    /**
     * Changes own password when change was forced by an administrator.
     *
     * @param password the password value to update
     *
     * @return Response object featuring the updated user - ProvisioningResult as Entity
     */
    @POST
    @Path("changePassword")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response changePassword(String password);

    /**
     * Provides answer for the security question configured for user matching the given username, if any.
     * If provided answer matches the one stored for that user, a password reset token is internally generated,
     * otherwise an error is returned.
     *
     * @param username username for which the security answer is provided
     * @param securityAnswer actual answer text
     */
    @POST
    @Path("requestPasswordReset")
    void requestPasswordReset(@NotNull @QueryParam("username") String username, String securityAnswer);

    /**
     * Reset the password value for the user matching the provided token, if available and still valid.
     * If the token actually matches one of users, and if it is still valid at the time of submission, the matching
     * user's password value is set as provided. The new password value will need anyway to comply with all relevant
     * password policies.
     *
     * @param token password reset token
     * @param password new password to be set
     */
    @POST
    @Path("confirmPasswordReset")
    void confirmPasswordReset(@NotNull @QueryParam("token") String token, String password);
}
