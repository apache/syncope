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
     * Create a new connector instance.
     *
     * @param connInstanceTO connector instance to be created
     * @return response containing URI location for created resource
     */
    @POST
    Response create(ConnInstanceTO connInstanceTO);

    /**
     * @param connInstanceId connector instance id to be deleted
     */
    @DELETE
    @Path("{connInstanceId}")
    void delete(@PathParam("connInstanceId") Long connInstanceId);

    /**
     * @param lang language to select property keys; default language is English
     * @return available connector bundles with property keys in selected language
     */
    @GET
    @Path("bundles")
    List<ConnBundleTO> getBundles(@QueryParam("lang") String lang);

    /**
     * @param connInstanceId connector instance id to read configuration from
     * @return configuration for selected connector instance
     */
    @GET
    @Path("{connInstanceId}/configuration")
    List<ConnConfProperty> getConfigurationProperties(@PathParam("connInstanceId") Long connInstanceId);

    /**
     * @param connInstanceId connector instance id to be used for schema lookup
     * @param connInstanceTO connector instance object to provide special configuration properties
     * @param showAll if set to true, special schema names (like '__PASSWORD__') will be included; default is false
     * @return schema names for connector bundle matching the given connector instance id
     */
    @POST
    @Path("{connInstanceId}/schemas")
    List<SchemaTO> getSchemaNames(@PathParam("connInstanceId") Long connInstanceId, ConnInstanceTO connInstanceTO,
            @QueryParam("showAll") @DefaultValue("false") boolean showAll);

    /**
     * @param lang language to select property keys; default language is English
     * @return list of all connector instances with property keys in the matching language
     */
    @GET
    List<ConnInstanceTO> list(@QueryParam("lang") String lang);

    /**
     * @param connInstanceId connector instance id to be read
     * @return connector instance with matching id
     */
    @GET
    @Path("{connInstanceId}")
    ConnInstanceTO read(@PathParam("connInstanceId") Long connInstanceId);

    /**
     * @param resourceName resource name to be used for connector lookup
     * @return connector instance for matching resource
     */
    @GET
    ConnInstanceTO readByResource(@MatrixParam("resourceName") String resourceName);

    /**
     * @param connInstanceId connector instance id to be updated
     * @param connInstaceTO connector instance to be stored
     */
    @PUT
    @Path("{connInstanceId}")
    void update(@PathParam("connInstanceId") Long connInstanceId, ConnInstanceTO connInstaceTO);

    /**
     * @param connInstaceTO connector instance to be used for connection check
     * @return true if connection could be established
     */
    @POST
    @Path("check")
    boolean check(ConnInstanceTO connInstaceTO);

    /**
     * Reload all connector bundles and instances.
     */
    @POST
    @Path("reload")
    void reload();
}