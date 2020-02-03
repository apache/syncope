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
package org.apache.syncope.ext.scimv2.cxf;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.ValidationException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.attrvalue.validation.ParsingValidationException;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.MalformedPathException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.workflow.api.WorkflowException;
import org.apache.syncope.ext.scimv2.api.BadRequestException;
import org.apache.syncope.ext.scimv2.api.data.SCIMError;
import org.apache.syncope.ext.scimv2.api.type.ErrorType;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionSystemException;

@Provider
public class SCIMExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = LoggerFactory.getLogger(SCIMExceptionMapper.class);

    private static Class<?> ENTITYEXISTS_EXCLASS = null;

    private static Class<?> PERSISTENCE_EXCLASS = null;

    private static Class<?> ROLLBACK_EXCLASS = null;

    private static Class<?> JPASYSTEM_EXCLASS = null;

    private static Class<?> CONNECTOR_EXCLASS = null;

    private static Class<?> IBATISPERSISTENCE_EXCLASS = null;

    static {
        try {
            ENTITYEXISTS_EXCLASS = Class.forName("javax.persistence.EntityExistsException");
            PERSISTENCE_EXCLASS = Class.forName("javax.persistence.PersistenceException");
            ROLLBACK_EXCLASS = Class.forName("javax.persistence.RollbackException");
            JPASYSTEM_EXCLASS = Class.forName("org.springframework.orm.jpa.JpaSystemException");
            CONNECTOR_EXCLASS = Class.forName("org.identityconnectors.framework.common.exceptions.ConnectorException");
            IBATISPERSISTENCE_EXCLASS = Class.forName("org.apache.ibatis.exceptions.PersistenceException");
        } catch (ClassNotFoundException e) {
            // ignore
        }
    }

    @Override
    public Response toResponse(final Exception ex) {
        LOG.error("Exception thrown", ex);

        ResponseBuilder builder;

        if (ex instanceof AccessDeniedException
                || ex instanceof ForbiddenException
                || ex instanceof NotAuthorizedException) {

            // leaves the default exception processing
            builder = null;
        } else if (ex instanceof NotFoundException) {
            return Response.status(Response.Status.NOT_FOUND).entity(new SCIMError(null,
                    Response.Status.NOT_FOUND.getStatusCode(), ExceptionUtils.getRootCauseMessage(ex))).
                    build();
        } else if (ex instanceof SyncopeClientException) {
            SyncopeClientException sce = (SyncopeClientException) ex;
            builder = builder(sce.getType(), ExceptionUtils.getRootCauseMessage(ex));
        } else if (ex instanceof DelegatedAdministrationException
                || ExceptionUtils.getRootCause(ex) instanceof DelegatedAdministrationException) {

            builder = builder(ClientExceptionType.DelegatedAdministration, ExceptionUtils.getRootCauseMessage(ex));
        } else if (ENTITYEXISTS_EXCLASS.isAssignableFrom(ex.getClass())
                || ex instanceof DuplicateException
                || PERSISTENCE_EXCLASS.isAssignableFrom(ex.getClass())
                && ENTITYEXISTS_EXCLASS.isAssignableFrom(ex.getCause().getClass())) {

            builder = builder(ClientExceptionType.EntityExists, ExceptionUtils.getRootCauseMessage(ex));
        } else if (ex instanceof DataIntegrityViolationException || JPASYSTEM_EXCLASS.isAssignableFrom(ex.getClass())) {
            builder = builder(ClientExceptionType.DataIntegrityViolation, ExceptionUtils.getRootCauseMessage(ex));
        } else if (CONNECTOR_EXCLASS.isAssignableFrom(ex.getClass())) {
            builder = builder(ClientExceptionType.ConnectorException, ExceptionUtils.getRootCauseMessage(ex));
        } else {
            builder = processInvalidEntityExceptions(ex);
            if (builder == null) {
                builder = processBadRequestExceptions(ex);
            }
            // process JAX-RS validation errors
            if (builder == null && ex instanceof ValidationException) {
                builder = builder(ClientExceptionType.RESTValidation, ExceptionUtils.getRootCauseMessage(ex));
            }
            // ...or just report as InternalServerError
            if (builder == null) {
                builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                        entity(ExceptionUtils.getRootCauseMessage(ex));
            }
        }

        return Optional.ofNullable(builder).map(ResponseBuilder::build).orElse(null);
    }

    private static ResponseBuilder processInvalidEntityExceptions(final Exception ex) {
        InvalidEntityException iee = null;

        if (ex instanceof InvalidEntityException) {
            iee = (InvalidEntityException) ex;
        }
        if (ex instanceof TransactionSystemException && ROLLBACK_EXCLASS.isAssignableFrom(ex.getCause().getClass())
                && ex.getCause().getCause() instanceof InvalidEntityException) {

            iee = (InvalidEntityException) ex.getCause().getCause();
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

            StringBuilder msg = new StringBuilder();

            for (Map.Entry<Class<?>, Set<EntityViolationType>> violation : iee.getViolations().entrySet()) {
                for (EntityViolationType violationType : violation.getValue()) {
                    msg.append(violationType.name()).append(": ").append(violationType.getMessage()).append('\n');
                }
            }

            return builder(exType, msg.toString());
        }

        return null;
    }

    private static ResponseBuilder processBadRequestExceptions(final Exception ex) {
        if (ex instanceof WorkflowException) {
            return builder(ClientExceptionType.Workflow, ExceptionUtils.getRootCauseMessage(ex));
        } else if (PERSISTENCE_EXCLASS.isAssignableFrom(ex.getClass())) {
            return builder(ClientExceptionType.GenericPersistence, ExceptionUtils.getRootCauseMessage(ex));
        } else if (IBATISPERSISTENCE_EXCLASS != null && IBATISPERSISTENCE_EXCLASS.isAssignableFrom(ex.getClass())) {
            return builder(ClientExceptionType.Workflow, "Currently unavailable. Please try later.");
        } else if (JPASYSTEM_EXCLASS.isAssignableFrom(ex.getClass())) {
            return builder(ClientExceptionType.DataIntegrityViolation, ExceptionUtils.getRootCauseMessage(ex));
        } else if (ex instanceof ConfigurationException) {
            return builder(ClientExceptionType.InvalidConnIdConf, ExceptionUtils.getRootCauseMessage(ex));
        } else if (ex instanceof ParsingValidationException) {
            return builder(ClientExceptionType.InvalidValues, ExceptionUtils.getRootCauseMessage(ex));
        } else if (ex instanceof MalformedPathException) {
            return builder(ClientExceptionType.InvalidPath, ExceptionUtils.getRootCauseMessage(ex));
        } else if (ex instanceof BadRequestException) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new SCIMError((BadRequestException) ex));
        }

        return null;
    }

    private static ResponseBuilder builder(final ClientExceptionType hType, final String msg) {
        ResponseBuilder builder = Response.status(hType.getResponseStatus());

        ErrorType scimType = null;
        if (hType.name().startsWith("Invalid") || hType == ClientExceptionType.RESTValidation) {
            scimType = ErrorType.invalidValue;
        } else if (hType == ClientExceptionType.EntityExists) {
            scimType = ErrorType.uniqueness;
        }

        return builder.entity(new SCIMError(scimType, hType.getResponseStatus().getStatusCode(), msg));
    }
}
