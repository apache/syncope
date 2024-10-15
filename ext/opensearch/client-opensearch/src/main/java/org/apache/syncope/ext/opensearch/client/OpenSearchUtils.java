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

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.apache.syncope.core.persistence.api.entity.GroupablePlainAttr;
import org.apache.syncope.core.persistence.api.entity.GroupableRelatable;
import org.apache.syncope.core.persistence.api.entity.Membership;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.Privilege;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Relationship;
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

    protected static final char[] ELASTICSEARCH_REGEX_CHARS = new char[] {
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

    /**
     * Returns the document specialized with content from the provided any.
     *
     * @param any user, group or any object to index
     * @return document specialized with content from the provided any
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public Map<String, Object> document(final Any<?> any) {
        Collection<String> resources = any instanceof User
                ? userDAO.findAllResourceKeys(any.getKey())
                : any instanceof AnyObject
                        ? anyObjectDAO.findAllResourceKeys(any.getKey())
                        : groupDAO.findAllResourceKeys(any.getKey());
        Collection<String> dynRealms = any instanceof User
                ? userDAO.findDynRealms(any.getKey())
                : any instanceof AnyObject
                        ? anyObjectDAO.findDynRealms(any.getKey())
                        : groupDAO.findDynRealms(any.getKey());

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
        builder.put("auxClasses", any.getAuxClasses().stream().map(AnyTypeClass::getKey).collect(Collectors.toList()));
        builder.put("resources", resources);
        builder.put("dynRealms", dynRealms);

        if (any instanceof AnyObject) {
            AnyObject anyObject = ((AnyObject) any);
            builder.put("name", anyObject.getName());

            builder.put("memberships", anyObjectDAO.findAllGroupKeys(anyObject));

            List<String> relationships = new ArrayList<>();
            List<String> relationshipTypes = new ArrayList<>();
            anyObjectDAO.findAllRelationships(anyObject).forEach(relationship -> {
                relationships.add(relationship.getRightEnd().getKey());
                relationshipTypes.add(relationship.getType().getKey());
            });
            builder.put("relationships", relationships);
            builder.put("relationshipTypes", relationshipTypes);

            customizeDocument(builder, anyObject);
        } else if (any instanceof Group) {
            Group group = ((Group) any);
            builder.put("name", group.getName());
            Optional.ofNullable(group.getUserOwner()).ifPresent(uo -> builder.put("userOwner", uo.getKey()));
            Optional.ofNullable(group.getGroupOwner()).ifPresent(go -> builder.put("groupOwner", go.getKey()));

            Set<String> members = new HashSet<>();
            members.addAll(groupDAO.findUMemberships(group).stream().
                    map(membership -> membership.getLeftEnd().getKey()).collect(Collectors.toList()));
            members.addAll(groupDAO.findUDynMembers(group));
            members.addAll(groupDAO.findAMemberships(group).stream().
                    map(membership -> membership.getLeftEnd().getKey()).collect(Collectors.toList()));
            members.addAll(groupDAO.findADynMembers(group));
            builder.put("members", members);

            customizeDocument(builder, group);
        } else if (any instanceof User) {
            User user = ((User) any);
            builder.put("username", user.getUsername());
            builder.put("token", user.getToken());
            builder.put("tokenExpireTime", user.getTokenExpireTime());
            builder.put("changePwdDate", user.getChangePwdDate());
            builder.put("failedLogins", user.getFailedLogins());
            builder.put("lastLoginDate", user.getLastLoginDate());
            builder.put("suspended", user.isSuspended());
            builder.put("mustChangePassword", user.isMustChangePassword());

            List<String> roles = new ArrayList<>();
            Set<String> privileges = new HashSet<>();
            userDAO.findAllRoles(user).forEach(role -> {
                roles.add(role.getKey());
                privileges.addAll(role.getPrivileges().stream().map(Privilege::getKey).collect(Collectors.toSet()));
            });
            builder.put("roles", roles);
            builder.put("privileges", privileges);

            builder.put("memberships", userDAO.findAllGroupKeys(user));

            List<String> relationships = new ArrayList<>();
            Set<String> relationshipTypes = new HashSet<>();
            user.getRelationships().forEach(relationship -> {
                relationships.add(relationship.getRightEnd().getKey());
                relationshipTypes.add(relationship.getType().getKey());
            });
            builder.put("relationships", relationships);
            builder.put("relationshipTypes", relationshipTypes);

            customizeDocument(builder, user);
        }

        for (PlainAttr<?> plainAttr : any.getPlainAttrs()) {
            List<Object> values = plainAttr.getValues().stream().
                    map(PlainAttrValue::getValue).collect(Collectors.toList());

            Optional.ofNullable(plainAttr.getUniqueValue()).ifPresent(v -> values.add(v.getValue()));

            builder.put(plainAttr.getSchema().getKey(), values.size() == 1 ? values.get(0) : values);
        }

        // add also flattened membership attributes
        if (any instanceof GroupableRelatable) {
            GroupableRelatable<? extends Any, ? extends Membership, ? extends GroupablePlainAttr, ? extends Any, ?
                    extends Relationship> entity = GroupableRelatable.class.cast(any);
            entity.getMemberships().forEach(m -> entity.getPlainAttrs(m).forEach(mAttr -> {
                List<Object> values = mAttr.getValues().stream().map(PlainAttrValue::getValue)
                        .collect(Collectors.toList());

                Optional.ofNullable(mAttr.getUniqueValue()).ifPresent(v -> values.add(v.getValue()));

                Object attr = builder.computeIfAbsent(mAttr.getSchema().getKey(),
                        k -> Collections.synchronizedSet(new HashSet<>()));
                if (attr instanceof Collection) {
                    ((Collection<Object>) attr).addAll(values);
                } else {
                    Set<Object> newValues = Collections.synchronizedSet(new HashSet<>(values));
                    newValues.add(attr);
                    builder.put(mAttr.getSchema().getKey(), newValues);
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

    public Map<String, Object> document(
            final long instant,
            final JsonNode message,
            final String domain) throws IOException {

        Map<String, Object> builder = new HashMap<>();

        builder.put("instant", instant);
        builder.put("message", message);

        customizeDocument(builder, instant, message, domain);

        return builder;
    }

    protected void customizeDocument(
            final Map<String, Object> builder,
            final long instant,
            final JsonNode message,
            final String domain)
            throws IOException {
    }
}
