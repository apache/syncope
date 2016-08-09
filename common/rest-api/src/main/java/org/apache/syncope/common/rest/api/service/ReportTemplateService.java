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

import java.io.InputStream;
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
import org.apache.syncope.common.lib.to.ReportTemplateTO;
import org.apache.syncope.common.lib.types.ReportTemplateFormat;

/**
 * REST operations for report templates.
 */
@Path("reportTemplates")
public interface ReportTemplateService extends JAXRSService {

    /**
     * Returns a list of all report templates.
     *
     * @return list of all report templates.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    List<ReportTemplateTO> list();

    /**
     * Creates a new report template.
     *
     * @param reportTemplateTO Creates a new report template.
     * @return Response object featuring Location header of created report template
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response create(@NotNull ReportTemplateTO reportTemplateTO);

    /**
     * Returns report template with matching key.
     *
     * @param key key of report template to be read
     * @return report template with matching key
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    ReportTemplateTO read(@NotNull @PathParam("key") String key);

    /**
     * Deletes the report template matching the given key.
     *
     * @param key key for report template to be deleted
     */
    @DELETE
    @Path("{key}")
    void delete(@NotNull @PathParam("key") String key);

    /**
     * Gets the template for the given key and format, if available.
     *
     * @param key report template
     * @param format template format
     * @return report template with matching key and format, if available
     */
    @GET
    @Path("{key}/{format}")
    Response getFormat(
            @NotNull @PathParam("key") String key,
            @NotNull @PathParam("format") ReportTemplateFormat format);

    /**
     * Sets the template for the given key and format, if available.
     *
     * @param key report template
     * @param format template format
     * @param templateIn template to be set
     */
    @PUT
    @Path("{key}/{format}")
    void setFormat(
            @NotNull @PathParam("key") String key,
            @NotNull @PathParam("format") ReportTemplateFormat format,
            InputStream templateIn);

    /**
     * Removes the template for the given key and format, if available.
     *
     * @param key report template
     * @param format template format
     */
    @DELETE
    @Path("{key}/{format}")
    void removeFormat(
            @NotNull @PathParam("key") String key,
            @NotNull @PathParam("format") ReportTemplateFormat format);

}
