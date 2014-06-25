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
import javax.ws.rs.MatrixParam;
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
import org.apache.syncope.common.wrap.CorrelationRuleClass;
import org.apache.syncope.common.to.AbstractPolicyTO;
import org.apache.syncope.common.types.PolicyType;

/**
 * REST operations for policies.
 */
@Path("policies")
public interface PolicyService extends JAXRSService {

    /**
     * Returns a list of classes to be used as correlation rules.
     *
     * @return list of classes to be used as correlation rules
     */
    @GET
    @Path("syncCorrelationRuleClasses")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<CorrelationRuleClass> getSyncCorrelationRuleClasses();

    /**
     * Returns the policy matching the given id.
     *
     * @param policyId id of requested policy
     * @param <T> response type (extending PolicyTO)
     * @return policy with matching id
     */
    @GET
    @Path("{policyId}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    <T extends AbstractPolicyTO> T read(@NotNull @PathParam("policyId") Long policyId);

    /**
     * Returns the global policy for the given type.
     *
     * @param type PolicyType to read global policy from
     * @param <T> response type (extending PolicyTO)
     * @return global policy for matching type
     */
    @GET
    @Path("global")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    <T extends AbstractPolicyTO> T readGlobal(@NotNull @MatrixParam("type") PolicyType type);

    /**
     * Returns a list of policies of the matching type.
     *
     * @param type Type selector for requested policies
     * @param <T> response type (extending PolicyTO)
     * @return list of policies with matching type
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    <T extends AbstractPolicyTO> List<T> list(@NotNull @MatrixParam("type") PolicyType type);

    /**
     * Create a new policy.
     *
     * @param policyTO Policy to be created (needs to match type)
     * @param <T> response type (extending PolicyTO)
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created policy
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE, value = "Featuring <tt>Location</tt> header of created policy")
    })
    @POST
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    <T extends AbstractPolicyTO> Response create(@NotNull T policyTO);

    /**
     * Updates policy matching the given id.
     *
     * @param policyId id of policy to be updated
     * @param policyTO Policy to replace existing policy
     * @param <T> response type (extending PolicyTO)
     */
    @PUT
    @Path("{policyId}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    <T extends AbstractPolicyTO> void update(@NotNull @PathParam("policyId") Long policyId, @NotNull T policyTO);

    /**
     * Delete policy matching the given id.
     *
     * @param policyId id of policy to be deleted
     * @param <T> response type (extending PolicyTO)
     */
    @DELETE
    @Path("{policyId}")
    <T extends AbstractPolicyTO> void delete(@NotNull @PathParam("policyId") Long policyId);

}
