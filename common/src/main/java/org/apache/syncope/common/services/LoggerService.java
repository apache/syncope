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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.syncope.common.to.EventCategoryTO;
import org.apache.syncope.common.to.LoggerTO;
import org.apache.syncope.common.types.LoggerType;

/**
 * REST operations for logging and auditing.
 */
@Path("logger")
public interface LoggerService extends JAXRSService {

    /**
     * Returns a list of all managed events in audit.
     *
     * @return list of all managed events in audit
     */
    @GET
    @Path("events")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<EventCategoryTO> events();

    /**
     * Returns logger with matching type and name.
     *
     * @param type LoggerType to be selected.
     * @param name Logger name to be read
     * @return logger with matching type and name
     */
    @GET
    @Path("{type}/{name}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    LoggerTO read(@PathParam("type") LoggerType type, @PathParam("name") final String name);

    /**
     * Returns a list of loggers with matching type.
     *
     * @param type LoggerType to be selected
     * @return list of loggers with matching type
     */
    @GET
    @Path("{type}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<LoggerTO> list(@PathParam("type") LoggerType type);

    /**
     * Creates or updates (if existing) the logger with matching name.
     *
     * @param type LoggerType to be selected
     * @param name Logger name to be updated
     * @param logger Logger to be created or updated
     */
    @PUT
    @Path("{type}/{name}/level")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    void update(@PathParam("type") LoggerType type, @PathParam("name") String name, LoggerTO logger);

    /**
     * Deletes the logger with matching name.
     *
     * @param type LoggerType to be selected
     * @param name Logger name to be deleted
     */
    @DELETE
    @Path("{type}/{name}")
    void delete(@PathParam("type") LoggerType type, @PathParam("name") String name);

}
