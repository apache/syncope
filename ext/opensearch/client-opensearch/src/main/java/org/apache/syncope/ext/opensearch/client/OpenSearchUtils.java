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
package org.apache.syncope.ext.opensearch.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AuditEvent;
import org.apache.syncope.core.persistence.api.entity.Groupable;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Relationship;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.springframework.transaction.annotation.Transactional;

/**
 * Utility methods for usage with OpenSearch.
 */
public class OpenSearchUtils {

    public static String getAnyIndex(final String domain, final AnyTypeKind kind) {
        return domain.toLowerCase() + '_' + kind.name().toLowerCase();
    }

    public static String getRealmIndex(final String domain) {
        return domain.toLowerCase() + "_realm";
    }

    public static String getAuditIndex(final String domain) {
        return domain.toLowerCase() + "_audit";
    }

    protected static final char[] ELASTICSEARCH_REGEX_CHARS = {
        '.', '?', '+', '*', '|', '{', '}', '[', ']', '(', ')', '"', '\\', '&' };

    public static String escapeForLikeRegex(final char c) {
        StringBuilder output = new StringBuilder();

        if (ArrayUtils.contains(ELASTICSEARCH_REGEX_CHARS, c)) {
            output.append('\\');
        }

        output.append(c);

        return output.toString();
    }

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final AnyObjectDAO anyObjectDAO;

    public OpenSearchUtils(
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO) {

        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.anyObjectDAO = anyObjectDAO;
    }

    protected void relationships(final List<? extends Relationship<?, ?>> input, final Map<String, Object> builder) {
        List<String> relationships = new ArrayList<>();
        List<String> relationshipTypes = new ArrayList<>();
        input.forEach(relationship -> {
            relationships.add(relationship.getRightEnd().getKey());
            relationshipTypes.add(relationship.getType().getKey());
        });
        builder.put("relationships", relationships);
        builder.put("relationshipTypes", relationshipTypes);
    }

    /**
     * Returns the document specialized with content from the provided any.
     *
     * @param any user, group or any object to index
     * @return document specialized with content from the provided any
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public Map<String, Object> document(final Any any) {
        Map<String, Object> builder = new HashMap<>();
        builder.put("id", any.getKey());
        builder.put("realm", any.getRealm().getKey());
        builder.put("anyType", any.getType().getKey());
        builder.put("creationDate", any.getCreationDate());
        builder.put("creationContext", any.getCreationContext());
        builder.put("creator", any.getCreator());
        builder.put("lastChangeDate", any.getLastChangeDate());
        builder.put("lastModifier", any.getLastModifier());
        builder.put("lastChangeContext", any.getLastChangeContext());
        builder.put("status", any.getStatus());
        builder.put("auxClasses", any.getAuxClasses().stream().map(AnyTypeClass::getKey).toList());

        switch (any) {
            case AnyObject anyObject -> {
                builder.put("name", anyObject.getName());

                builder.put("memberships", anyObjectDAO.findAllGroupKeys(anyObject));

                relationships(anyObjectDAO.findAllRelationships(anyObject), builder);

                builder.put("resources", anyObjectDAO.findAllResourceKeys(anyObject.getKey()));
                builder.put("dynRealms", anyObjectDAO.findDynRealms(anyObject.getKey()));

                customizeDocument(builder, anyObject);
            }
            case Group group -> {
                builder.put("name", group.getName());
                Optional.ofNullable(group.getUserOwner()).ifPresent(uo -> builder.put("userOwner", uo.getKey()));
                Optional.ofNullable(group.getGroupOwner()).ifPresent(go -> builder.put("groupOwner", go.getKey()));

                Set<String> members = new HashSet<>();
                members.addAll(groupDAO.findUMemberships(group).stream().
                        map(membership -> membership.getLeftEnd().getKey()).toList());
                members.addAll(groupDAO.findUDynMembers(group));
                members.addAll(groupDAO.findAMemberships(group).stream().
                        map(membership -> membership.getLeftEnd().getKey()).toList());
                members.addAll(groupDAO.findADynMembers(group));
                builder.put("members", members);

                relationships(group.getRelationships(), builder);

                builder.put("resources", groupDAO.findAllResourceKeys(group.getKey()));
                builder.put("dynRealms", groupDAO.findDynRealms(group.getKey()));

                customizeDocument(builder, group);
            }
            case User user -> {
                builder.put("username", user.getUsername());
                builder.put("token", user.getToken());
                builder.put("tokenExpireTime", user.getTokenExpireTime());
                builder.put("changePwdDate", user.getChangePwdDate());
                builder.put("failedLogins", user.getFailedLogins());
                builder.put("lastLoginDate", user.getLastLoginDate());
                builder.put("suspended", user.isSuspended());
                builder.put("mustChangePassword", user.isMustChangePassword());

                builder.put("roles", userDAO.findAllRoles(user).stream().map(Role::getKey).toList());

                builder.put("memberships", userDAO.findAllGroupKeys(user));

                relationships(user.getRelationships(), builder);

                builder.put("resources", userDAO.findAllResourceKeys(user.getKey()));
                builder.put("dynRealms", userDAO.findDynRealms(user.getKey()));

                customizeDocument(builder, user);
            }
            default -> {
            }
        }

        for (PlainAttr plainAttr : any.getPlainAttrs()) {
            List<Object> values = plainAttr.getValues().stream().
                    map(PlainAttrValue::getValue).collect(Collectors.toList());

            Optional.ofNullable(plainAttr.getUniqueValue()).ifPresent(v -> values.add(v.getValue()));

            builder.put(plainAttr.getSchema(), values.size() == 1 ? values.getFirst() : values);
        }

        // add also flattened membership attributes
        if (any instanceof Groupable<?, ?, ?, ?> groupable) {
            groupable.getMemberships().forEach(m -> groupable.getPlainAttrs(m).forEach(mAttr -> {
                List<Object> values = mAttr.getValues().stream().
                        map(PlainAttrValue::getValue).collect(Collectors.toList());

                Optional.ofNullable(mAttr.getUniqueValue()).ifPresent(v -> values.add(v.getValue()));

                Object attr = builder.computeIfAbsent(mAttr.getSchema(), k -> new HashSet<>());
                // also support case in which there is also an existing attribute set previously
                if (attr instanceof Collection) {
                    ((Collection<Object>) attr).addAll(values);
                } else {
                    values.add(attr);
                    builder.put(mAttr.getSchema(), values.size() == 1 ? values.getFirst() : values);
                }
            }));
        }

        return builder;
    }

    protected void customizeDocument(final Map<String, Object> builder, final AnyObject anyObject) {
    }

    protected void customizeDocument(final Map<String, Object> builder, final Group group) {
    }

    protected void customizeDocument(final Map<String, Object> builder, final User user) {
    }

    public Map<String, Object> document(final Realm realm) {
        Map<String, Object> builder = new HashMap<>();
        builder.put("id", realm.getKey());
        builder.put("name", realm.getName());
        builder.put("parent_id", realm.getParent() == null ? null : realm.getParent().getKey());
        builder.put("fullPath", realm.getFullPath());

        customizeDocument(builder, realm);

        return builder;
    }

    protected void customizeDocument(final Map<String, Object> builder, final Realm realm) {
    }

    public Map<String, Object> document(final AuditEvent auditEvent) {
        Map<String, Object> builder = new HashMap<>();
        builder.put("key", auditEvent.getKey());
        builder.put("opEvent", auditEvent.getOpEvent());
        builder.put("who", auditEvent.getWho());
        builder.put("when", auditEvent.getWhen());
        builder.put("before", auditEvent.getBefore());
        builder.put("inputs", auditEvent.getInputs());
        builder.put("output", auditEvent.getOutput());
        builder.put("throwable", auditEvent.getThrowable());

        customizeDocument(builder, auditEvent);

        return builder;
    }

    protected void customizeDocument(
            final Map<String, Object> builder,
            final AuditEvent auditEvent) {
    }
}
