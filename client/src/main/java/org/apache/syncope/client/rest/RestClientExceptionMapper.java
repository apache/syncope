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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.xml.ws.WebServiceException;
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
import org.apache.http.HttpStatus;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.common.validation.SyncopeClientErrorHandler;
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
        Exception ex = null;
        final int statusCode = response.getStatus();

        // 1. Check for composite exception in HTTP header
        SyncopeClientCompositeErrorException scce = checkCompositeException(response);
        if (scce != null) {
            ex = scce;

            // TODO reduce SCCEE to really composite ones and use normal exception for others
            // } else if (statusCode == HttpStatus.SC_FORBIDDEN) {
            // ex = new UnauthorizedRoleException(-1L);

            // 2. Map SC_UNAUTHORIZED
        } else if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
            ex = new AccessControlException("Remote unauthorized exception");

            // 3. Map SC_BAD_REQUEST
        } else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
            ex = new BadRequestException();

        } else {
            // 4. All other codes are mapped to runtime exception with HTTP code information
            ex = new WebServiceException(String.format("Remote exception with status code: %s",
                    Response.Status.fromStatusCode(statusCode).name()));
        }
        LOG.error("Exception thrown by REST methods: " + ex.getMessage(), ex);
        return ex;
    }

    private SyncopeClientCompositeErrorException checkCompositeException(final Response response) {
        final int statusCode = response.getStatus();
        List<Object> exTypesInHeaders = response.getHeaders().get(SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER);
        if (exTypesInHeaders == null) {
            LOG.debug("No " + SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER + " provided");
            return null;
        }

        final SyncopeClientCompositeErrorException compException = new SyncopeClientCompositeErrorException(
                org.springframework.http.HttpStatus.valueOf(statusCode));

        final Set<String> handledExceptions = new HashSet<String>();
        for (Object exceptionTypeValue : exTypesInHeaders) {
            final String exTypeAsString = (String) exceptionTypeValue;
            SyncopeClientExceptionType exceptionType = null;
            try {
                exceptionType = SyncopeClientExceptionType.getFromHeaderValue(exTypeAsString);
            } catch (IllegalArgumentException e) {
                LOG.error("Unexpected value of " + SyncopeClientErrorHandler.EXCEPTION_TYPE_HEADER + ": "
                        + exTypeAsString, e);
            }
            if (exceptionType != null) {
                handledExceptions.add(exTypeAsString);

                final SyncopeClientException clientException = new SyncopeClientException();
                clientException.setType(exceptionType);
                if (response.getHeaders().get(exceptionType.getElementHeaderName()) != null
                        && !response.getHeaders().get(exceptionType.getElementHeaderName()).isEmpty()) {
                    // TODO update clientException to support list of objects
                    final List<Object> elObjectList = response.getHeaders().get(exceptionType.getElementHeaderName());
                    final List<String> elStringList = new ArrayList<String>();
                    for (Object elementObject : elObjectList) {
                        if (elementObject instanceof String) {
                            elStringList.add((String) elementObject);
                        }
                    }
                    clientException.setElements(elStringList);
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
