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
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.syncope.common.to.LoggerTO;
import org.apache.syncope.common.types.LoggerType;

@Path("logger/{type}")
public interface LoggerService {

    @GET
    @Path("{name}")
    LoggerTO read(@PathParam("type") LoggerType type, @PathParam("name") final String name);

    @GET
    List<LoggerTO> list(@PathParam("type") LoggerType type);

    @PUT
    @Path("{name}/level")
    void update(@PathParam("type") LoggerType type, @PathParam("name") String name, LoggerTO logger);

    @DELETE
    @Path("{name}")
    void delete(@PathParam("type") LoggerType type, @PathParam("name") String name);

}
