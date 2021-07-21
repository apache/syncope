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
import java.util.HashSet;
import java.util.List;
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
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Utility methods for usage with Elasticsearch.
 */
public class ElasticsearchUtils {

    public static String getContextDomainName(final String domain, final AnyTypeKind kind) {
        return domain.toLowerCase() + '_' + kind.name().toLowerCase();
    }

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected AnyObjectDAO anyObjectDAO;

    protected int indexMaxResultWindow = 10000;

    protected int retryOnConflict = 5;

    protected int numberOfShards = 1;

    protected int numberOfReplicas = 1;

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

    public int getNumberOfShards() {
        return numberOfShards;
    }

    public void setNumberOfShards(final int numberOfShards) {
        this.numberOfShards = numberOfShards;
    }

    public int getNumberOfReplicas() {
        return numberOfReplicas;
    }

    public void setNumberOfReplicas(final int numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }

    /**
     * Returns the builder specialized with content from the provided any.
     *
     * @param any user, group or any object to index
     * @param domain tenant information
     * @return builder specialized with content from the provided any
     * @throws IOException in case of errors
     */
    @Transactional
    public XContentBuilder builder(final Any<?> any, final String domain) throws IOException {
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

        XContentBuilder builder = XContentFactory.jsonBuilder().
                startObject().
                field("id", any.getKey()).
                field("realm", any.getRealm().getFullPath()).
                field("anyType", any.getType().getKey()).
                field("creationDate", any.getCreationDate()).
                field("creationContext", any.getCreationContext()).
                field("creator", any.getCreator()).
                field("lastChangeDate", any.getLastChangeDate()).
                field("lastModifier", any.getLastModifier()).
                field("lastChangeContext", any.getLastChangeContext()).
                field("status", any.getStatus()).
                field("resources", resources).
                field("dynRealms", dynRealms);

        if (any instanceof AnyObject) {
            AnyObject anyObject = ((AnyObject) any);
            builder = builder.field("name", anyObject.getName());

            Collection<String> memberships = AuthContextUtils.callAsAdmin(
                    domain, () -> anyObjectDAO.findAllGroupKeys(anyObject));
            builder = builder.field("memberships", memberships);

            List<String> relationships = new ArrayList<>();
            List<String> relationshipTypes = new ArrayList<>();
            AuthContextUtils.callAsAdmin(domain, () -> {
                anyObjectDAO.findAllRelationships(anyObject).forEach(relationship -> {
                    relationships.add(relationship.getRightEnd().getKey());
                    relationshipTypes.add(relationship.getType().getKey());
                });
                return null;
            });
            builder = builder.field("relationships", relationships);
            builder = builder.field("relationshipTypes", relationshipTypes);

            builder = customizeBuilder(builder, anyObject, domain);
        } else if (any instanceof Group) {
            Group group = ((Group) any);
            builder = builder.field("name", group.getName());
            if (group.getUserOwner() != null) {
                builder = builder.field("userOwner", group.getUserOwner().getKey());
            }
            if (group.getGroupOwner() != null) {
                builder = builder.field("groupOwner", group.getGroupOwner().getKey());
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
            builder = builder.field("members", members);

            builder = customizeBuilder(builder, group, domain);
        } else if (any instanceof User) {
            User user = ((User) any);
            builder = builder.
                    field("username", user.getUsername()).
                    field("token", user.getToken()).
                    field("tokenExpireTime", user.getTokenExpireTime()).
                    field("changePwdDate", user.getChangePwdDate()).
                    field("failedLogins", user.getFailedLogins()).
                    field("lastLoginDate", user.getLastLoginDate()).
                    field("suspended", user.isSuspended()).
                    field("mustChangePassword", user.isMustChangePassword());

            List<String> roles = new ArrayList<>();
            Set<String> privileges = new HashSet<>();
            AuthContextUtils.callAsAdmin(domain, () -> {
                userDAO.findAllRoles(user).forEach(role -> {
                    roles.add(role.getKey());
                    privileges.addAll(role.getPrivileges().stream().map(Privilege::getKey).collect(Collectors.toSet()));
                });
                return null;
            });
            builder = builder.field("roles", roles);
            builder = builder.field("privileges", privileges);

            Collection<String> memberships = AuthContextUtils.callAsAdmin(
                    domain, () -> userDAO.findAllGroupKeys(user));
            builder = builder.field("memberships", memberships);

            List<String> relationships = new ArrayList<>();
            Set<String> relationshipTypes = new HashSet<>();
            user.getRelationships().forEach(relationship -> {
                relationships.add(relationship.getRightEnd().getKey());
                relationshipTypes.add(relationship.getType().getKey());
            });
            builder = builder.field("relationships", relationships);
            builder = builder.field("relationshipTypes", relationshipTypes);

            builder = customizeBuilder(builder, user, domain);
        }

        for (PlainAttr<?> plainAttr : any.getPlainAttrs()) {
            List<Object> values = plainAttr.getValues().stream().
                    map(PlainAttrValue::getValue).collect(Collectors.toList());

            if (plainAttr.getUniqueValue() != null) {
                values.add(plainAttr.getUniqueValue().getValue());
            }

            builder = builder.field(plainAttr.getSchema().getKey(), values.size() == 1 ? values.get(0) : values);
        }

        return builder.endObject();
    }

    protected XContentBuilder customizeBuilder(
            final XContentBuilder builder, final AnyObject anyObject, final String domain)
            throws IOException {

        return builder;
    }

    protected XContentBuilder customizeBuilder(
            final XContentBuilder builder, final Group group, final String domain)
            throws IOException {

        return builder;
    }

    protected XContentBuilder customizeBuilder(final XContentBuilder builder, final User user, final String domain)
            throws IOException {

        return builder;
    }
}
