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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.syncope.client.to.LoggerTO;
import org.apache.syncope.types.AuditLoggerName;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ch.qos.logback.classic.Level;

@Path("logger")
public interface LoggerService {

    @GET
    @RequestMapping(method = RequestMethod.GET, value = "/log/list")
    List<LoggerTO> listLogs();

    @GET
    @Path("audit")
    @RequestMapping(method = RequestMethod.GET, value = "/audit/list")
    List<AuditLoggerName> listAudits();

    @PUT
    @Path("{name}/level")
    @RequestMapping(method = RequestMethod.POST, value = "/log/{name}/{level}")
    LoggerTO setLogLevel(@PathParam("name") final String name, final Level level);

    @DELETE
    @Path("{name}")
    @RequestMapping(method = RequestMethod.GET, value = "/log/delete/{name}")
    LoggerTO deleteLog(@PathParam("name") final String name);

    /**
     * @deprecated Refactoring needed here. Use {@link #setLogLevel(String, Level)} after refactoring is done.
     */
    @Deprecated
    @RequestMapping(method = RequestMethod.PUT, value = "/audit/enable")
    void enableAudit(final AuditLoggerName auditLoggerName);

    /**
     * @deprecated Refactoring needed here. Use {@link #deleteLog(String)} after refactoring is done.
     */
    @Deprecated
    @RequestMapping(method = RequestMethod.PUT, value = "/audit/disable")
    void disableAudit(final AuditLoggerName auditLoggerName);

}