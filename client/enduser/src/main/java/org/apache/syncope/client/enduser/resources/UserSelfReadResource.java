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
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserApplication;
import org.apache.syncope.client.enduser.SyncopeEnduserConstants;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.annotations.Resource;
import org.apache.syncope.client.enduser.model.CustomAttributesInfo;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.SchemaType;
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

            // 1. Date -> millis conversion for PLAIN MEMBERSHIPS attributes of USER
            for (PlainSchemaTO plainSchema : SyncopeEnduserSession.get().getDatePlainSchemas()) {
                for (MembershipTO membership : userTO.getMemberships()) {
                    dateToMillis(membership.getPlainAttrs(), plainSchema);
                }
            }

            // 2. membership attributes management
            for (MembershipTO membership : userTO.getMemberships()) {
                String groupName = membership.getGroupName();
                membership.getPlainAttrs().stream().map(attr -> {
                    attr.setSchema(groupName.concat(SyncopeEnduserConstants.MEMBERSHIP_ATTR_SEPARATOR).
                            concat(attr.getSchema()));
                    return attr;
                }).forEachOrdered(attr -> {
                    userTO.getPlainAttrs().add(attr);
                });
                membership.getPlainAttrs().clear();
                membership.getDerAttrs().stream().map(attr -> {
                    attr.setSchema(groupName.concat(SyncopeEnduserConstants.MEMBERSHIP_ATTR_SEPARATOR).
                            concat(attr.getSchema()));
                    return attr;
                }).forEachOrdered(attr -> {
                    userTO.getDerAttrs().add(attr);
                });
                membership.getDerAttrs().clear();
                membership.getVirAttrs().stream().map((attr) -> {
                    attr.setSchema(groupName.concat(SyncopeEnduserConstants.MEMBERSHIP_ATTR_SEPARATOR).
                            concat(attr.getSchema()));
                    return attr;
                }).forEachOrdered(attr -> {
                    userTO.getVirAttrs().add(attr);
                });
                membership.getVirAttrs().clear();
            }
            // USER from customization, if empty or null ignore it, use it to filter attributes otherwise
            applyFromCustomization(userTO, SyncopeEnduserApplication.get().getCustomForm());

            // 1.1 Date -> millis conversion for PLAIN attributes of USER
            for (PlainSchemaTO plainSchema : SyncopeEnduserSession.get().getDatePlainSchemas()) {
                dateToMillis(userTO.getPlainAttrs(), plainSchema);
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

    private void applyFromCustomization(final UserTO userTO, final Map<String, CustomAttributesInfo> customForm) {
        if (customForm != null && !customForm.isEmpty()) {
            // filter PLAIN attributes
            customizeAttrTOs(userTO.getPlainAttrs(), customForm.get(SchemaType.PLAIN.name()));
            // filter DERIVED attributes
            customizeAttrTOs(userTO.getDerAttrs(), customForm.get(SchemaType.DERIVED.name()));
            // filter VIRTUAL attributes
            customizeAttrTOs(userTO.getVirAttrs(), customForm.get(SchemaType.VIRTUAL.name()));
        }
    }

    private void customizeAttrTOs(final Set<AttrTO> attrs, final CustomAttributesInfo customAttributesInfo) {
        if (customAttributesInfo != null
                && customAttributesInfo.isShow()
                && !customAttributesInfo.getAttributes().isEmpty()) {

            attrs.removeAll(attrs.stream().
                    filter(attr -> !customAttributesInfo.getAttributes().containsKey(attr.getSchema())).
                    collect(Collectors.toList()));
        } else if (customAttributesInfo != null && !customAttributesInfo.isShow()) {
            attrs.clear();
        }
    }

}
