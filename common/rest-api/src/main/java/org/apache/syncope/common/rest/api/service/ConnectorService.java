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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.ConnBundleTO;
import org.apache.syncope.common.lib.to.ConnIdObjectClassTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;

/**
 * REST operations for connector bundles and instances.
 */
@Path("connectors")
public interface ConnectorService extends JAXRSService {

    /**
     * Returns available connector bundles with property keys in selected language.
     *
     * @param lang language to select property keys; default language is English
     * @return available connector bundles with property keys in selected language
     */
    @GET
    @Path("bundles")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    List<ConnBundleTO> getBundles(@QueryParam("lang") String lang);

    /**
     * Builds the list of ConnId object classes information for the connector bundle matching the given connector
     * instance key, with the provided configuration.
     *
     * @param connInstanceTO connector instance object providing configuration properties
     * @param includeSpecial if set to true, special schema names (like '__PASSWORD__') will be included;
     * default is false
     * @return supported object classes info for the connector bundle matching the given connector instance key, with
     * the provided configuration
     */
    @POST
    @Path("{key}/supportedObjectClasses")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    List<ConnIdObjectClassTO> buildObjectClassInfo(
            @NotNull ConnInstanceTO connInstanceTO,
            @QueryParam("includeSpecial") @DefaultValue("false") boolean includeSpecial);

    /**
     * Returns connector instance with matching key.
     *
     * @param key connector instance key to be read
     * @param lang language to select property keys, null for default (English).
     * An ISO 639 alpha-2 or alpha-3 language code, or a language subtag up to 8 characters in length.
     * @return connector instance with matching key
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    ConnInstanceTO read(@NotNull @PathParam("key") String key, @QueryParam("lang") String lang);

    /**
     * Returns connector instance for matching resource.
     *
     * @param resourceName resource name to be used for connector lookup
     * @param lang language to select property keys, null for default (English).
     * An ISO 639 alpha-2 or alpha-3 language code, or a language subtag up to 8 characters in length.
     * @return connector instance for matching resource
     */
    @GET
    @Path("byResource/{resourceName}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    ConnInstanceTO readByResource(
            @NotNull @PathParam("resourceName") String resourceName, @QueryParam("lang") String lang);

    /**
     * Returns a list of all connector instances with property keys in the matching language.
     *
     * @param lang language to select property keys, null for default (English).
     * An ISO 639 alpha-2 or alpha-3 language code, or a language subtag up to 8 characters in length.
     * @return list of all connector instances with property keys in the matching language
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    List<ConnInstanceTO> list(@QueryParam("lang") String lang);

    /**
     * Creates a new connector instance.
     *
     * @param connInstanceTO connector instance to be created
     * @return Response object featuring Location header of created connector instance
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response create(@NotNull ConnInstanceTO connInstanceTO);

    /**
     * Updates the connector instance matching the provided key.
     *
     * @param connInstanceTO connector instance to be stored
     */
    @PUT
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void update(@NotNull ConnInstanceTO connInstanceTO);

    /**
     * Deletes the connector instance matching the provided key.
     *
     * @param key connector instance key to be deleted
     */
    @DELETE
    @Path("{key}")
    void delete(@NotNull @PathParam("key") String key);

    /**
     * Checks whether the connection to resource could be established.
     *
     * @param connInstanceTO connector instance to be used for connection check
     */
    @POST
    @Path("check")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void check(@NotNull ConnInstanceTO connInstanceTO);

    /**
     * Reload all connector bundles and instances.
     */
    @POST
    @Path("reload")
    void reload();
}
