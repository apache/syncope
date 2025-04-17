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
package org.apache.syncope.client.lib;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.ws.WebServiceException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ErrorTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class RestClientExceptionMapper implements ResponseExceptionMapper<Exception> {

    private static final Logger LOG = LoggerFactory.getLogger(RestClientExceptionMapper.class);

    @Override
    public Exception fromResponse(final Response response) {
        int statusCode = response.getStatus();
        String message = response.getHeaderString(RESTHeaders.ERROR_INFO);

        Exception ex;
        SyncopeClientCompositeException scce = checkSyncopeClientCompositeException(response);
        if (scce != null) {
            // 1. Check for client (possibly composite) exception in HTTP header
            ex = scce.getExceptions().size() == 1
                    ? scce.getExceptions().iterator().next()
                    : scce;
        } else if (statusCode == Response.Status.UNAUTHORIZED.getStatusCode()) {
            // 2. Map SC_UNAUTHORIZED
            ex = new NotAuthorizedException(StringUtils.isBlank(message)
                    ? "Remote unauthorized exception"
                    : message,
                    Response.status(Response.Status.UNAUTHORIZED).build());
        } else if (statusCode == Response.Status.FORBIDDEN.getStatusCode()) {
            // 3. Map SC_FORBIDDEN
            ex = new ForbiddenException(StringUtils.isBlank(message)
                    ? "Remote forbidden exception"
                    : message);
        } else if (statusCode == Response.Status.BAD_REQUEST.getStatusCode()) {
            // 4. Map SC_BAD_REQUEST
            ex = StringUtils.isBlank(message)
                    ? new BadRequestException()
                    : new BadRequestException(message);
        } else {
            // 5. All other codes are mapped to runtime exception with HTTP code information
            ex = new WebServiceException(String.format("Remote exception with status code: %s",
                    Response.Status.fromStatusCode(statusCode).name()));
        }

        if (ex instanceof NotFoundException
                || (ex instanceof SyncopeClientException sce && sce.getType() == ClientExceptionType.NotFound)) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Exception thrown", ex);
            } else {
                LOG.warn("{} thrown: {}", NotFoundException.class.getSimpleName(), ex.getMessage());
            }
        } else {
            LOG.error("Exception thrown", ex);
        }

        return ex;
    }

    private static SyncopeClientCompositeException checkSyncopeClientCompositeException(final Response response) {
        SyncopeClientCompositeException compException = SyncopeClientException.buildComposite();

        // Attempts to read ErrorTO or List<ErrorTO> as entity...
        List<ErrorTO> errors = null;
        try {
            ErrorTO error = response.readEntity(ErrorTO.class);
            if (error != null) {
                errors = List.of(error);
            }
        } catch (Exception e) {
            LOG.debug("Could not read {}, attempting to read composite...", ErrorTO.class.getName(), e);
        }
        if (errors == null) {
            try {
                errors = response.readEntity(new GenericType<>() {
                });
            } catch (Exception e) {
                LOG.debug("Could not read {} list, attempting to read headers...", ErrorTO.class.getName(), e);
            }
        }

        // ...if not possible, attempts to parse response headers
        if (errors == null) {
            List<String> exTypesInHeaders = response.getStringHeaders().get(RESTHeaders.ERROR_CODE);
            if (exTypesInHeaders == null) {
                LOG.debug("No " + RESTHeaders.ERROR_CODE + " provided");
                return null;
            }
            List<String> exInfos = response.getStringHeaders().get(RESTHeaders.ERROR_INFO);

            Set<String> handledExceptions = new HashSet<>();
            exTypesInHeaders.forEach(exTypeAsString -> {
                ClientExceptionType exceptionType = null;
                try {
                    exceptionType = ClientExceptionType.fromHeaderValue(exTypeAsString);
                } catch (IllegalArgumentException e) {
                    LOG.error("Unexpected value of " + RESTHeaders.ERROR_CODE + ": {}", exTypeAsString, e);
                }
                if (exceptionType != null) {
                    handledExceptions.add(exTypeAsString);

                    SyncopeClientException clientException = SyncopeClientException.build(exceptionType);
                    if (exInfos != null && !exInfos.isEmpty()) {
                        for (String element : exInfos) {
                            if (element.startsWith(exceptionType.name())) {
                                clientException.getElements().add(StringUtils.substringAfter(element, ":"));
                            }
                        }
                    }
                    compException.addException(clientException);
                }
            });

            exTypesInHeaders.removeAll(handledExceptions);
            if (!exTypesInHeaders.isEmpty()) {
                LOG.error("Unmanaged exceptions: {}", exTypesInHeaders);
            }
        } else {
            for (ErrorTO error : errors) {
                SyncopeClientException clientException = SyncopeClientException.build(error.getType());
                clientException.getElements().addAll(error.getElements());
                compException.addException(clientException);
            }
        }

        if (compException.hasExceptions()) {
            return compException;
        }

        return null;
    }
}
