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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.validation.ValidationExceptionMapper;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ErrorTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.attrvalue.validation.ParsingValidationException;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.MalformedPathException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
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
public class RestServiceExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = LoggerFactory.getLogger(RestServiceExceptionMapper.class);

    private final ValidationExceptionMapper validationEM = new ValidationExceptionMapper();

    @Autowired
    private Environment env;

    private static final Map<String, String> EXCEPTION_CODE_MAP = new HashMap<String, String>() {

        private static final long serialVersionUID = -7688359318035249200L;

        {
            put("23000", "UniqueConstraintViolation");
            put("23505", "UniqueConstraintViolation");
        }
    };

    @Override
    public Response toResponse(final Exception ex) {
        LOG.error("Exception thrown", ex);

        ResponseBuilder builder;

        if (ex instanceof AccessDeniedException) {
            // leaves the default exception processing to Spring Security
            builder = null;
        } else if (ex instanceof SyncopeClientException) {
            SyncopeClientException sce = (SyncopeClientException) ex;
            builder = sce.isComposite()
                    ? getSyncopeClientCompositeExceptionResponse(sce.asComposite())
                    : getSyncopeClientExceptionResponse(sce);
        } else if (ex instanceof DelegatedAdministrationException
                || ExceptionUtils.getRootCause(ex) instanceof DelegatedAdministrationException) {

            builder = builder(ClientExceptionType.DelegatedAdministration, ExceptionUtils.getRootCauseMessage(ex));
        } else if (ex instanceof EntityExistsException || ex instanceof DuplicateException
                || ex instanceof PersistenceException && ex.getCause() instanceof EntityExistsException) {

            builder = builder(ClientExceptionType.EntityExists,
                    getJPAMessage(ex instanceof PersistenceException ? ex.getCause() : ex));
        } else if (ex instanceof DataIntegrityViolationException || ex instanceof JpaSystemException) {
            builder = builder(ClientExceptionType.DataIntegrityViolation, getJPAMessage(ex));
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
            if (builder == null && ex instanceof ValidationException) {
                builder = builder(validationEM.toResponse((ValidationException) ex)).
                        header(RESTHeaders.ERROR_CODE, ClientExceptionType.RESTValidation.name()).
                        header(RESTHeaders.ERROR_INFO, ClientExceptionType.RESTValidation.getInfoHeaderValue(
                                ExceptionUtils.getRootCauseMessage(ex)));

                ErrorTO error = new ErrorTO();
                error.setStatus(ClientExceptionType.RESTValidation.getResponseStatus().getStatusCode());
                error.setType(ClientExceptionType.RESTValidation);
                error.getElements().add(ExceptionUtils.getRootCauseMessage(ex));

                builder.entity(error);
            }
            // ...or just report as InternalServerError
            if (builder == null) {
                builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                        header(RESTHeaders.ERROR_INFO, ClientExceptionType.Unknown.getInfoHeaderValue(
                                ExceptionUtils.getRootCauseMessage(ex)));

                ErrorTO error = new ErrorTO();
                error.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
                error.setType(ClientExceptionType.Unknown);
                error.getElements().add(ExceptionUtils.getRootCauseMessage(ex));

                builder.entity(error);
            }
        }

        return builder == null ? null : builder.build();
    }

    private ResponseBuilder getSyncopeClientExceptionResponse(final SyncopeClientException ex) {
        ResponseBuilder builder = Response.status(ex.getType().getResponseStatus());
        builder.header(RESTHeaders.ERROR_CODE, ex.getType().name());

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

        List<ErrorTO> errors = new ArrayList<>();
        for (SyncopeClientException sce : ex.getExceptions()) {
            builder.header(RESTHeaders.ERROR_CODE, sce.getType().name());

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
            ClientExceptionType exType = iee.getEntityClassSimpleName().endsWith("Policy")
                    ? ClientExceptionType.InvalidPolicy
                    : iee.getEntityClassSimpleName().equals(PlainAttr.class.getSimpleName())
                    ? ClientExceptionType.InvalidValues
                    : ClientExceptionType.valueOf("Invalid" + iee.getEntityClassSimpleName());

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

    private ResponseBuilder processBadRequestExceptions(final Exception ex) {
        // This exception might be raised by Flowable (if enabled)
        Class<?> ibatisPersistenceException = null;
        try {
            ibatisPersistenceException = Class.forName("org.apache.ibatis.exceptions.PersistenceException");
        } catch (ClassNotFoundException e) {
            // ignore
        }

        if (ex instanceof WorkflowException) {
            return builder(ClientExceptionType.Workflow, ExceptionUtils.getRootCauseMessage(ex));
        } else if (ex instanceof PersistenceException) {
            return builder(ClientExceptionType.GenericPersistence, ExceptionUtils.getRootCauseMessage(ex));
        } else if (ibatisPersistenceException != null && ibatisPersistenceException.isAssignableFrom(ex.getClass())) {
            return builder(ClientExceptionType.Workflow, "Currently unavailable. Please try later.");
        } else if (ex instanceof JpaSystemException) {
            return builder(ClientExceptionType.DataIntegrityViolation, ExceptionUtils.getRootCauseMessage(ex));
        } else if (ex instanceof ConfigurationException) {
            return builder(ClientExceptionType.InvalidConnIdConf, ExceptionUtils.getRootCauseMessage(ex));
        } else if (ex instanceof ParsingValidationException) {
            return builder(ClientExceptionType.InvalidValues, ExceptionUtils.getRootCauseMessage(ex));
        } else if (ex instanceof MalformedPathException) {
            return builder(ClientExceptionType.InvalidPath, ExceptionUtils.getRootCauseMessage(ex));
        }

        return null;
    }

    private ResponseBuilder builder(final ClientExceptionType hType, final String msg) {
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
     * Overriding {@link JAXRSUtils#fromResponse(javax.ws.rs.core.Response)} in order to avoid setting
     * {@code Content-Type} from original {@code response}.
     *
     * @param response model to construct {@link ResponseBuilder} from
     * @return new {@link ResponseBuilder} instance initialized from given response
     */
    private ResponseBuilder builder(final Response response) {
        ResponseBuilder builder = JAXRSUtils.toResponseBuilder(response.getStatus());
        builder.entity(response.getEntity());
        for (Map.Entry<String, List<Object>> entry : response.getMetadata().entrySet()) {
            if (!HttpHeaders.CONTENT_TYPE.equals(entry.getKey())) {
                for (Object value : entry.getValue()) {
                    builder.header(entry.getKey(), value);
                }
            }
        }

        return builder;
    }

    private String getJPAMessage(final Throwable ex) {
        Throwable throwable = ExceptionUtils.getRootCause(ex);
        String message = null;
        if (throwable instanceof SQLException) {
            String messageKey = EXCEPTION_CODE_MAP.get(((SQLException) throwable).getSQLState());
            if (messageKey != null) {
                message = env.getProperty("errMessage." + messageKey);
            }
        }

        return message == null
                ? (ex.getCause() == null) ? ex.getMessage() : ex.getCause().getMessage()
                : message;
    }
}
