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
import javax.persistence.RollbackException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
import org.apache.syncope.common.services.InvalidSearchConditionException;
import org.apache.syncope.common.types.EntityViolationType;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.types.RESTHeaders;
import org.apache.syncope.common.validation.SyncopeClientCompositeException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.persistence.dao.MissingConfKeyException;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.validation.attrvalue.ParsingValidationException;
import org.apache.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.workflow.WorkflowException;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionSystemException;

@Provider
public class RestServiceExceptionMapper implements ExceptionMapper<Exception>, ResponseExceptionMapper<Exception> {

    private static final String BASIC_REALM_UNAUTHORIZED = "Basic realm=\"Apache Syncope authentication\"";

    private static final Logger LOG = LoggerFactory.getLogger(RestServiceExceptionMapper.class);

    @Override
    public Response toResponse(final Exception ex) {
        LOG.error("Exception thrown by REST method: " + ex.getMessage(), ex);

        if (ex instanceof SyncopeClientException) {
            SyncopeClientException sce = (SyncopeClientException) ex;
            return (sce.isComposite()
                    ? getSyncopeClientCompositeExceptionResponse(sce.asComposite())
                    : getSyncopeClientExceptionResponse(sce));
        }

        if (ex instanceof AccessDeniedException) {
            return Response.status(Response.Status.UNAUTHORIZED).
                    header(HttpHeaders.WWW_AUTHENTICATE, BASIC_REALM_UNAUTHORIZED).
                    build();
        }

        if (ex instanceof UnauthorizedRoleException) {
            return buildResponse(Response.status(Response.Status.UNAUTHORIZED),
                    ClientExceptionType.UnauthorizedRole,
                    getExMessage(ex));
        }

        if (ex instanceof EntityExistsException) {
            return buildResponse(Response.status(Response.Status.CONFLICT),
                    ClientExceptionType.EntityExists,
                    getExMessage(ex));
        }

        if (ex instanceof DataIntegrityViolationException) {
            return buildResponse(Response.status(Response.Status.CONFLICT),
                    ClientExceptionType.DataIntegrityViolation,
                    getExMessage(ex));
        }

        Response response = processNotFoundExceptions(ex);
        if (response != null) {
            return response;
        }

        response = processInvalidEntityExceptions(ex);
        if (response != null) {
            return response;
        }

        response = processBadRequestExceptions(ex);
        if (response != null) {
            return response;
        }

        // Rest is interpreted as InternalServerError
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                header(ClientExceptionType.Unknown.getElementHeaderName(), getExMessage(ex)).
                build();
    }

    @Override
    public Exception fromResponse(final Response r) {
        throw new UnsupportedOperationException(
                "Call of fromResponse() method is not expected in RestServiceExceptionMapper");
    }

    private Response getSyncopeClientExceptionResponse(final SyncopeClientException ex) {
        ResponseBuilder responseBuilder = Response.status(ex.getType().getResponseStatus());
        responseBuilder.header(RESTHeaders.EXCEPTION_TYPE, ex.getType().getHeaderValue());

        for (Object element : ex.getElements()) {
            responseBuilder.header(ex.getType().getElementHeaderName(), element);
        }

        return responseBuilder.build();
    }

    private Response getSyncopeClientCompositeExceptionResponse(final SyncopeClientCompositeException ex) {
        if (ex.getExceptions().size() == 1) {
            return getSyncopeClientExceptionResponse(ex.getExceptions().iterator().next());
        }

        ResponseBuilder responseBuilder = Response.status(Response.Status.BAD_REQUEST);
        for (SyncopeClientException sce : ex.getExceptions()) {
            responseBuilder.header(RESTHeaders.EXCEPTION_TYPE, sce.getType().getHeaderValue());

            for (Object element : sce.getElements()) {
                responseBuilder.header(sce.getType().getElementHeaderName(), element);
            }
        }
        return responseBuilder.build();
    }

    private Response processNotFoundExceptions(final Exception ex) {
        ResponseBuilder responseBuilder = Response.status(Response.Status.NOT_FOUND);

        if (ex instanceof javax.ws.rs.NotFoundException || ex instanceof NotFoundException) {
            return buildResponse(responseBuilder, ClientExceptionType.NotFound, getExMessage(ex));
        } else if (ex instanceof MissingConfKeyException) {
            return buildResponse(responseBuilder, ClientExceptionType.NotFound,
                    getMessage(ex, ((MissingConfKeyException) ex).getConfKey()));
        }

        return null;
    }

    private Response processInvalidEntityExceptions(final Exception ex) {
        InvalidEntityException iee = null;

        if (ex instanceof InvalidEntityException) {
            iee = (InvalidEntityException) ex;
        }
        if (ex instanceof TransactionSystemException && ex.getCause() instanceof RollbackException
                && ex.getCause().getCause() instanceof InvalidEntityException) {

            iee = (InvalidEntityException) ex.getCause().getCause();
        }

        if (iee != null) {
            ResponseBuilder builder = Response.status(Response.Status.BAD_REQUEST);

            ClientExceptionType exType = ClientExceptionType.valueOf("Invalid" + iee.getEntityClassSimpleName());

            builder.header(RESTHeaders.EXCEPTION_TYPE, exType.getHeaderValue());

            for (Map.Entry<Class<?>, Set<EntityViolationType>> violation : iee.getViolations().entrySet()) {
                for (EntityViolationType violationType : violation.getValue()) {
                    builder.header(exType.getElementHeaderName(),
                            violationType.name() + ": " + violationType.getMessage());
                }
            }

            return builder.build();
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
        } else if (ex instanceof WorkflowException) {
            return buildResponse(responseBuilder, ClientExceptionType.Workflow, getExMessage(ex));
        } else if (ex instanceof InvalidSearchConditionException) {
            return buildResponse(responseBuilder, ClientExceptionType.InvalidSearchCondition, getExMessage(ex));
        } else if (ex instanceof PersistenceException) {
            return buildResponse(responseBuilder, ClientExceptionType.GenericPersistence, getExMessage(ex));
        } else if (ex instanceof org.apache.ibatis.exceptions.PersistenceException) {
            return buildResponse(responseBuilder, ClientExceptionType.Workflow, getMessage(ex,
                    "Currently unavailable. Please try later."));
        } else if (ex instanceof JpaSystemException) {
            return buildResponse(responseBuilder, ClientExceptionType.DataIntegrityViolation, getExMessage(ex));
        } else if (ex instanceof ConfigurationException) {
            return buildResponse(responseBuilder, ClientExceptionType.InvalidConnIdConf, getExMessage(ex));
        } else if (ex instanceof ParsingValidationException) {
            return buildResponse(responseBuilder, ClientExceptionType.InvalidValues, getExMessage(ex));
        }

        return null;
    }

    private Response buildResponse(final ResponseBuilder responseBuilder, final ClientExceptionType hType,
            final String msg) {

        return responseBuilder.header(RESTHeaders.EXCEPTION_TYPE, hType.getHeaderValue()).
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
