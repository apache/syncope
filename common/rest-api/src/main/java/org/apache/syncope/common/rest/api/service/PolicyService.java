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

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.policy.AbstractPolicyTO;
import org.apache.syncope.common.lib.types.PolicyType;

/**
 * REST operations for policies.
 */
@Path("policies")
public interface PolicyService extends JAXRSService {

    /**
     * Returns the policy matching the given key.
     *
     * @param key key of requested policy
     * @param <T> response type (extending PolicyTO)
     * @return policy with matching id
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    <T extends AbstractPolicyTO> T read(@NotNull @PathParam("key") String key);

    /**
     * Returns a list of policies of the matching type.
     *
     * @param type Type selector for requested policies
     * @param <T> response type (extending PolicyTO)
     * @return list of policies with matching type
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    <T extends AbstractPolicyTO> List<T> list(@NotNull @MatrixParam("type") PolicyType type);

    /**
     * Create a new policy.
     *
     * @param policyTO Policy to be created (needs to match type)
     * @return Response object featuring Location header of created policy
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response create(@NotNull AbstractPolicyTO policyTO);

    /**
     * Updates policy matching the given key.
     *
     * @param policyTO Policy to replace existing policy
     */
    @PUT
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void update(@NotNull AbstractPolicyTO policyTO);

    /**
     * Delete policy matching the given key.
     *
     * @param key key of policy to be deleted
     */
    @DELETE
    @Path("{key}")
    void delete(@NotNull @PathParam("key") String key);

}
