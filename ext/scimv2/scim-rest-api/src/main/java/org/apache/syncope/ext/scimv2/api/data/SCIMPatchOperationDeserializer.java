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
package org.apache.syncope.ext.scimv2.api.data;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.cxf.common.util.StringUtils;
import org.apache.syncope.ext.scimv2.api.type.PatchOp;

public class SCIMPatchOperationDeserializer extends StdDeserializer<SCIMPatchOperation> {

    private static final long serialVersionUID = -7401353969242788372L;

    private static final Pattern PATH_PATTERN = Pattern.compile(
            "^(?<schema>[A-Za-z0-9:.]+:)?(?<attribute>\\w+)(?<filter>\\[.*\\])?(?<sub>\\.\\w+)?");

    private static Serializable scalar(final JsonNode v) {
        if (v.isNull()) {
            return null;
        }
        if (v.isBoolean()) {
            return v.booleanValue();
        }
        if (v.isFloat()) {
            return v.floatValue();
        }
        if (v.isDouble()) {
            return v.doubleValue();
        }
        if (v.isInt()) {
            return v.intValue();
        }
        if (v.isShort()) {
            return v.shortValue();
        }
        if (v.isLong()) {
            return v.longValue();
        }

        return v.asText();
    }

    public SCIMPatchOperationDeserializer() {
        this(null);
    }

    public SCIMPatchOperationDeserializer(final Class<?> vc) {
        super(vc);
    }

    @Override
    public SCIMPatchOperation deserialize(final JsonParser jp, final DeserializationContext ctxt)
            throws IOException {

        JsonNode node = jp.getCodec().readTree(jp);

        SCIMPatchOperation scimPatchOperation = new SCIMPatchOperation();

        if (node.has("op")) {
            scimPatchOperation.setOp(PatchOp.valueOf(node.get("op").asText().toLowerCase()));
        }

        if (node.has("path")) {
            Matcher matcher = PATH_PATTERN.matcher(node.get("path").asText());
            if (matcher.matches()) {
                SCIMPatchPath path = new SCIMPatchPath();
                scimPatchOperation.setPath(path);

                Optional.ofNullable(matcher.group("schema")).
                        ifPresent(schema -> path.setSchema(schema.substring(0, schema.length() - 1)));

                path.setAttribute(StringUtils.uncapitalize(matcher.group("attribute")));

                Optional.ofNullable(matcher.group("filter")).
                        ifPresent(condition -> path.setFilter(condition.substring(1, condition.length() - 1)));

                Optional.ofNullable(matcher.group("sub")).
                        ifPresent(sub -> path.setSub(StringUtils.uncapitalize(sub.substring(1))));
            }
        }

        if (node.has("value")) {
            JsonNode value = node.get("value");

            if (scimPatchOperation.getPath() == null) {
                scimPatchOperation.setValue(List.of(jp.getCodec().treeToValue(value, SCIMUser.class)));
            } else {
                if ("members".equals(scimPatchOperation.getPath().getAttribute())) {
                    scimPatchOperation.setValue(List.of(
                            (Serializable[]) jp.getCodec().treeToValue(value, Member[].class)));
                } else if (value.isObject()) {
                    SCIMUser user = new SCIMUser(
                            null,
                            List.of(),
                            null,
                            "userName".equals(scimPatchOperation.getPath().getAttribute()) ? value.asText() : null,
                            "active".equals(scimPatchOperation.getPath().getAttribute()) ? value.asBoolean() : null);
                    user.setEnterpriseInfo(new SCIMEnterpriseInfo());

                    switch (scimPatchOperation.getPath().getAttribute()) {
                        case "externalId":
                            user.setExternalId(value.asText());
                            break;

                        case "name":
                            user.setName(jp.getCodec().treeToValue(value, SCIMUserName.class));
                            break;

                        case "displayName":
                            user.setDisplayName(value.asText());
                            break;

                        case "nickName":
                            user.setNickName(value.asText());
                            break;

                        case "profileUrl":
                            user.setProfileUrl(value.asText());
                            break;

                        case "title":
                            user.setTitle(value.asText());
                            break;

                        case "userType":
                            user.setUserType(value.asText());
                            break;

                        case "preferredLanguage":
                            user.setPreferredLanguage(value.asText());
                            break;

                        case "locale":
                            user.setLocale(value.asText());
                            break;

                        case "timezone":
                            user.setTimezone(value.asText());
                            break;

                        case "emails":
                            user.getEmails().add(jp.getCodec().treeToValue(value, SCIMComplexValue.class));
                            break;

                        case "phoneNumbers":
                            user.getPhoneNumbers().add(jp.getCodec().treeToValue(value, SCIMComplexValue.class));
                            break;

                        case "ims":
                            user.getIms().add(jp.getCodec().treeToValue(value, SCIMComplexValue.class));
                            break;

                        case "photos":
                            user.getPhotos().add(jp.getCodec().treeToValue(value, SCIMComplexValue.class));
                            break;

                        case "addresses":
                            user.getAddresses().add(jp.getCodec().treeToValue(value, SCIMUserAddress.class));
                            break;

                        case "x509Certificates":
                            user.getX509Certificates().add(jp.getCodec().treeToValue(value, Value.class));
                            break;

                        case "employeeNumber":
                            user.getEnterpriseInfo().setEmployeeNumber(value.asText());
                            break;

                        case "costCenter":
                            user.getEnterpriseInfo().setCostCenter(value.asText());
                            break;

                        case "organization":
                            user.getEnterpriseInfo().setOrganization(value.asText());
                            break;

                        case "division":
                            user.getEnterpriseInfo().setDivision(value.asText());
                            break;

                        case "department":
                            user.getEnterpriseInfo().setDepartment(value.asText());
                            break;

                        case "manager":
                            user.getEnterpriseInfo().
                                    setManager(jp.getCodec().treeToValue(value, SCIMUserManager.class));
                            break;

                        default:
                    }

                    scimPatchOperation.setValue(List.of(user));
                } else if (!value.isContainerNode()) {
                    scimPatchOperation.setValue(List.of(scalar(value)));
                }
            }
        }

        return scimPatchOperation;
    }
}
