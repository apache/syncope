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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.annotations.Resource;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.wicket.request.resource.AbstractResource;

@Resource(key = "groups", path = "/api/groups")
public class GroupResource extends BaseResource {

    private static final long serialVersionUID = 7475706378304995200L;

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes) {
        LOG.debug("Search all available groups");

        ResourceResponse response = new ResourceResponse();
        response.setContentType(MediaType.APPLICATION_JSON);
        try {
            HttpServletRequest request = (HttpServletRequest) attributes.getRequest().getContainerRequest();
            if (!xsrfCheck(request)) {
                LOG.error("XSRF TOKEN does not match");
                response.setError(Response.Status.BAD_REQUEST.getStatusCode(), "XSRF TOKEN does not match");
                return response;
            }

            String realm = URLDecoder.decode(attributes.getParameters().get("realm").
                    toString(SyncopeConstants.ROOT_REALM), "UTF-8");
            final List<GroupTO> groupTOs = SyncopeEnduserSession.get().
                    getService(SyncopeService.class).searchAssignableGroups(realm, 1, 30).getResult();
            response.setTextEncoding(StandardCharsets.UTF_8.name());
            response.setWriteCallback(new AbstractResource.WriteCallback() {

                @Override
                public void writeData(final Attributes attributes) throws IOException {
                    attributes.getResponse().write(MAPPER.writeValueAsString(groupTOs));
                }
            });
            response.setStatusCode(Response.Status.OK.getStatusCode());
        } catch (Exception e) {
            LOG.error("Error retrieving available groups", e);
            response.setError(Response.Status.BAD_REQUEST.getStatusCode(), new StringBuilder()
                    .append("ErrorMessage{{ ")
                    .append(e.getMessage())
                    .append(" }}")
                    .toString());
        }
        return response;
    }

}
