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
package org.apache.syncope.core.rest.cxf;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.RollbackException;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.validation.ValidationExceptionMapper;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ErrorTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.core.persistence.api.attrvalue.InvalidEntityException;
import org.apache.syncope.core.persistence.api.attrvalue.ParsingValidationException;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.MalformedPathException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.UncategorizedDataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionSystemException;

@Provider
public class RestServiceExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = LoggerFactory.getLogger(RestServiceExceptionMapper.class);

    private final ValidationExceptionMapper validationEM = new ValidationExceptionMapper();

    private static final String UNIQUE_MSG_KEY = "UniqueConstraintViolation";

    private static final Map<String, String> EXCEPTION_CODE_MAP = new HashMap<>() {

        private static final long serialVersionUID = -7688359318035249200L;

        {
            put("23000", UNIQUE_MSG_KEY);
            put("23505", UNIQUE_MSG_KEY);
        }
    };

    protected final Environment env;

    public RestServiceExceptionMapper(final Environment env) {
        this.env = env;
    }

    @Override
    public Response toResponse(final Exception ex) {
        if (ex instanceof NotFoundException) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Exception thrown", ex);
            } else {
                LOG.warn("{} thrown: {}", NotFoundException.class.getSimpleName(), ex.getMessage());
            }
        } else {
            LOG.error("Exception thrown", ex);
        }

        ResponseBuilder builder;

        if (ex instanceof AccessDeniedException) {
            // leaves the default exception processing to Spring Security
            builder = null;
        } else if (ex instanceof SyncopeClientException sce) {
            builder = sce.isComposite()
                    ? getSyncopeClientCompositeExceptionResponse(sce.asComposite())
                    : getSyncopeClientExceptionResponse(sce);
        } else if (ex instanceof DelegatedAdministrationException
                || ExceptionUtils.getRootCause(ex) instanceof DelegatedAdministrationException) {

            builder = builder(ClientExceptionType.DelegatedAdministration, ExceptionUtils.getRootCauseMessage(ex));
        } else if (ex instanceof EntityExistsException || ex instanceof DuplicateException
                || ((ex instanceof PersistenceException || ex instanceof DataIntegrityViolationException)
                && (ex.getCause() instanceof EntityExistsException || ex.getMessage().contains("already exists")))) {

            builder = builder(ClientExceptionType.EntityExists, getPersistenceErrorMessage(
                    ex instanceof PersistenceException || ex instanceof DataIntegrityViolationException
                            ? ex.getCause() : ex));
        } else if (ex instanceof DataIntegrityViolationException || ex instanceof UncategorizedDataAccessException) {
            builder = builder(ClientExceptionType.DataIntegrityViolation, getPersistenceErrorMessage(ex));
        } else if (ex instanceof ConnectorException) {
            builder = builder(ClientExceptionType.ConnectorException, ExceptionUtils.getRootCauseMessage(ex));
        } else if (ex instanceof NotFoundException) {
            builder = builder(ClientExceptionType.NotFound, ExceptionUtils.getRootCauseMessage(ex));
        } else {
            builder = processInvalidEntityExceptions(ex);
            if (builder == null) {
                builder = processBadRequestExceptions(ex);
            }
            // process JAX-RS validation errors
            if (builder == null && ex instanceof final ValidationException validationException) {
                builder = builder(validationEM.toResponse(validationException)).
                        header(RESTHeaders.ERROR_CODE, ClientExceptionType.RESTValidation.name()).
                        header(RESTHeaders.ERROR_INFO, ClientExceptionType.RESTValidation.getInfoHeaderValue(
                                ExceptionUtils.getRootCauseMessage(ex)));
            }
            // process web application exceptions
            if (builder == null && ex instanceof final WebApplicationException webApplicationException) {
                builder = builder(webApplicationException.getResponse()).
                        header(RESTHeaders.ERROR_CODE, ClientExceptionType.Unknown.name()).
                        header(RESTHeaders.ERROR_INFO, ClientExceptionType.Unknown.getInfoHeaderValue(
                                ExceptionUtils.getRootCauseMessage(ex)));
            }
            // ...or just report as InternalServerError
            if (builder == null) {
                builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                        header(RESTHeaders.ERROR_CODE, ClientExceptionType.Unknown.name()).
                        header(RESTHeaders.ERROR_INFO, ClientExceptionType.Unknown.getInfoHeaderValue(
                                ExceptionUtils.getRootCauseMessage(ex)));

                ErrorTO error = new ErrorTO();
                error.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
                error.setType(ClientExceptionType.Unknown);
                error.getElements().add(ExceptionUtils.getRootCauseMessage(ex));

                builder.entity(error);
            }
        }

        return Optional.ofNullable(builder).map(ResponseBuilder::build).orElse(null);
    }

    private static ResponseBuilder getSyncopeClientExceptionResponse(final SyncopeClientException ex) {
        ResponseBuilder builder = Response.status(ex.getType().getResponseStatus());
        builder.header(RESTHeaders.ERROR_CODE, ex.getType().name());

        ErrorTO error = new ErrorTO();
        error.setStatus(ex.getType().getResponseStatus().getStatusCode());
        error.setType(ex.getType());

        ex.getElements().forEach(element -> {
            builder.header(RESTHeaders.ERROR_INFO, ex.getType().getInfoHeaderValue(element));
            error.getElements().add(element);
        });

        return builder.entity(error);
    }

    private static ResponseBuilder getSyncopeClientCompositeExceptionResponse(
            final SyncopeClientCompositeException ex) {
        if (ex.getExceptions().size() == 1) {
            return getSyncopeClientExceptionResponse(ex.getExceptions().iterator().next());
        }

        ResponseBuilder builder = Response.status(Response.Status.BAD_REQUEST);

        List<ErrorTO> errors = ex.getExceptions().stream().map(sce -> {
            builder.header(RESTHeaders.ERROR_CODE, sce.getType().name());

            ErrorTO error = new ErrorTO();
            error.setStatus(sce.getType().getResponseStatus().getStatusCode());
            error.setType(sce.getType());

            sce.getElements().forEach(element -> {
                builder.header(RESTHeaders.ERROR_INFO, sce.getType().getInfoHeaderValue(element));
                error.getElements().add(element);
            });

            return error;
        }).toList();

        return builder.entity(errors);
    }

    private static ResponseBuilder processInvalidEntityExceptions(final Exception ex) {
        InvalidEntityException iee = null;

        if (ex instanceof InvalidEntityException invalidEntityException) {
            iee = invalidEntityException;
        }
        if (ex instanceof TransactionSystemException && ex.getCause() instanceof RollbackException
                && ex.getCause().getCause() instanceof final InvalidEntityException invalidEntityException) {

            iee = invalidEntityException;
        }

        if (iee != null) {
            ClientExceptionType exType;
            if (iee.getEntityClassSimpleName().endsWith("Policy")) {
                exType = ClientExceptionType.InvalidPolicy;
            } else if (iee.getEntityClassSimpleName().equals(PlainAttr.class.getSimpleName())) {
                exType = ClientExceptionType.InvalidValues;
            } else {
                try {
                    exType = ClientExceptionType.valueOf("Invalid" + iee.getEntityClassSimpleName());
                } catch (IllegalArgumentException e) {
                    // ignore
                    exType = ClientExceptionType.InvalidEntity;
                }
            }

            ResponseBuilder builder = Response.status(Response.Status.BAD_REQUEST);
            builder.header(RESTHeaders.ERROR_CODE, exType.name());

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

    private static ResponseBuilder processBadRequestExceptions(final Exception ex) {
        // This exception might be raised by Flowable (if enabled)
        Class<?> ibatisPersistenceException = null;
        try {
            ibatisPersistenceException = Class.forName("org.apache.ibatis.exceptions.PersistenceException");
        } catch (ClassNotFoundException e) {
            // ignore
        }

        if (ex instanceof WorkflowException) {
            return builder(ClientExceptionType.Workflow, ExceptionUtils.getRootCauseMessage(ex));
        }
        if (ex instanceof PersistenceException) {
            return builder(ClientExceptionType.GenericPersistence, ExceptionUtils.getRootCauseMessage(ex));
        }
        if (ibatisPersistenceException != null && ibatisPersistenceException.isAssignableFrom(ex.getClass())) {
            return builder(ClientExceptionType.Workflow, "Currently unavailable. Please try later.");
        }
        if (ex instanceof UncategorizedDataAccessException) {
            return builder(ClientExceptionType.DataIntegrityViolation, ExceptionUtils.getRootCauseMessage(ex));
        }
        if (ex instanceof ConfigurationException) {
            return builder(ClientExceptionType.InvalidConnIdConf, ExceptionUtils.getRootCauseMessage(ex));
        }
        if (ex instanceof ParsingValidationException) {
            return builder(ClientExceptionType.InvalidValues, ExceptionUtils.getRootCauseMessage(ex));
        }
        if (ex instanceof MalformedPathException) {
            return builder(ClientExceptionType.InvalidPath, ExceptionUtils.getRootCauseMessage(ex));
        }

        return null;
    }

    private static ResponseBuilder builder(final ClientExceptionType hType, final String msg) {
        ResponseBuilder builder = Response.status(hType.getResponseStatus()).
                header(RESTHeaders.ERROR_CODE, hType.name()).
                header(RESTHeaders.ERROR_INFO, hType.getInfoHeaderValue(msg));

        ErrorTO error = new ErrorTO();
        error.setStatus(hType.getResponseStatus().getStatusCode());
        error.setType(hType);
        error.getElements().add(msg);

        return builder.entity(error);
    }

    /**
     * Overriding {@link JAXRSUtils#fromResponse(jakarta.ws.rs.core.Response)} in order to avoid setting
     * {@code Content-Type} from original {@code response}.
     *
     * @param response model to construct {@link ResponseBuilder} from
     * @return new {@link ResponseBuilder} instance initialized from given response
     */
    private static ResponseBuilder builder(final Response response) {
        ResponseBuilder builder = JAXRSUtils.toResponseBuilder(response.getStatus());
        builder.entity(response.getEntity());
        response.getMetadata().forEach((key, value) -> {
            if (!HttpHeaders.CONTENT_TYPE.equals(key)) {
                value.forEach(headerValue -> builder.header(key, headerValue));
            }
        });

        return builder;
    }

    private String getPersistenceErrorMessage(final Throwable ex) {
        Throwable throwable = ExceptionUtils.getRootCause(ex);

        String message = null;
        if (throwable instanceof SQLException sqlException) {
            String messageKey = EXCEPTION_CODE_MAP.get(sqlException.getSQLState());
            if (messageKey != null) {
                message = env.getProperty("errMessage." + messageKey);
            }
        } else if (throwable instanceof EntityExistsException
                || throwable instanceof DuplicateException
                || ex.getMessage().contains("already exists")) {

            message = env.getProperty("errMessage." + UNIQUE_MSG_KEY);
        }

        return Optional.ofNullable(message).
                orElseGet(() -> Optional.ofNullable(ex.getCause()).
                map(Throwable::getMessage).
                orElseGet(ex::getMessage));
    }
}
