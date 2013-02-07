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
import java.util.Set;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.to.ConfigurationTO;
import org.apache.syncope.common.to.MailTemplateTO;
import org.apache.syncope.common.to.ValidatorTO;

@Path("configurations")
public interface ConfigurationService {

    /**
     * Creates a new configuration element.
     *
     * @param configurationTO Configuration to be stored.
     * @return Response containing URI location for created resource.
     */
    @POST
    Response create(ConfigurationTO configurationTO);

    /**
     * @return Returns configuration as an downloadable content.xml database export file.
     */
    @GET
    @Path("all/export")
    Response dbExport();

    /**
     * @param key Deletes configuration with matching key.
     */
    @DELETE
    @Path("{key}")
    void delete(@PathParam("key") String key);

    /**
     * @return Returns a list of known mail-template names.
     */
    @GET
    @Path("mailTemplates")
    Set<MailTemplateTO> getMailTemplates();

    /**
     * @return Returns a list of known validator names.
     */
    @GET
    @Path("validators")
    Set<ValidatorTO> getValidators();

    /**
     * @return Returns a list of all configuration elements.
     */
    @GET
    List<ConfigurationTO> list();

    /**
     * @param key Identifier of configuration to be read.
     * @return Returns configuration element with matching key.
     */
    @GET
    @Path("{key}")
    ConfigurationTO read(@PathParam("key") String key);

    /**
     * @param key Overwrites configuration element with matching key.
     * @param configurationTO New configuration
     */
    @PUT
    @Path("{key}")
    void update(@PathParam("key") String key, ConfigurationTO configurationTO);
}
