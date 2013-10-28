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
package org.apache.syncope.client.rest;

import java.security.AccessControlException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.xml.ws.WebServiceException;
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class RestClientExceptionMapper implements ExceptionMapper<Exception>, ResponseExceptionMapper<Exception> {

    private static final Logger LOG = LoggerFactory.getLogger(RestClientExceptionMapper.class);

    @Override
    public Response toResponse(final Exception exception) {
        throw new UnsupportedOperationException(
                "Call of toResponse() method is not expected in RestClientExceptionnMapper");
    }

    @Override
    public Exception fromResponse(final Response response) {
        final int statusCode = response.getStatus();
        Exception ex;

        // 1. Check for client (possibly composite) exception in HTTP header
        SyncopeClientCompositeException scce = checkSyncopeClientCompositeException(response);
        if (scce != null) {
            if (scce.getExceptions().size() == 1) {
                ex = scce.getExceptions().iterator().next();
            } else {
                ex = scce;
            }
        } // 2. Map SC_UNAUTHORIZED
        else if (statusCode == Response.Status.UNAUTHORIZED.getStatusCode()) {
            ex = new AccessControlException("Remote unauthorized exception");
        } // 3. Map SC_BAD_REQUEST
        else if (statusCode == Response.Status.BAD_REQUEST.getStatusCode()) {
            ex = new BadRequestException();
        } // 4. All other codes are mapped to runtime exception with HTTP code information
        else {
            ex = new WebServiceException(String.format("Remote exception with status code: %s",
                    Response.Status.fromStatusCode(statusCode).name()));
        }
        LOG.error("Exception thrown by REST methods: " + ex.getMessage(), ex);
        return ex;
    }

    private SyncopeClientCompositeException checkSyncopeClientCompositeException(final Response response) {
        List<Object> exTypesInHeaders = response.getHeaders().get(SyncopeConstants.REST_EXCEPTION_TYPE_HEADER);
        if (exTypesInHeaders == null) {
            LOG.debug("No " + SyncopeConstants.REST_EXCEPTION_TYPE_HEADER + " provided");
            return null;
        }

        final SyncopeClientCompositeException compException = SyncopeClientException.buildComposite();

        final Set<String> handledExceptions = new HashSet<String>();
        for (Object exceptionTypeValue : exTypesInHeaders) {
            final String exTypeAsString = (String) exceptionTypeValue;
            ClientExceptionType exceptionType = null;
            try {
                exceptionType = ClientExceptionType.fromHeaderValue(exTypeAsString);
            } catch (IllegalArgumentException e) {
                LOG.error("Unexpected value of " + SyncopeConstants.REST_EXCEPTION_TYPE_HEADER + ": "
                        + exTypeAsString, e);
            }
            if (exceptionType != null) {
                handledExceptions.add(exTypeAsString);

                final SyncopeClientException clientException = SyncopeClientException.build(exceptionType);

                if (response.getHeaders().get(exceptionType.getElementHeaderName()) != null
                        && !response.getHeaders().get(exceptionType.getElementHeaderName()).isEmpty()) {

                    clientException.getElements().addAll(
                            response.getHeaders().get(exceptionType.getElementHeaderName()));
                }
                compException.addException(clientException);
            }
        }

        exTypesInHeaders.removeAll(handledExceptions);
        if (!exTypesInHeaders.isEmpty()) {
            LOG.error("Unmanaged exceptions: " + exTypesInHeaders);
        }

        if (compException.hasExceptions()) {
            return compException;
        }

        return null;
    }
}
