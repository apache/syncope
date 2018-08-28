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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserApplication;
import org.apache.syncope.client.enduser.SyncopeEnduserConstants;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.annotations.Resource;
import org.apache.syncope.client.enduser.model.CustomAttributesInfo;
import org.apache.syncope.client.enduser.util.UserRequestValidator;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.IResource;

@Resource(key = "userSelfUpdate", path = "/api/self/update")
public class UserSelfUpdateResource extends BaseUserSelfResource {

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
            Map<String, CustomAttributesInfo> customFormAttributes =
                    SyncopeEnduserApplication.get().getCustomFormAttributes();

            // check if request is compliant with customization form rules
            if (UserRequestValidator.compliant(userTO, customFormAttributes, false)) {
                // 1. membership attributes management
                Set<AttrTO> membAttrs = new HashSet<>();
                userTO.getPlainAttrs().stream().
                        filter(attr -> (attr.getSchema().contains(SyncopeEnduserConstants.MEMBERSHIP_ATTR_SEPARATOR))).
                        forEachOrdered((attr) -> {
                            String[] simpleAttrs = attr.getSchema().split(
                                    SyncopeEnduserConstants.MEMBERSHIP_ATTR_SEPARATOR);
                            MembershipTO membership = userTO.getMemberships().stream().
                                    filter(item -> simpleAttrs[0].equals(item.getGroupName())).
                                    findFirst().orElse(null);
                            if (membership == null) {
                                membership = new MembershipTO.Builder().group(null, simpleAttrs[0]).build();
                                userTO.getMemberships().add(membership);
                            }
                            AttrTO clone = SerializationUtils.clone(attr);
                            clone.setSchema(simpleAttrs[1]);
                            membership.getPlainAttrs().add(clone);
                            membAttrs.add(attr);
                        });
                userTO.getPlainAttrs().removeAll(membAttrs);

                // 2. millis -> Date conversion for PLAIN attributes of USER and its MEMBERSHIPS
                SyncopeEnduserSession.get().getDatePlainSchemas().stream().
                        map(plainSchema -> {
                            millisToDate(userTO.getPlainAttrs(), plainSchema);
                            return plainSchema;
                        }).forEachOrdered(plainSchema -> {
                    userTO.getMemberships().forEach(membership -> {
                        millisToDate(membership.getPlainAttrs(), plainSchema);
                    });
                });

                membAttrs.clear();
                userTO.getDerAttrs().stream().
                        filter(attr -> (attr.getSchema().contains(SyncopeEnduserConstants.MEMBERSHIP_ATTR_SEPARATOR))).
                        forEachOrdered(attr -> {
                            String[] simpleAttrs = attr.getSchema().split(
                                    SyncopeEnduserConstants.MEMBERSHIP_ATTR_SEPARATOR);
                            MembershipTO membership = userTO.getMemberships().stream().
                                    filter(item -> simpleAttrs[0].equals(item.getGroupName())).
                                    findFirst().orElse(null);
                            if (membership == null) {
                                membership = new MembershipTO.Builder().group(null, simpleAttrs[0]).build();
                                userTO.getMemberships().add(membership);
                            }
                            AttrTO clone = SerializationUtils.clone(attr);
                            clone.setSchema(simpleAttrs[1]);
                            membership.getDerAttrs().add(clone);
                            membAttrs.add(attr);
                        });
                userTO.getDerAttrs().removeAll(membAttrs);

                membAttrs.clear();
                userTO.getVirAttrs().stream().
                        filter(attr -> (attr.getSchema().contains(SyncopeEnduserConstants.MEMBERSHIP_ATTR_SEPARATOR))).
                        forEachOrdered((attr) -> {
                            String[] simpleAttrs = attr.getSchema().split(
                                    SyncopeEnduserConstants.MEMBERSHIP_ATTR_SEPARATOR);
                            MembershipTO membership = userTO.getMemberships().stream().
                                    filter(item -> simpleAttrs[0].equals(item.getGroupName())).
                                    findFirst().orElse(null);
                            if (membership == null) {
                                membership = new MembershipTO.Builder().group(null, simpleAttrs[0]).build();
                                userTO.getMemberships().add(membership);

                            }
                            AttrTO clone = SerializationUtils.clone(attr);
                            clone.setSchema(simpleAttrs[1]);
                            membership.getVirAttrs().add(clone);
                            membAttrs.add(attr);
                        });
                userTO.getVirAttrs().removeAll(membAttrs);

                // get old user object from session
                UserTO selfTO = SyncopeEnduserSession.get().getSelfTO();
                // align "userTO" and "selfTO" objects
                if (customFormAttributes != null && !customFormAttributes.isEmpty()) {
                    completeUserObject(userTO, selfTO);
                }
                // create diff patch
                UserPatch userPatch = AnyOperations.diff(userTO, selfTO, false);
                if (userPatch.isEmpty()) {
                    // nothing to do
                    buildResponse(response,
                            Response.Status.OK.getStatusCode(),
                            "No need to update [" + selfTO.getUsername() + "]");
                } else {
                    // update user by patch
                    Response coreResponse = SyncopeEnduserSession.get().
                            getService(userTO.getETagValue(), UserSelfService.class).update(userPatch);

                    buildResponse(response,
                            coreResponse.getStatus(),
                            coreResponse.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL
                            ? "User [" + selfTO.getUsername() + "] successfully updated"
                            : "ErrorMessage{{ " + coreResponse.getStatusInfo().getReasonPhrase() + " }}");
                }
            } else {
                LOG.warn(
                        "Incoming update request [{}] is not compliant with form customization rules."
                        + " Update NOT allowed", userTO.getUsername());
                buildResponse(response, Response.Status.OK.getStatusCode(),
                        "User: " + userTO.getUsername() + " successfully created");
            }
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

    private void completeUserObject(final UserTO userTO, final UserTO selfTO) {
        // memberships plain and virtual attrs
        userTO.getMemberships().forEach(updatedTOMemb -> {
            selfTO.getMemberships().stream().
                    filter(oldTOMemb -> updatedTOMemb.getGroupKey().equals(oldTOMemb.getGroupKey())).
                    findFirst().ifPresent(oldTOMatchedMemb -> {
                        if (!updatedTOMemb.getPlainAttrs().isEmpty()) {
                            completeAttrs(updatedTOMemb.getPlainAttrs(), oldTOMatchedMemb.getPlainAttrs());
                        }
                        if (!updatedTOMemb.getVirAttrs().isEmpty()) {
                            completeAttrs(updatedTOMemb.getVirAttrs(), oldTOMatchedMemb.getVirAttrs());
                        }
                    });
        });
        // plain attrs
        completeAttrs(userTO.getPlainAttrs(), selfTO.getPlainAttrs());
        // virtual attrs
        completeAttrs(userTO.getVirAttrs(), selfTO.getVirAttrs());
    }

    private void completeAttrs(final Set<AttrTO> userTOAttrs, final Set<AttrTO> selfTOAttrs) {
        Map<String, AttrTO> userTOAttrsMap =
                EntityTOUtils.buildAttrMap(userTOAttrs);
        selfTOAttrs.stream().
                filter(selfTOAttr -> (!userTOAttrsMap.containsKey(selfTOAttr.getSchema()))).
                forEachOrdered(selfTOAttr -> {
                    userTOAttrs.add(selfTOAttr);
                });
    }

}
