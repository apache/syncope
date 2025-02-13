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
package org.apache.syncope.common.lib.types;

import jakarta.ws.rs.core.Response;

public enum ClientExceptionType {

    AssociatedResources(Response.Status.BAD_REQUEST),
    Composite(Response.Status.BAD_REQUEST),
    ConcurrentModification(Response.Status.PRECONDITION_FAILED),
    ConnectorException(Response.Status.BAD_REQUEST),
    DataIntegrityViolation(Response.Status.CONFLICT),
    EntityExists(Response.Status.CONFLICT),
    GenericPersistence(Response.Status.BAD_REQUEST),
    InvalidAccessToken(Response.Status.INTERNAL_SERVER_ERROR),
    InvalidImplementation(Response.Status.BAD_REQUEST),
    InvalidImplementationType(Response.Status.NOT_FOUND),
    InvalidSecurityAnswer(Response.Status.BAD_REQUEST),
    InvalidEntity(Response.Status.BAD_REQUEST),
    InvalidLogger(Response.Status.BAD_REQUEST),
    InvalidConnInstance(Response.Status.BAD_REQUEST),
    InvalidConnIdConf(Response.Status.BAD_REQUEST),
    InvalidPolicy(Response.Status.BAD_REQUEST),
    InvalidConf(Response.Status.BAD_REQUEST),
    InvalidPath(Response.Status.BAD_REQUEST),
    InvalidProvision(Response.Status.BAD_REQUEST),
    InvalidOrgUnit(Response.Status.BAD_REQUEST),
    InvalidReport(Response.Status.BAD_REQUEST),
    InvalidReportExec(Response.Status.BAD_REQUEST),
    InvalidRelationship(Response.Status.BAD_REQUEST),
    InvalidRelationshipType(Response.Status.BAD_REQUEST),
    InvalidAnyType(Response.Status.BAD_REQUEST),
    InvalidAnyObject(Response.Status.BAD_REQUEST),
    InvalidGroup(Response.Status.BAD_REQUEST),
    InvalidSchemaDefinition(Response.Status.BAD_REQUEST),
    InvalidSearchParameters(Response.Status.BAD_REQUEST),
    InvalidPageOrSize(Response.Status.BAD_REQUEST),
    InvalidPropagationTaskExecReport(Response.Status.BAD_REQUEST),
    InvalidPlainSchema(Response.Status.BAD_REQUEST),
    InvalidDerSchema(Response.Status.BAD_REQUEST),
    InvalidVirSchema(Response.Status.BAD_REQUEST),
    InvalidMapping(Response.Status.BAD_REQUEST),
    InvalidMembership(Response.Status.BAD_REQUEST),
    InvalidRealm(Response.Status.BAD_REQUEST),
    InvalidDynRealm(Response.Status.BAD_REQUEST),
    InvalidRole(Response.Status.BAD_REQUEST),
    InvalidUser(Response.Status.BAD_REQUEST),
    InvalidExternalResource(Response.Status.BAD_REQUEST),
    InvalidLiveSyncTask(Response.Status.BAD_REQUEST),
    InvalidPullTask(Response.Status.BAD_REQUEST),
    InvalidRequest(Response.Status.BAD_REQUEST),
    InvalidValues(Response.Status.BAD_REQUEST),
    NotFound(Response.Status.NOT_FOUND),
    RealmContains(Response.Status.BAD_REQUEST),
    RequiredValuesMissing(Response.Status.BAD_REQUEST),
    RESTValidation(Response.Status.BAD_REQUEST),
    GroupOwnership(Response.Status.BAD_REQUEST),
    InUse(Response.Status.BAD_REQUEST),
    Scheduling(Response.Status.BAD_REQUEST),
    DelegatedAdministration(Response.Status.FORBIDDEN),
    Reconciliation(Response.Status.BAD_REQUEST),
    RunError(Response.Status.INTERNAL_SERVER_ERROR),
    Unknown(Response.Status.BAD_REQUEST),
    Workflow(Response.Status.BAD_REQUEST);

    private final Response.Status responseStatus;

    ClientExceptionType(final Response.Status responseStatus) {
        this.responseStatus = responseStatus;
    }

    public static ClientExceptionType fromHeaderValue(final String exceptionTypeHeaderValue) {
        ClientExceptionType result = null;
        for (ClientExceptionType type : values()) {
            if (exceptionTypeHeaderValue.equals(type.name())) {
                result = type;
            }
        }

        if (result == null) {
            throw new IllegalArgumentException("Unexpected header value: " + exceptionTypeHeaderValue);
        }

        return result;
    }

    public String getInfoHeaderValue(final String value) {
        // HTTP header values cannot contain CR / LF
        return (name() + ':' + value).replaceAll("(\\r|\\n)", " ");
    }

    public Response.Status getResponseStatus() {
        return responseStatus;
    }

}
