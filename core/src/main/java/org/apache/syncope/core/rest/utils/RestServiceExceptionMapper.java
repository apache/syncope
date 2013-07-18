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

import java.util.Map;
import java.util.Set;
import javax.persistence.EntityExistsException;
import javax.persistence.PersistenceException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
import org.apache.syncope.common.services.InvalidSearchConditionException;
import org.apache.syncope.common.types.EntityViolationType;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.common.validation.SyncopeClientErrorHandler;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.persistence.dao.MissingConfKeyException;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.workflow.WorkflowException;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.access.AccessDeniedException;

@Provider
public class RestServiceExceptionMapper implements ExceptionMapper<Exception>, ResponseExceptionMapper<Exception> {

    private static final String BASIC_REALM_UNAUTHORIZED = "Basic realm=\"Apache Syncope authentication\"";

    private static final Logger LOG = LoggerFactory.getLogger(RestServiceExceptionMapper.class);

    public static final String EXCEPTION_TYPE_HEADER = "ExceptionType";

    @Override
    public Response toResponse(final Exception ex) {
        LOG.error("Exception thrown by REST method: " + ex.getMessage(), ex);

        if (ex instanceof SyncopeClientCompositeErrorException) {
            return getCompositeExceptionResponse((SyncopeClientCompositeErrorException) ex);
        }

        if (ex instanceof AccessDeniedException) {
            return Response.status(Response.Status.UNAUTHORIZED).
                    header(HttpHeaders.WWW_AUTHENTICATE, BASIC_REALM_UNAUTHORIZED).
                    build();
        }

        if (ex instanceof UnauthorizedRoleException) {
            return buildResponse(Response.status(Response.Status.FORBIDDEN),
                    SyncopeClientExceptionType.UnauthorizedRole,
                    getExMessage(ex));
        }

        if (ex instanceof EntityExistsException) {
            return buildResponse(Response.status(Response.Status.CONFLICT),
                    SyncopeClientExceptionType.EntityExists,
                    getExMessage(ex));
        }

        if (ex instanceof DataIntegrityViolationException) {
            return buildResponse(Response.status(Response.Status.CONFLICT),
                    SyncopeClientExceptionType.DataIntegrityViolation,
                    getExMessage(ex));
        }

        Response response = processBadRequestExceptions(ex);
        if (response != null) {
            return response;
        }

        response = processNotFoundExceptions(ex);
        if (response != null) {
            return response;
        }

        response = processServerErrorExceptions(ex);
        if (response != null) {
            return response;
        }

        // Rest is interpreted as InternalServerError
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                header(SyncopeClientExceptionType.Unknown.getElementHeaderName(), getExMessage(ex)).
                build();
    }

    @Override
    public Exception fromResponse(final Response r) {
        throw new UnsupportedOperationException(
                "Call of fromResponse() method is not expected in RestServiceExceptionMapper");
    }

    private Response getCompositeExceptionResponse(final SyncopeClientCompositeErrorException ex) {
        ResponseBuilder responseBuilder = Response.status(ex.getStatusCode().value());
        for (SyncopeClientException sce : ex.getExceptions()) {
            responseBuilder.header(EXCEPTION_TYPE_HEADER, sce.getType().getHeaderValue());

            for (String attributeName : sce.getElements()) {
                responseBuilder.header(sce.getType().getElementHeaderName(), attributeName);
            }
        }
        return responseBuilder.build();
    }

    private Response processServerErrorExceptions(final Exception ex) {
        ResponseBuilder responseBuilder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
        if (ex instanceof org.apache.ibatis.exceptions.PersistenceException) {
            return buildResponse(responseBuilder, SyncopeClientExceptionType.Workflow, getMessage(ex,
                    "Currently unavailable. Please try later."));
        } else if (ex instanceof JpaSystemException) {
            return buildResponse(responseBuilder, SyncopeClientExceptionType.DataIntegrityViolation, getExMessage(ex));

        } else if (ex instanceof ConfigurationException) {
            return buildResponse(responseBuilder, SyncopeClientExceptionType.InvalidConnIdConf, getExMessage(ex));
        }

        return null;
    }

    private Response processNotFoundExceptions(final Exception ex) {
        ResponseBuilder responseBuilder = Response.status(Response.Status.NOT_FOUND);

        if (ex instanceof javax.ws.rs.NotFoundException) {
            return buildResponse(responseBuilder, SyncopeClientExceptionType.NotFound, getExMessage(ex));

        } else if (ex instanceof NotFoundException) {
            return buildResponse(responseBuilder, SyncopeClientExceptionType.NotFound, getExMessage(ex));

        } else if (ex instanceof MissingConfKeyException) {
            return buildResponse(responseBuilder, SyncopeClientExceptionType.NotFound, getMessage(ex,
                    ((MissingConfKeyException) ex).getConfKey()));
        }
        return null;
    }

    private Response processBadRequestExceptions(final Exception ex) {
        ResponseBuilder responseBuilder = Response.status(Response.Status.BAD_REQUEST);

        if (ex instanceof BadRequestException) {
            if (((BadRequestException) ex).getResponse() == null) {
                return responseBuilder.build();
            } else {
                return ((BadRequestException) ex).getResponse();
            }

        } else if (ex instanceof InvalidEntityException) {
            SyncopeClientExceptionType exType = SyncopeClientExceptionType.valueOf("Invalid"
                    + ((InvalidEntityException) ex).getEntityClassSimpleName());

            responseBuilder.header(SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER, exType.getHeaderValue());

            for (@SuppressWarnings("rawtypes") Map.Entry<Class, Set<EntityViolationType>> violation
                    : ((InvalidEntityException) ex).getViolations().entrySet()) {

                for (EntityViolationType violationType : violation.getValue()) {
                    responseBuilder.header(exType.getElementHeaderName(),
                            violation.getClass().getSimpleName() + ": " + violationType);
                }
            }
            return responseBuilder.build();
        } else if (ex instanceof WorkflowException) {
            return buildResponse(responseBuilder, SyncopeClientExceptionType.Workflow, getExMessage(ex));
        } else if (ex instanceof InvalidSearchConditionException) {
            return buildResponse(responseBuilder, SyncopeClientExceptionType.InvalidSearchCondition, getExMessage(ex));
        } else if (ex instanceof PersistenceException) {
            return buildResponse(responseBuilder, SyncopeClientExceptionType.GenericPersistence, getExMessage(ex));
        }

        return null;
    }

    private Response buildResponse(final ResponseBuilder responseBuilder, final SyncopeClientExceptionType hType,
            final String msg) {

        return responseBuilder.header(SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER, hType.getHeaderValue()).
                header(hType.getElementHeaderName(), msg).
                build();
    }

    private String getMessage(final Throwable ex, final String msg) {
        return (msg == null) ? getExMessage(ex) : msg;
    }

    private String getExMessage(final Throwable ex) {
        return (ex.getCause() == null) ? ex.getMessage() : ex.getCause().getMessage();
    }
}
