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

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ErrorTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.core.persistence.api.DomainHolder;

/**
 * Checks that requested Domain exists.
 */
@Provider
@PreMatching
public class CheckDomainFilter implements ContainerRequestFilter {

    protected final DomainHolder domainHolder;

    public CheckDomainFilter(final DomainHolder domainHolder) {
        this.domainHolder = domainHolder;
    }

    @Override
    public void filter(final ContainerRequestContext reqContext) throws IOException {
        String domain = reqContext.getHeaderString(RESTHeaders.DOMAIN);
        if (domain != null && !SyncopeConstants.MASTER_DOMAIN.equals(domain)) {
            if (!domainHolder.getDomains().containsKey(domain)) {
                String message = "Domain '" + domain + "' not available";

                ErrorTO error = new ErrorTO();
                error.setStatus(Response.Status.NOT_FOUND.getStatusCode());
                error.setType(ClientExceptionType.NotFound);
                error.getElements().add(message);

                reqContext.abortWith(Response.status(Response.Status.NOT_FOUND).
                        entity(error).
                        header(HttpHeaders.CONTENT_TYPE,
                                reqContext.getAcceptableMediaTypes().isEmpty()
                                ? MediaType.APPLICATION_JSON
                                : reqContext.getAcceptableMediaTypes().get(0).toString()).
                        header(RESTHeaders.ERROR_CODE,
                                ClientExceptionType.NotFound.name()).
                        header(RESTHeaders.ERROR_INFO,
                                ClientExceptionType.NotFound.getInfoHeaderValue(message)).
                        build());
            }
        }
    }
}
