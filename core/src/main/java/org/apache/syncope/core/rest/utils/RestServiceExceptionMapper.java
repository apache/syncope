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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityExistsException;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;
import javax.validation.ValidationException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.validation.ValidationExceptionMapper;
import org.apache.syncope.common.types.EntityViolationType;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.types.RESTHeaders;
import org.apache.syncope.common.SyncopeClientCompositeException;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.reqres.ErrorTO;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.validation.attrvalue.ParsingValidationException;
import org.apache.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.workflow.WorkflowException;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionSystemException;

@Configuration
@PropertySource("classpath:errorMessages.properties")
@Provider
public class RestServiceExceptionMapper implements ExceptionMapper<Exception>, ResponseExceptionMapper<Exception> {

    private static final String BASIC_REALM_UNAUTHORIZED = "Basic realm=\"Apache Syncope authentication\"";

    private static final Logger LOG = LoggerFactory.getLogger(RestServiceExceptionMapper.class);

    private final ValidationExceptionMapper validationEM = new ValidationExceptionMapper();

    @Autowired
    private Environment env;

    private static final Map<String, String> exceptionCodeMap;

    static {
        exceptionCodeMap = new HashMap<String, String>();
        exceptionCodeMap.put("23000", "UniqueConstraintViolation");
        exceptionCodeMap.put("23505", "UniqueConstraintViolation");
    }

    @Override
    public Response toResponse(final Exception ex) {
        LOG.error("Exception thrown by REST method: " + ex.getMessage(), ex);

        ResponseBuilder builder;

        if (ex instanceof SyncopeClientException) {
            SyncopeClientException sce = (SyncopeClientException) ex;
            builder = sce.isComposite()
                    ? getSyncopeClientCompositeExceptionResponse(sce.asComposite())
                    : getSyncopeClientExceptionResponse(sce);
        } else if (ex instanceof WebApplicationException) {
            Response response = ((WebApplicationException) ex).getResponse();

            ErrorTO error = new ErrorTO();
            error.setStatus(response.getStatus());
            error.setType(ClientExceptionType.Unknown);
            error.getElements().add(getExMessage(ex));

            builder = JAXRSUtils.fromResponse(response).entity(error);
        } else if (ex instanceof AccessDeniedException) {
            builder = Response.status(Response.Status.UNAUTHORIZED).
                    header(HttpHeaders.WWW_AUTHENTICATE, BASIC_REALM_UNAUTHORIZED);
        } else if (ex instanceof UnauthorizedRoleException) {
            builder = builder(Response.Status.UNAUTHORIZED, ClientExceptionType.UnauthorizedRole, getExMessage(ex));
        } else if (ex instanceof EntityExistsException) {
            builder = builder(Response.Status.CONFLICT, ClientExceptionType.EntityExists, getJPAMessage(ex));
        } else if (ex instanceof DataIntegrityViolationException) {
            builder = builder(Response.Status.CONFLICT, ClientExceptionType.DataIntegrityViolation, getJPAMessage(ex));
        } else if (ex instanceof JpaSystemException) {
            builder = builder(Response.Status.CONFLICT, ClientExceptionType.DataIntegrityViolation, getJPAMessage(ex));
        } else {
            builder = processNotFoundExceptions(ex);
            if (builder == null) {
                builder = processInvalidEntityExceptions(ex);
                if (builder == null) {
                    builder = processBadRequestExceptions(ex);
                }
                // process JAX-RS validation errors
                if (builder == null && ex instanceof ValidationException) {
                    builder = JAXRSUtils.fromResponse(validationEM.toResponse((ValidationException) ex)).
                            header(RESTHeaders.ERROR_CODE, ClientExceptionType.RESTValidation.getHeaderValue()).
                            header(RESTHeaders.ERROR_INFO,
                                    ClientExceptionType.RESTValidation.getInfoHeaderValue(getExMessage(ex)));

                    ErrorTO error = new ErrorTO();
                    error.setStatus(ClientExceptionType.RESTValidation.getResponseStatus().getStatusCode());
                    error.setType(ClientExceptionType.RESTValidation);
                    error.getElements().add(getExMessage(ex));
                    builder.entity(error);
                }
                // ...or just report as InternalServerError
                if (builder == null) {
                    builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                            header(RESTHeaders.ERROR_INFO,
                                    ClientExceptionType.Unknown.getInfoHeaderValue(getExMessage(ex)));

                    ErrorTO error = new ErrorTO();
                    error.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
                    error.setType(ClientExceptionType.Unknown);
                    error.getElements().add(getExMessage(ex));
                    builder.entity(error);
                }
            }
        }

        return builder.build();
    }

    @Override
    public Exception fromResponse(final Response r) {
        throw new UnsupportedOperationException(
                "Call of fromResponse() method is not expected in RestServiceExceptionMapper");
    }

    private ResponseBuilder getSyncopeClientExceptionResponse(final SyncopeClientException ex) {
        ResponseBuilder builder = Response.status(ex.getType().getResponseStatus());
        builder.header(RESTHeaders.ERROR_CODE, ex.getType().getHeaderValue());

        ErrorTO error = new ErrorTO();
        error.setStatus(ex.getType().getResponseStatus().getStatusCode());
        error.setType(ex.getType());

        for (String element : ex.getElements()) {
            builder.header(RESTHeaders.ERROR_INFO, ex.getType().getInfoHeaderValue(element));
            error.getElements().add(element);
        }

        return builder.entity(error);
    }

    private ResponseBuilder getSyncopeClientCompositeExceptionResponse(final SyncopeClientCompositeException ex) {
        if (ex.getExceptions().size() == 1) {
            return getSyncopeClientExceptionResponse(ex.getExceptions().iterator().next());
        }

        ResponseBuilder builder = Response.status(Response.Status.BAD_REQUEST);

        List<ErrorTO> errors = new ArrayList<ErrorTO>();
        for (SyncopeClientException sce : ex.getExceptions()) {
            builder.header(RESTHeaders.ERROR_CODE, sce.getType().getHeaderValue());

            ErrorTO error = new ErrorTO();
            error.setStatus(sce.getType().getResponseStatus().getStatusCode());
            error.setType(sce.getType());

            for (String element : sce.getElements()) {
                builder.header(RESTHeaders.ERROR_INFO, sce.getType().getInfoHeaderValue(element));
                error.getElements().add(element);
            }

            errors.add(error);
        }

        return builder.entity(errors);
    }

    private ResponseBuilder processNotFoundExceptions(final Exception ex) {
        if (ex instanceof javax.ws.rs.NotFoundException || ex instanceof NotFoundException) {
            return builder(Response.Status.NOT_FOUND, ClientExceptionType.NotFound, getExMessage(ex));
        }

        return null;
    }

    private ResponseBuilder processInvalidEntityExceptions(final Exception ex) {
        InvalidEntityException iee = null;

        if (ex instanceof InvalidEntityException) {
            iee = (InvalidEntityException) ex;
        }
        if (ex instanceof TransactionSystemException && ex.getCause() instanceof RollbackException
                && ex.getCause().getCause() instanceof InvalidEntityException) {

            iee = (InvalidEntityException) ex.getCause().getCause();
        }

        if (iee != null) {
            ClientExceptionType exType =
                    iee.getEntityClassSimpleName().endsWith("Policy")
                            ? ClientExceptionType.InvalidPolicy
                            : ClientExceptionType.valueOf("Invalid" + iee.getEntityClassSimpleName());

            ResponseBuilder builder = Response.status(Response.Status.BAD_REQUEST);
            builder.header(RESTHeaders.ERROR_CODE, exType.getHeaderValue());

            ErrorTO error = new ErrorTO();
            error.setStatus(exType.getResponseStatus().getStatusCode());
            error.setType(exType);

            for (Map.Entry<Class<?>, Set<EntityViolationType>> violation : iee.getViolations().entrySet()) {
                for (EntityViolationType violationType : violation.getValue()) {
                    builder.header(RESTHeaders.ERROR_INFO,
                            exType.getInfoHeaderValue(violationType.name() + ": " + violationType.getMessage()));
                    error.getElements().add(violationType.name() + ": " + violationType.getMessage());
                }
            }

            return builder;
        }

        return null;
    }

    private ResponseBuilder processBadRequestExceptions(final Exception ex) {
        ResponseBuilder builder = Response.status(Response.Status.BAD_REQUEST);

        if (ex instanceof BadRequestException) {
            if (((BadRequestException) ex).getResponse() == null) {
                return builder;
            } else {
                return JAXRSUtils.fromResponse(((BadRequestException) ex).getResponse());
            }
        } else if (ex instanceof WorkflowException) {
            return builder(Response.Status.BAD_REQUEST, ClientExceptionType.Workflow, getExMessage(ex));
        } else if (ex instanceof PersistenceException) {
            return builder(Response.Status.BAD_REQUEST, ClientExceptionType.GenericPersistence, getExMessage(ex));
        } else if (ex instanceof org.apache.ibatis.exceptions.PersistenceException) {
            return builder(Response.Status.BAD_REQUEST, ClientExceptionType.Workflow,
                    getMessage(ex, "Currently unavailable. Please try later."));
        } else if (ex instanceof JpaSystemException) {
            return builder(Response.Status.BAD_REQUEST, ClientExceptionType.DataIntegrityViolation, getExMessage(ex));
        } else if (ex instanceof ConfigurationException) {
            return builder(Response.Status.BAD_REQUEST, ClientExceptionType.InvalidConnIdConf, getExMessage(ex));
        } else if (ex instanceof ParsingValidationException) {
            return builder(Response.Status.BAD_REQUEST, ClientExceptionType.InvalidValues, getExMessage(ex));
        }

        return null;
    }

    private ResponseBuilder builder(final Response.Status status, final ClientExceptionType hType, final String msg) {
        ResponseBuilder builder = Response.status(status).
                header(RESTHeaders.ERROR_CODE, hType.getHeaderValue()).
                header(RESTHeaders.ERROR_INFO, hType.getInfoHeaderValue(msg));

        ErrorTO error = new ErrorTO();
        error.setStatus(status.getStatusCode());
        error.setType(hType);
        error.getElements().add(msg);

        return builder.entity(error);
    }

    private String getMessage(final Throwable ex, final String msg) {
        return (msg == null) ? getExMessage(ex) : msg;
    }

    private String getExMessage(final Throwable ex) {
        return (ex.getCause() == null) ? ex.getMessage() : ex.getCause().getMessage();
    }

    private String getJPAMessage(final Throwable ex) {
        Throwable throwable = ExceptionUtils.getRootCause(ex);
        String message = null;
        if (throwable instanceof SQLException) {
            String SQLState = ((SQLException) throwable).getSQLState();
            String messageKey = exceptionCodeMap.get(SQLState);
            if (messageKey != null) {
                message = env.getProperty("errMessage." + messageKey);
            }
        }

        return message == null ? getExMessage(ex) : message;
    }
}
