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
package org.apache.syncope.client.enduser.resources;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserConstants;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.adapters.PlatformInfoAdapter;
import org.apache.syncope.client.enduser.annotations.Resource;
import org.apache.syncope.client.enduser.util.SaltGenerator;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.util.cookies.CookieUtils;

@Resource(key = "info", path = "/api/info")
public class InfoResource extends BaseResource {

    private static final long serialVersionUID = 6453101466981543020L;

    @Override
    protected ResourceResponse newResourceResponse(final IResource.Attributes attributes) {
        ResourceResponse response = new ResourceResponse();
        response.setContentType(MediaType.APPLICATION_JSON);

        try {
            final CookieUtils sessionCookieUtils = SyncopeEnduserSession.get().getCookieUtils();
            // set XSRF_TOKEN cookie
            if (!SyncopeEnduserSession.get().isXsrfTokenGenerated() && (sessionCookieUtils.getCookie(
                    SyncopeEnduserConstants.XSRF_COOKIE) == null || StringUtils.isBlank(
                            sessionCookieUtils.getCookie(SyncopeEnduserConstants.XSRF_COOKIE).getValue()))) {
                LOG.debug("Set XSRF-TOKEN cookie");
                SyncopeEnduserSession.get().setXsrfTokenGenerated(true);
                sessionCookieUtils.save(SyncopeEnduserConstants.XSRF_COOKIE, SaltGenerator.generate(
                        SyncopeEnduserSession.get().getId()));
            }
            response.setTextEncoding(StandardCharsets.UTF_8.name());
            response.setWriteCallback(new WriteCallback() {

                @Override
                public void writeData(final IResource.Attributes attributes) throws IOException {
                    attributes.getResponse().write(
                            MAPPER.writeValueAsString(
                                    PlatformInfoAdapter.toPlatformInfoRequest(
                                            SyncopeEnduserSession.get().getPlatformInfo())));
                }
            });
            response.setStatusCode(Response.Status.OK.getStatusCode());
        } catch (Exception e) {
            LOG.error("Error retrieving syncope info", e);
            response.setError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), new StringBuilder()
                    .append("ErrorMessage{{ ")
                    .append(e.getMessage())
                    .append(" }}")
                    .toString());
        }

        return response;
    }
}
