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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.syncope.common.wrap.EntitlementTO;

/**
 * REST operations for entitlements.
 */
@Path("entitlements")
public interface EntitlementService extends JAXRSService {

    /**
     * Returns a list of all known entitlements.
     *
     * @return list of all known entitlements
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<EntitlementTO> getAllEntitlements();

    /**
     * Returns a list of entitlements assigned to user making the current request.
     *
     * @return list of entitlements assigned to user making the current request
     */
    @GET
    @Path("own")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<EntitlementTO> getOwnEntitlements();
}
