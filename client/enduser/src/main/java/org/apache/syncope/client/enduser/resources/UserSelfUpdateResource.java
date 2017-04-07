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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.syncope.client.enduser.SyncopeEnduserConstants;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.annotations.Resource;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;

@Resource(key = "userSelfUpdate", path = "/api/self/update")
public class UserSelfUpdateResource extends BaseResource {

    private static final long serialVersionUID = -2721621682300247583L;

    @Override
    protected ResourceResponse newResourceResponse(final IResource.Attributes attributes) {
        ResourceResponse response = new AbstractResource.ResourceResponse();
        response.setContentType(MediaType.TEXT_PLAIN);
        try {
            HttpServletRequest request = (HttpServletRequest) attributes.getRequest().getContainerRequest();
            if (!xsrfCheck(request)) {
                LOG.error("XSRF TOKEN does not match");
                response.setError(Response.Status.BAD_REQUEST.getStatusCode(), "XSRF TOKEN does not match");
                return response;
            }

            if (!captchaCheck(
                    request.getHeader("captcha"),
                    request.getSession().getAttribute(SyncopeEnduserConstants.CAPTCHA_SESSION_KEY))) {

                throw new IllegalArgumentException("Entered captcha is not matching");
            }

            UserTO userTO = MAPPER.readValue(request.getReader().readLine(), UserTO.class);

            // 1. membership attributes management
            Set<AttrTO> membAttrs = new HashSet<>();
            for (AttrTO attr : userTO.getPlainAttrs()) {
                if (attr.getSchema().contains("#")) {
                    final String[] compositeSchemaKey = attr.getSchema().split("#");
                    MembershipTO membership = IterableUtils.find(userTO.getMemberships(),
                            new Predicate<MembershipTO>() {

                        @Override
                        public boolean evaluate(final MembershipTO item) {
                            return compositeSchemaKey[0].equals(item.getGroupName());
                        }
                    });
                    if (membership == null) {
                        membership = new MembershipTO.Builder().group(null, compositeSchemaKey[0]).build();
                        userTO.getMemberships().add(membership);
                    }
                    AttrTO clone = SerializationUtils.clone(attr);
                    clone.setSchema(compositeSchemaKey[1]);
                    membership.getPlainAttrs().add(clone);
                    membAttrs.add(attr);
                }
            }
            userTO.getPlainAttrs().removeAll(membAttrs);

            // 2. millis -> Date conversion for PLAIN attributes of USER and its MEMBERSHIPS
            Map<String, AttrTO> userPlainAttrMap = userTO.getPlainAttrMap();
            for (PlainSchemaTO plainSchema : SyncopeEnduserSession.get().getDatePlainSchemas()) {
                millisToDate(userPlainAttrMap, plainSchema);
                for (MembershipTO membership : userTO.getMemberships()) {
                    millisToDate(membership.getPlainAttrMap(), plainSchema);
                }
            }

            membAttrs.clear();
            for (AttrTO attr : userTO.getDerAttrs()) {
                if (attr.getSchema().contains("#")) {
                    final String[] simpleAttrs = attr.getSchema().split("#");
                    MembershipTO membership = IterableUtils.find(userTO.getMemberships(),
                            new Predicate<MembershipTO>() {

                        @Override
                        public boolean evaluate(final MembershipTO item) {
                            return simpleAttrs[0].equals(item.getGroupName());
                        }
                    });
                    if (membership == null) {
                        membership = new MembershipTO.Builder().group(null, simpleAttrs[0]).build();
                        userTO.getMemberships().add(membership);
                    }
                    AttrTO clone = SerializationUtils.clone(attr);
                    clone.setSchema(simpleAttrs[1]);
                    membership.getDerAttrs().add(clone);
                    membAttrs.add(attr);
                }
            }
            userTO.getDerAttrs().removeAll(membAttrs);

            membAttrs.clear();
            for (AttrTO attr : userTO.getVirAttrs()) {
                if (attr.getSchema().contains("#")) {
                    final String[] simpleAttrs = attr.getSchema().split("#");
                    MembershipTO membership = IterableUtils.find(userTO.getMemberships(),
                            new Predicate<MembershipTO>() {

                        @Override
                        public boolean evaluate(final MembershipTO item) {
                            return simpleAttrs[0].equals(item.getGroupName());
                        }
                    });
                    if (membership == null) {
                        membership = new MembershipTO.Builder().group(null, simpleAttrs[0]).build();
                        userTO.getMemberships().add(membership);

                    }
                    AttrTO clone = SerializationUtils.clone(attr);
                    clone.setSchema(simpleAttrs[1]);
                    membership.getVirAttrs().add(clone);
                    membAttrs.add(attr);
                }
            }
            userTO.getVirAttrs().removeAll(membAttrs);

            // update user by patch
            Response res = SyncopeEnduserSession.get().
                    getService(userTO.getETagValue(), UserSelfService.class).update(AnyOperations.diff(userTO,
                    SyncopeEnduserSession.get().getSelfTO(), true));

            final String responseMessage = res.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)
                    ? new StringBuilder().
                            append("User").append(userTO.getUsername()).append(" successfully updated").toString()
                    : new StringBuilder().
                            append("ErrorMessage{{ ").append(res.getStatusInfo().getReasonPhrase()).append(" }}").
                            toString();

            response.setTextEncoding(StandardCharsets.UTF_8.name());
            response.setWriteCallback(new WriteCallback() {

                @Override
                public void writeData(final Attributes attributes) throws IOException {
                    attributes.getResponse().write(responseMessage);
                }
            });

            response.setStatusCode(res.getStatus());
        } catch (final Exception e) {
            LOG.error("Error while updating user", e);
            response.setError(Response.Status.BAD_REQUEST.getStatusCode(),
                    new StringBuilder().
                            append("ErrorMessage{{ ").
                            append(e.getMessage()).
                            append(" }}").
                            toString());
        }
        return response;
    }

    private void millisToDate(final Map<String, AttrTO> plainAttrMap, final PlainSchemaTO plainSchema)
            throws IllegalArgumentException {
        LOG.info("CONVERTING >>>>>>>>>> {}", plainSchema.getKey());
        if (plainAttrMap.containsKey(plainSchema.getKey())) {
            FastDateFormat fmt = FastDateFormat.getInstance(plainSchema.getConversionPattern());

            AttrTO dateAttr = plainAttrMap.get(plainSchema.getKey());
            List<String> formattedValues = new ArrayList<>(dateAttr.getValues().size());
            for (String value : dateAttr.getValues()) {
                try {
                    formattedValues.add(fmt.format(Long.valueOf(value)));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid format value for " + value);
                }
            }
            dateAttr.getValues().clear();
            dateAttr.getValues().addAll(formattedValues);
        }
    }
}
