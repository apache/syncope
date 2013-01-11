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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.apache.syncope.client.to.ConnBundleTO;
import org.apache.syncope.client.to.ConnInstanceTO;
import org.apache.syncope.types.ConnConfProperty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Path("connectors")
public interface ConnectorService {

    @POST
    ConnInstanceTO create(ConnInstanceTO connectorTO);

    @DELETE
    @Path("{connectorId}")
    ConnInstanceTO delete(@PathParam("connectorId") Long connectorId);

    @GET
    @Path("bundles")
    List<ConnBundleTO> getBundles(@QueryParam("lang") String lang);

    @GET
    @Path("{connectorId}/configuration")
    List<ConnConfProperty> getConfigurationProperties(
            @PathParam("connectorId") Long connectorId);

    @GET
    @POST
    @Path("{connectorId}/schemas")
    List<String> getSchemaNames(
            @PathParam("connectorId") Long connectorId,
            ConnInstanceTO connectorTO,
            @QueryParam("showall") @DefaultValue("false") boolean showall);

    @GET
    List<ConnInstanceTO> list(@QueryParam("lang") String lang);

    @GET
    @Path("{connectorId}")
    ConnInstanceTO read(@PathParam("connectorId") Long connectorId);

    @GET
    @RequestMapping(method = RequestMethod.GET, value = "/{resourceName}/connectorBean")
    ConnInstanceTO readConnectorBean(
            @MatrixParam("resourceName") String resourceName);

    @PUT
    @Path("{connectorId}")
    ConnInstanceTO update(@PathParam("connectorId") Long connectorId,
            ConnInstanceTO connectorTO);

    @POST
    @Path("validate")
    @RequestMapping(method = RequestMethod.POST, value = "/check")
    boolean validate(ConnInstanceTO connectorTO);
}
