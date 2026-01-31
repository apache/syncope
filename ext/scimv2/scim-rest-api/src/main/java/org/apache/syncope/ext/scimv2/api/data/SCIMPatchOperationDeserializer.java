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

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.cxf.common.util.StringUtils;
import org.apache.syncope.ext.scimv2.api.type.PatchOp;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

public class SCIMPatchOperationDeserializer extends StdDeserializer<SCIMPatchOperation> {

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

        return v.asString();
    }

    public SCIMPatchOperationDeserializer() {
        this(SCIMPatchOperation.class);
    }

    public SCIMPatchOperationDeserializer(final Class<?> vc) {
        super(vc);
    }

    @Override
    public SCIMPatchOperation deserialize(final JsonParser jp, final DeserializationContext ctxt)
            throws JacksonException {

        JsonNode node = jp.readValueAsTree();

        SCIMPatchOperation scimPatchOperation = new SCIMPatchOperation();

        if (node.has("op")) {
            scimPatchOperation.setOp(PatchOp.valueOf(node.get("op").asString().toLowerCase()));
        }

        if (node.has("path")) {
            Matcher matcher = PATH_PATTERN.matcher(node.get("path").asString());
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
                scimPatchOperation.getValue().add(
                        jp.objectReadContext().treeAsTokens(value).readValueAs(SCIMUser.class));
            } else {
                if ("members".equals(scimPatchOperation.getPath().getAttribute())) {
                    scimPatchOperation.getValue().addAll(
                            jp.objectReadContext().treeAsTokens(value).readValueAs(new TypeReference<List<Member>>() {
                            }));
                } else if (value.isObject()) {
                    SCIMUser user = new SCIMUser(
                            null,
                            List.of(),
                            null,
                            "userName".equals(scimPatchOperation.getPath().getAttribute()) ? value.asString() : null,
                            "active".equals(scimPatchOperation.getPath().getAttribute()) ? value.asBoolean() : null);
                    user.setEnterpriseInfo(new SCIMEnterpriseInfo());

                    switch (scimPatchOperation.getPath().getAttribute()) {
                        case "externalId" ->
                            user.setExternalId(value.asString());

                        case "name" ->
                            user.setName(jp.objectReadContext().treeAsTokens(value).readValueAs(SCIMUserName.class));

                        case "displayName" ->
                            user.setDisplayName(value.asString());

                        case "nickName" ->
                            user.setNickName(value.asString());

                        case "profileUrl" ->
                            user.setProfileUrl(value.asString());

                        case "title" ->
                            user.setTitle(value.asString());

                        case "userType" ->
                            user.setUserType(value.asString());

                        case "preferredLanguage" ->
                            user.setPreferredLanguage(value.asString());

                        case "locale" ->
                            user.setLocale(value.asString());

                        case "timezone" ->
                            user.setTimezone(value.asString());

                        case "emails" ->
                            user.getEmails().add(
                                    jp.objectReadContext().treeAsTokens(value).readValueAs(SCIMComplexValue.class));

                        case "phoneNumbers" ->
                            user.getPhoneNumbers().add(
                                    jp.objectReadContext().treeAsTokens(value).readValueAs(SCIMComplexValue.class));

                        case "ims" ->
                            user.getIms().add(
                                    jp.objectReadContext().treeAsTokens(value).readValueAs(SCIMComplexValue.class));

                        case "photos" ->
                            user.getPhotos().add(
                                    jp.objectReadContext().treeAsTokens(value).readValueAs(SCIMComplexValue.class));

                        case "addresses" ->
                            user.getAddresses().add(
                                    jp.objectReadContext().treeAsTokens(value).readValueAs(SCIMUserAddress.class));

                        case "x509Certificates" ->
                            user.getX509Certificates().add(
                                    jp.objectReadContext().treeAsTokens(value).readValueAs(Value.class));

                        case "employeeNumber" ->
                            user.getEnterpriseInfo().setEmployeeNumber(value.asString());

                        case "costCenter" ->
                            user.getEnterpriseInfo().setCostCenter(value.asString());

                        case "organization" ->
                            user.getEnterpriseInfo().setOrganization(value.asString());

                        case "division" ->
                            user.getEnterpriseInfo().setDivision(value.asString());

                        case "department" ->
                            user.getEnterpriseInfo().setDepartment(value.asString());

                        case "manager" ->
                            user.getEnterpriseInfo().setManager(
                                    jp.objectReadContext().treeAsTokens(value).readValueAs(SCIMUserManager.class));

                        default -> {
                        }
                    }

                    scimPatchOperation.getValue().add(user);
                } else if (!value.isContainer()) {
                    scimPatchOperation.getValue().add(scalar(value));
                }
            }
        }

        return scimPatchOperation;
    }
}
