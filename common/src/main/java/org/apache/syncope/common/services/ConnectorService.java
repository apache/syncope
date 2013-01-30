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

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.syncope.common.to.ConnBundleTO;
import org.apache.syncope.common.to.ConnInstanceTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.types.ConnConfProperty;

@Path("connectors")
public interface ConnectorService {

    /**
     * Creates a new connector instance.
     *
     * @param connectorTO Connector to be created.
     * @return Response containing URI location for created resource.
     */
    @POST
    Response create(ConnInstanceTO connectorTO);

    /**
     * @param connectorId Deletes connector with matching id.
     */
    @DELETE
    @Path("{connectorId}")
    void delete(@PathParam("connectorId") Long connectorId);

    /**
     * @param lang Language to select bundles from. Default language is English.
     * @return Returns known bundles in selected language.
     */
    @GET
    @Path("bundles")
    List<ConnBundleTO> getBundles(@QueryParam("lang") String lang);

    /**
     * @param connectorId ConnectorID to read configuration from.
     * @return Returns configuration for selected connector.
     */
    @GET
    @Path("{connectorId}/configuration")
    List<ConnConfProperty> getConfigurationProperties(@PathParam("connectorId") Long connectorId);

    /**
     * @param connectorId ConnectorID to be used for schema lookup.
     * @param connectorTO Connector object to provide special configuration properties.
     * @param showAll If set to true, all schema names will be shown, including special ones like '__PASSWORD__'.
     * Default is false.
     * @return Returns schema names for matching connector.
     */
    @POST
    @Path("{connectorId}/schemas")
    List<SchemaTO> getSchemaNames(@PathParam("connectorId") Long connectorId, ConnInstanceTO connectorTO,
            @QueryParam("showAll") @DefaultValue("false") boolean showAll);

    /**
     * @param lang Language to select connectors for. Default language is English.
     * @return Returns a list of all connectors with matching language.
     */
    @GET
    List<ConnInstanceTO> list(@QueryParam("lang") String lang);

    /**
     * @param connectorId ConnectorID to be read.
     * @return Returns connector with matching id.
     */
    @GET
    @Path("{connectorId}")
    ConnInstanceTO read(@PathParam("connectorId") Long connectorId);

    /**
     * @param resourceName Resource name to be used for connector lookup.
     * @return Returns connector for matching resourceName.
     */
    @GET
    ConnInstanceTO readConnectorBean(@MatrixParam("resourceName") String resourceName);

    /**
     * @param connectorId Overwrites connector with matching key.
     * @param connectorTO Connector to be stored.
     */
    @PUT
    @Path("{connectorId}")
    void update(@PathParam("connectorId") Long connectorId, ConnInstanceTO connectorTO);

    /**
     * @param connectorTO ConnectorTO to be used for connection check
     * @return True is connection could be established.
     */
    @POST
    @Path("check")
    boolean check(ConnInstanceTO connectorTO);

}
