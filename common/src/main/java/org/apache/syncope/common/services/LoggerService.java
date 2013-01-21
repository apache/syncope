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
import org.apache.syncope.common.types.AuditLoggerName;

import ch.qos.logback.classic.Level;

@Path("logger")
public interface LoggerService {

    //TODO use list(LoggerType) signature for both normal logger and audit logger instead of two different methods
    @GET
    List<LoggerTO> listLogs();

    @GET
    @Path("audit")
    List<AuditLoggerName> listAudits();

    @PUT
    @Path("{name}/level")
    LoggerTO update(@PathParam("name") String name, Level level);

    @DELETE
    @Path("{name}")
    LoggerTO delete(@PathParam("name") String name);

    // TODO refactor this method to use update()
    void enableAudit(AuditLoggerName auditLoggerName);

    // TODO refactor this method to use delete()
    void disableAudit(AuditLoggerName auditLoggerName);
}
