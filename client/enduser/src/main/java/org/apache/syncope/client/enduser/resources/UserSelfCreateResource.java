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

import static org.apache.syncope.client.enduser.resources.BaseResource.LOG;

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
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.UserSelfService;

@Resource(key = "userSelfCreate", path = "/api/self/create")
public class UserSelfCreateResource extends BaseResource {

    private static final long serialVersionUID = -2721621682300247583L;

    private boolean isSelfRegistrationAllowed() {
        Boolean result = null;
        try {
            result = SyncopeEnduserSession.get().getPlatformInfo().isSelfRegAllowed();
        } catch (SyncopeClientException e) {
            LOG.error("While seeking if self registration is allowed", e);
        }

        return result == null
                ? false
                : result;
    }

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes) {
        ResourceResponse response = new ResourceResponse();
        response.setContentType(MediaType.TEXT_PLAIN);
        try {
            HttpServletRequest request = (HttpServletRequest) attributes.getRequest().getContainerRequest();

            if (!xsrfCheck(request)) {
                LOG.error("XSRF TOKEN is not matching");
                response.setError(Response.Status.BAD_REQUEST.getStatusCode(), "XSRF TOKEN is not matching");
                return response;
            }

            String jsonString = request.getReader().readLine();
            final UserTO userTO = MAPPER.readValue(jsonString, UserTO.class);

            if (!captchaCheck(
                    request.getHeader("captcha"),
                    request.getSession().getAttribute(SyncopeEnduserConstants.CAPTCHA_SESSION_KEY))) {

                throw new IllegalArgumentException("Entered captcha is not matching");
            }

            if (isSelfRegistrationAllowed() && userTO != null) {
                Map<String, AttrTO> userPlainAttrMap = userTO.getPlainAttrMap();

                // millis -> Date conversion
                for (PlainSchemaTO plainSchema : SyncopeEnduserSession.get().getDatePlainSchemas()) {
                    if (userPlainAttrMap.containsKey(plainSchema.getKey())) {
                        FastDateFormat fmt = FastDateFormat.getInstance(plainSchema.getConversionPattern());

                        AttrTO dateAttr = userPlainAttrMap.get(plainSchema.getKey());
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

                // membership attributes management
                Set<AttrTO> membAttrs = new HashSet<>();
                for (AttrTO attr : userTO.getPlainAttrs()) {
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
                        membership.getPlainAttrs().add(clone);
                        membAttrs.add(attr);
                    }
                }
                userTO.getPlainAttrs().removeAll(membAttrs);

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

                LOG.debug("Received user self registration request for user: [{}]", userTO.getUsername());
                LOG.trace("Received user self registration request is: [{}]", userTO);

                // adapt request and create user
                final Response res = SyncopeEnduserSession.get().getService(UserSelfService.class).create(userTO, true);

                response.setTextEncoding(StandardCharsets.UTF_8.name());

                response.setWriteCallback(new WriteCallback() {

                    @Override
                    public void writeData(final Attributes attributes) throws IOException {
                        attributes.getResponse().write(res.getStatusInfo().getFamily().equals(
                                Response.Status.Family.SUCCESSFUL)
                                        ? new StringBuilder().append("User: ").append(userTO.getUsername()).
                                                append(" successfully created")
                                        : new StringBuilder().append("ErrorMessage{{ ").
                                                append(res.getStatusInfo().getReasonPhrase()).append(" }}"));
                    }
                });
                response.setStatusCode(res.getStatus());
            } else {
                response.setError(Response.Status.FORBIDDEN.getStatusCode(), new StringBuilder().
                        append("ErrorMessage{{").append(userTO == null
                        ? "Request received is not valid }}"
                        : "Self registration not allowed }}").toString());
            }

        } catch (Exception e) {
            LOG.error("Could not create userTO", e);
            response.setError(Response.Status.BAD_REQUEST.getStatusCode(),
                    new StringBuilder().
                            append("ErrorMessage{{ ").
                            append(e.getMessage()).
                            append(" }}").
                            toString());
        }
        return response;
    }
}
