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
import org.apache.syncope.common.to.CorrelationRuleClassTO;
import org.apache.syncope.common.to.AbstractPolicyTO;
import org.apache.syncope.common.types.PolicyType;

@Path("policies")
@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
public interface PolicyService {

    /**
     * @param policyTO Policy to be created (needs to match type)
     * @param <T> response type (extending PolicyTO)
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created policy
     */
    @POST
    <T extends AbstractPolicyTO> Response create(T policyTO);

    /**
     * @param policyId Deletes policy with given id
     * @param <T> response type (extending PolicyTO)
     */
    @DELETE
    @Path("{policyId}")
    <T extends AbstractPolicyTO> void delete(@PathParam("policyId") Long policyId);

    /**
     * @param type Type selector for requested policies
     * @param <T> response type (extending PolicyTO)
     * @return List of policies with matching type
     */
    @GET
    <T extends AbstractPolicyTO> List<T> list(@MatrixParam("type") PolicyType type);

    /**
     * @param policyId ID of requested policy
     * @param <T> response type (extending PolicyTO)
     * @return policy with matching id
     */
    @GET
    @Path("{policyId}")
    <T extends AbstractPolicyTO> T read(@PathParam("policyId") Long policyId);

    /**
     * @param type PolicyType to read global policy from
     * @param <T> response type (extending PolicyTO)
     * @return Global Policy for matching type
     */
    @GET
    @Path("global")
    <T extends AbstractPolicyTO> T readGlobal(@MatrixParam("type") PolicyType type);

    /**
     * @param policyId ID of policy to be updated
     * @param policyTO Policy to replace existing policy
     * @param <T> response type (extending PolicyTO)
     */
    @PUT
    @Path("{policyId}")
    <T extends AbstractPolicyTO> void update(@PathParam("policyId") Long policyId, T policyTO);

    /**
     * @return correlation rules java classes
     */
    @GET
    @Path("syncCorrelationRuleClasses")
    List<CorrelationRuleClassTO> getSyncCorrelationRuleClasses();
}
