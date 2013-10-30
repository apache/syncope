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
package org.apache.syncope.common.types;

import javax.ws.rs.core.Response;

public enum ClientExceptionType {

    AssociatedResources(Response.Status.BAD_REQUEST),
    Composite(Response.Status.BAD_REQUEST),
    DataIntegrityViolation(Response.Status.BAD_REQUEST),
    EntityExists(Response.Status.CONFLICT),
    GenericPersistence(Response.Status.BAD_REQUEST),
    InvalidLogger(Response.Status.BAD_REQUEST),
    InvalidConnInstance(Response.Status.BAD_REQUEST),
    InvalidConnIdConf(Response.Status.BAD_REQUEST),
    InvalidPasswordPolicy(Response.Status.BAD_REQUEST),
    InvalidAccountPolicy(Response.Status.BAD_REQUEST),
    InvalidSyncPolicy(Response.Status.BAD_REQUEST),
    InvalidSyncopeRole(Response.Status.BAD_REQUEST),
    InvalidReportExec(Response.Status.BAD_REQUEST),
    InvalidRoles(Response.Status.BAD_REQUEST),
    InvalidSchemaDefinition(Response.Status.BAD_REQUEST),
    InvalidSearchCondition(Response.Status.BAD_REQUEST),
    InvalidPropagationTaskExecReport(Response.Status.BAD_REQUEST),
    InvalidUSchema(Response.Status.BAD_REQUEST),
    InvalidUDerSchema(Response.Status.BAD_REQUEST),
    InvalidUVirSchema(Response.Status.BAD_REQUEST),
    InvalidRSchema(Response.Status.BAD_REQUEST),
    InvalidRDerSchema(Response.Status.BAD_REQUEST),
    InvalidRVirSchema(Response.Status.BAD_REQUEST),
    InvalidMSchema(Response.Status.BAD_REQUEST),
    InvalidMDerSchema(Response.Status.BAD_REQUEST),
    InvalidMVirSchema(Response.Status.BAD_REQUEST),
    InvalidSchemaMapping(Response.Status.BAD_REQUEST),
    InvalidSyncopeConf(Response.Status.BAD_REQUEST),
    InvalidSyncopeUser(Response.Status.BAD_REQUEST),
    InvalidExternalResource(Response.Status.BAD_REQUEST),
    InvalidNotification(Response.Status.BAD_REQUEST),
    InvalidPropagationTask(Response.Status.BAD_REQUEST),
    InvalidSchedTask(Response.Status.BAD_REQUEST),
    InvalidSyncTask(Response.Status.BAD_REQUEST),
    InvalidValues(Response.Status.BAD_REQUEST),
    NotFound(Response.Status.NOT_FOUND),
    RejectedUserCreate(Response.Status.BAD_REQUEST),
    RequiredValuesMissing(Response.Status.BAD_REQUEST),
    RoleOwnership(Response.Status.BAD_REQUEST),
    Scheduling(Response.Status.BAD_REQUEST),
    UnauthorizedRole(Response.Status.UNAUTHORIZED),
    Unauthorized(Response.Status.UNAUTHORIZED),
    Unknown(Response.Status.BAD_REQUEST),
    Workflow(Response.Status.BAD_REQUEST);

    private final Response.Status responseStatus;

    private ClientExceptionType(final Response.Status responseStatus) {
        this.responseStatus = responseStatus;
    }

    public static ClientExceptionType fromHeaderValue(final String exceptionTypeHeaderValue) {
        ClientExceptionType result = null;
        for (ClientExceptionType type : values()) {
            if (exceptionTypeHeaderValue.equals(type.getHeaderValue())) {
                result = type;
            }
        }

        if (result == null) {
            throw new IllegalArgumentException("Unexpected header value: " + exceptionTypeHeaderValue);
        }

        return result;
    }

    public String getHeaderValue() {
        return name();
    }

    public String getElementHeaderName() {
        return getHeaderValue() + ".element";
    }

    public Response.Status getResponseStatus() {
        return responseStatus;
    }

}
