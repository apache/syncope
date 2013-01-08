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

import org.apache.syncope.client.to.AbstractSchemaTO;

@Path("schemas")
public interface SchemaService {

    @POST
    //    @RequestMapping(method = RequestMethod.POST, value = "/{kind}/create")
    <T extends AbstractSchemaTO> T create(@PathParam("kind") final String kind, final T schemaTO);

    @DELETE
    @Path("{kind}/{schema}")
    //    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/delete/{schema}")
    <T extends AbstractSchemaTO> T delete(@PathParam("kind") final String kind,
            @PathParam("schema") final String schemaName, final Class<T> type);

    @GET
    @Path("{kind}")
    //    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/list")
    <T extends AbstractSchemaTO> List<T> list(@PathParam("kind") final String kind, final Class<T[]> type);

    @GET
    @Path("{kind}/{schema}")
    //    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/read/{schema}")
    <T extends AbstractSchemaTO> T read(@PathParam("kind") final String kind,
            @PathParam("schema") final String schemaName, final Class<T> type);

    @PUT
    @Path("{kind}/{schema}")
    //    @RequestMapping(method = RequestMethod.POST, value = "/{kind}/update")
    <T extends AbstractSchemaTO> T update(@PathParam("kind") final String kind,
            @PathParam("schema") final String schemaName, final T schemaTO);

}