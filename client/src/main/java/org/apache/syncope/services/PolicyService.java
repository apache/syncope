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
package org.apache.syncope.services;

import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import org.apache.syncope.client.to.PolicyTO;
import org.apache.syncope.types.PolicyType;

@Path("policies")
public interface PolicyService {

    @POST
    <T extends PolicyTO> T create(T policyTO);

    // TODO: policyClass is required only for Spring RestTemplate mock. Must be removed for CXF
    @DELETE
    @Path("{policyId}")
    <T extends PolicyTO> T delete(@PathParam("policyId") Long policyId, Class<T> policyClass);

    @GET
    @Path("{kind}")
    <T extends PolicyTO> List<T> listByType(@PathParam("kind") PolicyType type);

    // TODO: policyClass is required only for Spring RestTemplate mock. Must be removed for CXF
    @GET
    @Path("{policyId}")
    <T extends PolicyTO> T read(@PathParam("policyId") Long policyId, Class<T> policyClass);

    // TODO: policyClass is required only for Spring RestTemplate mock. Must be removed for CXF
    @GET
    @Path("global/{kind}")
    <T extends PolicyTO> T readGlobal(@PathParam("kind") PolicyType type, Class<T> policyClass);

    @PUT
    @Path("{policyId}")
    <T extends PolicyTO> T update(@PathParam("policyId") Long policyId, T policyTO);
}
