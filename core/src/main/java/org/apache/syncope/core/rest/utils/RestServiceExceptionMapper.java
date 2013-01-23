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
package org.apache.syncope.core.rest.utils;

import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.PersistenceException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
import org.apache.syncope.common.types.EntityViolationType;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.common.validation.SyncopeClientErrorHandler;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.persistence.dao.InvalidSearchConditionException;
import org.apache.syncope.core.persistence.dao.MissingConfKeyException;
import org.apache.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.util.NotFoundException;
import org.apache.syncope.core.workflow.WorkflowException;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;

@Provider
public class RestServiceExceptionMapper implements ExceptionMapper<Exception>, ResponseExceptionMapper<Exception> {

    private static final String BASIC_REALM_UNAUTHORIZED = "Basic realm=\"Spring Security Application\"";
    private static final Logger LOG = LoggerFactory.getLogger(RestServiceExceptionMapper.class);
    public static final String EXCEPTION_TYPE_HEADER = "ExceptionType";

    @Override
    public Response toResponse(final Exception ex) {

        LOG.error("Exception thrown by REST methods: " + ex.getMessage(), ex);

        // 1. Process SyncopeClientCompositeErrorException
        Response response = processCompositeExceptions(ex);
        if (response != null) {
            return response;
        }

        // 2. Process Bad Requests
        response = processBadRequestExceptions(ex);
        if (response != null) {
            return response;
        }

        // 3. Process Unauthorized
        response = processUnauthorizedExceptions(ex);
        if (response != null) {
            return response;
        }

        // 4. Process Forbidden
        response = processForbiddenExceptions(ex);
        if (response != null) {
            return response;
        }

        // 4. Process NotFound
        response = processNotFoundExceptions(ex);
        if (response != null) {
            return response;
        }

        // 5. Process Conflict
        response = processConflictExceptions(ex);
        if (response != null) {
            return response;
        }

        // 5. Process InternalServerError
        response = processServerErrorExceptions(ex);
        if (response != null) {
            return response;
        }

        // 6. Rest is interpreted as InternalServerError
        ResponseBuilder responseBuilder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
        responseBuilder.header(SyncopeClientExceptionType.Unknown.getElementHeaderName(), ex.getCause() == null
                ? ex.getMessage()
                : ex.getCause().getMessage());

        return responseBuilder.build();
    }

    @Override
    public Exception fromResponse(final Response r) {
        throw new UnsupportedOperationException(
                "Call of fromResponse() method is not expected in RestServiceExceptionMapper");
    }

    private Response processCompositeExceptions(Exception ex) {
        Response response = null;

        if (ex instanceof SyncopeClientCompositeErrorException) {
            ResponseBuilder responseBuilder = Response.status(((SyncopeClientCompositeErrorException) ex)
                    .getStatusCode().value());
            for (SyncopeClientException sce : ((SyncopeClientCompositeErrorException) ex).getExceptions()) {
                responseBuilder.header(EXCEPTION_TYPE_HEADER, sce.getType().getHeaderValue());

                for (String attributeName : sce.getElements()) {
                    responseBuilder.header(sce.getType().getElementHeaderName(), attributeName);
                }
            }
            response = responseBuilder.build();
        }
        return response;
    }

    private Response processForbiddenExceptions(Exception ex) {
        Response response = null;
        ResponseBuilder responseBuilder = Response.status(Response.Status.FORBIDDEN);

        if (ex instanceof UnauthorizedRoleException) {
            response = buildResponse(responseBuilder, SyncopeClientExceptionType.UnauthorizedRole, ex, null);
        }
        return response;
    }

    private Response processUnauthorizedExceptions(Exception ex) {
        Response response = null;
        ResponseBuilder responseBuilder = Response.status(Response.Status.UNAUTHORIZED);

        if (ex instanceof org.springframework.security.access.AccessDeniedException) {
            response = responseBuilder.header(HttpHeaders.WWW_AUTHENTICATE, BASIC_REALM_UNAUTHORIZED).build();
        }
        return response;
    }

    private Response processConflictExceptions(Exception ex) {
        Response response = null;
        ResponseBuilder responseBuilder = Response.status(Response.Status.CONFLICT);

        if (ex instanceof DataIntegrityViolationException) {
            response = buildResponse(responseBuilder, SyncopeClientExceptionType.DataIntegrityViolation, ex, null);
        }
        return response;
    }

    private Response processServerErrorExceptions(Exception ex) {
        Response response = null;
        ResponseBuilder responseBuilder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);

        if (ex instanceof org.apache.ibatis.exceptions.PersistenceException) {
            response = buildResponse(responseBuilder, SyncopeClientExceptionType.Workflow, ex,
                    "Currently unavailable. Please try later.");

        } else if (ex instanceof JpaSystemException) {
            response = buildResponse(responseBuilder, SyncopeClientExceptionType.DataIntegrityViolation, ex, null);

        } else if (ex instanceof ConfigurationException) {
            response = buildResponse(responseBuilder, SyncopeClientExceptionType.InvalidExternalResource, ex, null);
        }

        return response;
    }

    private Response processNotFoundExceptions(Exception ex) {
        Response response = null;
        ResponseBuilder responseBuilder = Response.status(Response.Status.NOT_FOUND);

        if (ex instanceof NotFoundException) {
            response = buildResponse(responseBuilder, SyncopeClientExceptionType.NotFound, ex, null);

        } else if (ex instanceof MissingConfKeyException) {
            response = buildResponse(responseBuilder, SyncopeClientExceptionType.NotFound, ex,
                    ((MissingConfKeyException) ex).getConfKey());
        }

        return response;
    }

    private Response processBadRequestExceptions(Exception ex) {
        Response response = null;
        ResponseBuilder responseBuilder = Response.status(Response.Status.BAD_REQUEST);

        if (ex instanceof InvalidEntityException) {
            SyncopeClientExceptionType exType = SyncopeClientExceptionType.valueOf("Invalid"
                    + ((InvalidEntityException) ex).getEntityClassSimpleName());

            responseBuilder.header(SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER, exType.getHeaderValue());

            for (@SuppressWarnings("rawtypes")
            Entry<Class, Set<EntityViolationType>> violation : ((InvalidEntityException) ex).getViolations().entrySet()) {

                for (EntityViolationType violationType : violation.getValue()) {
                    responseBuilder.header(exType.getElementHeaderName(), violation.getClass().getSimpleName() + ": "
                            + violationType);
                }
            }
            response = responseBuilder.build();

        } else if (ex instanceof WorkflowException) {
            response = buildResponse(responseBuilder, SyncopeClientExceptionType.Workflow, ex, ex.getCause()
                    .getMessage());

        } else if (ex instanceof PropagationException) {
            response = buildResponse(responseBuilder, SyncopeClientExceptionType.Propagation, ex,
                    ((PropagationException) ex).getResourceName());

        } else if (ex instanceof InvalidSearchConditionException) {
            response = buildResponse(responseBuilder, SyncopeClientExceptionType.InvalidSearchCondition, ex, null);

        } else if (ex instanceof PersistenceException) {
            response = buildResponse(responseBuilder, SyncopeClientExceptionType.GenericPersistence, ex, null);
        }

        return response;
    }

    private Response buildResponse(ResponseBuilder responseBuilder, SyncopeClientExceptionType hType, Throwable ex,
            String msg) {
        responseBuilder.header(SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER, hType.getHeaderValue());
        String exMsg = ex.getCause() == null
                ? ex.getMessage()
                : ex.getCause().getMessage();
        responseBuilder.header(hType.getElementHeaderName(), (msg != null)
                ? msg
                : exMsg);
        return responseBuilder.build();
    }
}
