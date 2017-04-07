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
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.annotations.Resource;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;

@Resource(key = "userSelfRead", path = "/api/self/read")
public class UserSelfReadResource extends BaseUserSelfResource {

    private static final long serialVersionUID = -9184809392631523912L;

    @Override
    protected ResourceResponse newResourceResponse(final IResource.Attributes attributes) {
        LOG.debug("Requested user self information");

        ResourceResponse response = new AbstractResource.ResourceResponse();
        response.setContentType(MediaType.APPLICATION_JSON);
        try {
            HttpServletRequest request = (HttpServletRequest) attributes.getRequest().getContainerRequest();
            if (!xsrfCheck(request)) {
                LOG.error("XSRF TOKEN does not match");
                response.setError(Response.Status.BAD_REQUEST.getStatusCode(), "XSRF TOKEN does not match");
                return response;
            }

            UserTO userTO = SerializationUtils.clone(SyncopeEnduserSession.get().getSelfTO());
            Map<String, AttrTO> userPlainAttrMap = userTO.getPlainAttrMap();

            // 1. Date -> millis conversion for PLAIN attributes of USER and its MEMBERSHIPS
            for (PlainSchemaTO plainSchema : SyncopeEnduserSession.get().getDatePlainSchemas()) {
                dateToMillis(userPlainAttrMap, plainSchema);
                for (MembershipTO membership : userTO.getMemberships()) {
                    dateToMillis(membership.getPlainAttrMap(), plainSchema);
                }
            }

            // 2. membership attributes management
            for (MembershipTO membership : userTO.getMemberships()) {
                String groupName = membership.getGroupName();
                for (AttrTO attr : membership.getPlainAttrs()) {
                    attr.setSchema(groupName.concat("#").concat(attr.getSchema()));
                    userTO.getPlainAttrs().add(attr);
                }
                membership.getPlainAttrs().clear();
                for (AttrTO attr : membership.getDerAttrs()) {
                    attr.setSchema(groupName.concat("#").concat(attr.getSchema()));
                    userTO.getDerAttrs().add(attr);
                }
                membership.getDerAttrs().clear();
                for (AttrTO attr : membership.getVirAttrs()) {
                    attr.setSchema(groupName.concat("#").concat(attr.getSchema()));
                    userTO.getVirAttrs().add(attr);
                }
                membership.getVirAttrs().clear();
            }

            final String selfTOJson = MAPPER.writeValueAsString(userTO);
            response.setContentType(MediaType.APPLICATION_JSON);
            response.setTextEncoding(StandardCharsets.UTF_8.name());

            response.setWriteCallback(new WriteCallback() {

                @Override
                public void writeData(final Attributes attributes) throws IOException {
                    attributes.getResponse().write(selfTOJson);
                }
            });
            response.setStatusCode(Response.Status.OK.getStatusCode());
        } catch (Exception e) {
            LOG.error("Error retrieving selfTO", e);
            response.setError(Response.Status.BAD_REQUEST.getStatusCode(), new StringBuilder()
                    .append("ErrorMessage{{ ")
                    .append(e.getMessage())
                    .append(" }}")
                    .toString());
        }
        return response;
    }

}
