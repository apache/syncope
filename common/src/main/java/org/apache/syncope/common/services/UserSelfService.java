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
package org.apache.syncope.common.services;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.Descriptions;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.UserTO;

/**
 * REST operations for user self-management.
 */
@Path("users/self")
public interface UserSelfService extends JAXRSService {

    /**
     * Checks whether self-registration is allowed.
     *
     * @return <tt>Response</tt> contains special Syncope HTTP header indicating if user self registration is allowed
     * @see org.apache.syncope.common.types.RESTHeaders#SELFREGISTRATION_ALLOWED
     */
    @Descriptions({
        @Description(target = DocTarget.RETURN,
                value = "<tt>Response</tt> contains special Syncope HTTP header indicating if user self registration "
                + "is allowed")
    })
    @OPTIONS
    Response getOptions();

    /**
     * Returns the user making the service call.
     *
     * @return calling user data
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    UserTO read();

    /**
     * Self-registration for new user.
     *
     * @param userTO user to be created
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of self-registered user as well as the user
     * itself - {@link UserTO} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RETURN,
                value = "<tt>Response</tt> object featuring <tt>Location</tt> header of self-registered user as well "
                + "as the user itself - {@link UserTO} as <tt>Entity</tt>")
    })
    @POST
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response create(UserTO userTO);

    /**
     * Self-updates user.
     *
     * @param userId id of user to be updated
     * @param userMod modification to be applied to user matching the provided userId
     * @return <tt>Response</tt> object featuring the updated user - {@link UserTO} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RETURN,
                value = "<tt>Response</tt> object featuring the updated user - <tt>UserTO</tt> as <tt>Entity</tt>")
    })
    @POST
    @Path("{userId}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response update(@PathParam("userId") Long userId, UserMod userMod);

    /**
     * Self-deletes user.
     *
     * @return <tt>Response</tt> object featuring the deleted user - {@link UserTO} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RETURN,
                value = "<tt>Response</tt> object featuring the deleted user - <tt>UserTO</tt> as <tt>Entity</tt>")
    })
    @DELETE
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response delete();

}
