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
package org.apache.syncope.ext.elasticsearch.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.Privilege;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * Utility methods for usage with Elasticsearch.
 */
public class ElasticsearchUtils {

    public static String getContextDomainName(final String domain, final AnyTypeKind kind) {
        return domain.toLowerCase() + '_' + kind.name().toLowerCase();
    }

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected int indexMaxResultWindow = 10000;

    protected int retryOnConflict = 5;

    protected String numberOfShards = "1";

    protected String numberOfReplicas = "1";

    public ElasticsearchUtils(final UserDAO userDAO, final GroupDAO groupDAO, final AnyObjectDAO anyObjectDAO) {
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.anyObjectDAO = anyObjectDAO;
    }

    public void setIndexMaxResultWindow(final int indexMaxResultWindow) {
        this.indexMaxResultWindow = indexMaxResultWindow;
    }

    public int getIndexMaxResultWindow() {
        return indexMaxResultWindow;
    }

    public void setRetryOnConflict(final int retryOnConflict) {
        this.retryOnConflict = retryOnConflict;
    }

    public int getRetryOnConflict() {
        return retryOnConflict;
    }

    public String getNumberOfShards() {
        return numberOfShards;
    }

    public void setNumberOfShards(final int numberOfShards) {
        this.numberOfShards = String.valueOf(numberOfShards);
    }

    public String getNumberOfReplicas() {
        return numberOfReplicas;
    }

    public void setNumberOfReplicas(final int numberOfReplicas) {
        this.numberOfReplicas = String.valueOf(numberOfReplicas);
    }

    /**
     * Returns the document specialized with content from the provided any.
     *
     * @param any user, group or any object to index
     * @param domain tenant information
     * @return document specialized with content from the provided any
     * @throws IOException in case of errors
     */
    @Transactional
    public Map<String, Object> document(final Any<?> any, final String domain) throws IOException {
        Set<String> resources = new HashSet<>();
        List<String> dynRealms = new ArrayList<>();
        AuthContextUtils.callAsAdmin(domain, () -> {
            resources.addAll(any instanceof User
                    ? userDAO.findAllResourceKeys(any.getKey())
                    : any instanceof AnyObject
                            ? anyObjectDAO.findAllResourceKeys(any.getKey())
                            : groupDAO.findAllResourceKeys(any.getKey()));
            dynRealms.addAll(any instanceof User
                    ? userDAO.findDynRealms(any.getKey())
                    : any instanceof AnyObject
                            ? anyObjectDAO.findDynRealms(any.getKey())
                            : groupDAO.findDynRealms(any.getKey()));
            return null;
        });

        Map<String, Object> builder = new HashMap<>();
        builder.put("id", any.getKey());
        builder.put("realm", any.getRealm().getFullPath());
        builder.put("anyType", any.getType().getKey());
        builder.put("creationDate", any.getCreationDate());
        builder.put("creationContext", any.getCreationContext());
        builder.put("creator", any.getCreator());
        builder.put("lastChangeDate", any.getLastChangeDate());
        builder.put("lastModifier", any.getLastModifier());
        builder.put("lastChangeContext", any.getLastChangeContext());
        builder.put("status", any.getStatus());
        builder.put("resources", resources);
        builder.put("dynRealms", dynRealms);

        if (any instanceof AnyObject) {
            AnyObject anyObject = ((AnyObject) any);
            builder.put("name", anyObject.getName());

            Collection<String> memberships = AuthContextUtils.callAsAdmin(
                    domain, () -> anyObjectDAO.findAllGroupKeys(anyObject));
            builder.put("memberships", memberships);

            List<String> relationships = new ArrayList<>();
            List<String> relationshipTypes = new ArrayList<>();
            AuthContextUtils.callAsAdmin(domain, () -> {
                anyObjectDAO.findAllRelationships(anyObject).forEach(relationship -> {
                    relationships.add(relationship.getRightEnd().getKey());
                    relationshipTypes.add(relationship.getType().getKey());
                });
                return null;
            });
            builder.put("relationships", relationships);
            builder.put("relationshipTypes", relationshipTypes);

            ElasticsearchUtils.this.customizeDocument(builder, anyObject, domain);
        } else if (any instanceof Group) {
            Group group = ((Group) any);
            builder.put("name", group.getName());
            if (group.getUserOwner() != null) {
                builder.put("userOwner", group.getUserOwner().getKey());
            }
            if (group.getGroupOwner() != null) {
                builder.put("groupOwner", group.getGroupOwner().getKey());
            }

            Set<String> members = new HashSet<>();
            AuthContextUtils.callAsAdmin(domain, () -> {
                members.addAll(groupDAO.findUMemberships(group).stream().
                        map(membership -> membership.getLeftEnd().getKey()).collect(Collectors.toList()));
                members.addAll(groupDAO.findUDynMembers(group));
                members.addAll(groupDAO.findAMemberships(group).stream().
                        map(membership -> membership.getLeftEnd().getKey()).collect(Collectors.toList()));
                members.addAll(groupDAO.findADynMembers(group));
                return null;
            });
            builder.put("members", members);

            ElasticsearchUtils.this.customizeDocument(builder, group, domain);
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
            AuthContextUtils.callAsAdmin(domain, () -> {
                userDAO.findAllRoles(user).forEach(role -> {
                    roles.add(role.getKey());
                    privileges.addAll(role.getPrivileges().stream().map(Privilege::getKey).collect(Collectors.toSet()));
                });
                return null;
            });
            builder.put("roles", roles);
            builder.put("privileges", privileges);

            Collection<String> memberships = AuthContextUtils.callAsAdmin(
                    domain, () -> userDAO.findAllGroupKeys(user));
            builder.put("memberships", memberships);

            List<String> relationships = new ArrayList<>();
            Set<String> relationshipTypes = new HashSet<>();
            user.getRelationships().forEach(relationship -> {
                relationships.add(relationship.getRightEnd().getKey());
                relationshipTypes.add(relationship.getType().getKey());
            });
            builder.put("relationships", relationships);
            builder.put("relationshipTypes", relationshipTypes);

            customizeDocument(builder, user, domain);
        }

        for (PlainAttr<?> plainAttr : any.getPlainAttrs()) {
            List<Object> values = plainAttr.getValues().stream().
                    map(PlainAttrValue::getValue).collect(Collectors.toList());

            if (plainAttr.getUniqueValue() != null) {
                values.add(plainAttr.getUniqueValue().getValue());
            }

            builder.put(plainAttr.getSchema().getKey(), values.size() == 1 ? values.get(0) : values);
        }

        return builder;
    }

    protected void customizeDocument(
            final Map<String, Object> builder, final AnyObject anyObject, final String domain)
            throws IOException {
    }

    protected void customizeDocument(
            final Map<String, Object> builder, final Group group, final String domain)
            throws IOException {
    }

    protected void customizeDocument(final Map<String, Object> builder, final User user, final String domain)
            throws IOException {
    }
}
