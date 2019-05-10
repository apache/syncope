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
package org.apache.syncope.ext.self.keymaster.cxf;

import javax.validation.ValidationException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

@Provider
public class SelfKeymasterExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = LoggerFactory.getLogger(SelfKeymasterExceptionMapper.class);

    @Override
    public Response toResponse(final Exception ex) {
        LOG.error("Exception thrown", ex);

        ResponseBuilder builder = null;

        if (ex instanceof AccessDeniedException
                || ex instanceof ForbiddenException
                || ex instanceof NotAuthorizedException) {

            // leaves the default exception processing
            builder = null;
        } else if (ex instanceof NotFoundException) {
            builder = Response.status(Response.Status.NOT_FOUND).
                    entity(ExceptionUtils.getRootCauseMessage(ex));
        } else if (ex instanceof KeymasterException) {
            builder = Response.status(Response.Status.BAD_REQUEST).
                    entity(ExceptionUtils.getRootCauseMessage(ex));
        } else if (ex instanceof SyncopeClientException) {
            SyncopeClientException sce = (SyncopeClientException) ex;
            builder = builder(sce.getType(), ExceptionUtils.getRootCauseMessage(ex));
        } else {
            // process JAX-RS validation errors
            if (ex instanceof ValidationException) {
                builder = builder(ClientExceptionType.RESTValidation, ExceptionUtils.getRootCauseMessage(ex));
            }
            // ...or just report as InternalServerError
            if (builder == null) {
                builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                        entity(ExceptionUtils.getRootCauseMessage(ex));
            }
        }

        return builder == null ? null : builder.build();
    }

    private ResponseBuilder builder(final ClientExceptionType type, final String msg) {
        ResponseBuilder builder = Response.status(type.getResponseStatus());

        return builder.entity(msg);
    }
}
