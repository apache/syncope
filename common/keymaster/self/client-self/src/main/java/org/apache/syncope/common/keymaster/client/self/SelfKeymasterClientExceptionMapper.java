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
package org.apache.syncope.common.keymaster.client.self;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.ws.WebServiceException;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class SelfKeymasterClientExceptionMapper implements ResponseExceptionMapper<Exception> {

    private static final Logger LOG = LoggerFactory.getLogger(SelfKeymasterClientExceptionMapper.class);

    @Override
    public Exception fromResponse(final Response response) {
        int statusCode = response.getStatus();
        String message = response.readEntity(String.class);

        Exception ex;
        if (statusCode == Response.Status.UNAUTHORIZED.getStatusCode()) {
            // 1. Map SC_UNAUTHORIZED
            ex = new NotAuthorizedException(StringUtils.isBlank(message)
                    ? "Remote unauthorized exception"
                    : message);
        } else if (statusCode == Response.Status.FORBIDDEN.getStatusCode()) {
            // 2. Map SC_FORBIDDEN
            ex = new ForbiddenException(StringUtils.isBlank(message)
                    ? "Remote forbidden exception"
                    : message);
        } else if (statusCode == Response.Status.NOT_FOUND.getStatusCode()) {
            // 3. Map SC_NOT_FOUND
            ex = StringUtils.isBlank(message)
                    ? new NotFoundException()
                    : new NotFoundException(message);
        } else if (statusCode == Response.Status.BAD_REQUEST.getStatusCode()) {
            // 4. Map SC_BAD_REQUEST
            ex = StringUtils.isBlank(message)
                    ? new BadRequestException()
                    : message.contains(KeymasterException.class.getSimpleName())
                    ? new KeymasterException(message)
                    : new BadRequestException(message);
        } else {
            // 5. All other codes are mapped to runtime exception with HTTP code information
            ex = new WebServiceException(String.format("Remote exception with status code: %s",
                    Response.Status.fromStatusCode(statusCode).name()));
        }
        LOG.error("Exception thrown", ex);
        return ex;
    }
}
