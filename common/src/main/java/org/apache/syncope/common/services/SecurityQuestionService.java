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

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.Descriptions;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.apache.syncope.common.to.SecurityQuestionTO;

/**
 * REST operations for configuration.
 */
@Path("securityQuestions")
public interface SecurityQuestionService extends JAXRSService {

    /**
     * Returns a list of all security questions.
     *
     * @return list of all security questions
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<SecurityQuestionTO> list();

    /**
     * Returns security question with matching id.
     *
     * @param securityQuestionId security question id to be read
     * @return security question with matching id
     */
    @GET
    @Path("{securityQuestionId}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    SecurityQuestionTO read(@NotNull @PathParam("securityQuestionId") Long securityQuestionId);

    /**
     * Creates a new security question.
     *
     * @param securityQuestionTO security question to be created
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created security question
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring <tt>Location</tt> header of created security question")
    })
    @POST
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response create(@NotNull SecurityQuestionTO securityQuestionTO);

    /**
     * Updates the security question matching the provided id.
     *
     * @param securityQuestionId security question id to be updated
     * @param securityQuestionTO security question to be stored
     */
    @PUT
    @Path("{securityQuestionId}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    void update(@NotNull @PathParam("securityQuestionId") Long securityQuestionId,
            @NotNull SecurityQuestionTO securityQuestionTO);

    /**
     * Deletes the security question matching the provided id.
     *
     * @param securityQuestionId security question id to be deleted
     */
    @DELETE
    @Path("{securityQuestionId}")
    void delete(@NotNull @PathParam("securityQuestionId") Long securityQuestionId);

    /**
     * Ask for security question configured for the user matching the given username, if any.
     *
     * @param username username for which the security question is requested
     * @return security question, if configured for the user matching the given username
     */
    @GET
    @Path("byUser/{username}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    SecurityQuestionTO readByUser(@NotNull @PathParam("username") String username);
}
