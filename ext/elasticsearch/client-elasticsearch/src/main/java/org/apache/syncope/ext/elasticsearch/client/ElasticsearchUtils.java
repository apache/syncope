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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.ARelationship;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.utils.EntityUtils;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Utility methods for usage with Elasticsearch.
 */
public class ElasticsearchUtils {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    private int indexMaxResultWindow = 10000;

    public void setIndexMaxResultWindow(final int indexMaxResultWindow) {
        this.indexMaxResultWindow = indexMaxResultWindow;
    }

    public int getIndexMaxResultWindow() {
        return indexMaxResultWindow;
    }

    /**
     * Returns the builder specialized with content from the provided any.
     *
     * @param any user, group or any object to index
     * @return builder specialized with content from the provided any
     * @throws IOException in case of errors
     */
    @Transactional
    public XContentBuilder builder(final Any<?> any) throws IOException {
        XContentBuilder builder = XContentFactory.jsonBuilder().
                startObject().
                field("id", any.getKey()).
                field("realm", any.getRealm().getFullPath()).
                field("anyType", any.getType().getKey()).
                field("creationDate", any.getCreationDate()).
                field("creator", any.getCreator()).
                field("lastChangeDate", any.getLastChangeDate()).
                field("lastModified", any.getLastModifier()).
                field("status", any.getStatus()).
                field("resources",
                        any instanceof User
                                ? userDAO.findAllResourceKeys(any.getKey())
                                : any instanceof AnyObject
                                        ? anyObjectDAO.findAllResourceKeys(any.getKey())
                                        : any.getResourceKeys());

        if (any instanceof AnyObject) {
            AnyObject anyObject = ((AnyObject) any);
            builder = builder.field("name", anyObject.getName());

            List<Object> memberships = new ArrayList<Object>(anyObjectDAO.findAllGroupKeys(anyObject));
            builder = builder.field("memberships", memberships);

            List<Object> relationships = new ArrayList<>();
            List<Object> relationshipTypes = new ArrayList<>();
            for (ARelationship relationship : anyObjectDAO.findAllARelationships(anyObject)) {
                relationships.add(relationship.getRightEnd().getKey());
                relationshipTypes.add(relationship.getType().getKey());
            }
            builder = builder.field("relationships", relationships);
            builder = builder.field("relationshipTypes", relationshipTypes);
        } else if (any instanceof Group) {
            Group group = ((Group) any);
            builder = builder.field("name", group.getName());
            if (group.getUserOwner() != null) {
                builder = builder.field("userOwner", group.getUserOwner().getKey());
            }
            if (group.getGroupOwner() != null) {
                builder = builder.field("groupOwner", group.getGroupOwner().getKey());
            }

            List<Object> members = CollectionUtils.collect(groupDAO.findUMemberships(group),
                    new Transformer<UMembership, Object>() {

                @Override
                public Object transform(final UMembership input) {
                    return input.getLeftEnd().getKey();
                }
            }, new ArrayList<>());
            members.add(groupDAO.findUDynMembersKeys(group));
            CollectionUtils.collect(groupDAO.findAMemberships(group),
                    new Transformer<AMembership, Object>() {

                @Override
                public Object transform(final AMembership input) {
                    return input.getLeftEnd().getKey();
                }
            }, members);
            members.add(groupDAO.findADynMembersKeys(group));
            builder = builder.field("members", members);
        } else if (any instanceof User) {
            User user = ((User) any);
            builder = builder.
                    field("username", user.getUsername()).
                    field("lastLoginDate", user.getLastLoginDate()).
                    field("lastRecertification", user.getLastRecertification()).
                    field("lastRecertificator", user.getLastRecertificator());

            List<Object> roles = CollectionUtils.collect(userDAO.findAllRoles(user),
                    EntityUtils.<Role>keyTransformer(), new ArrayList<>());
            builder = builder.field("roles", roles);

            List<Object> memberships = new ArrayList<Object>(userDAO.findAllGroupKeys(user));
            builder = builder.field("memberships", memberships);

            List<Object> relationships = new ArrayList<>();
            Set<Object> relationshipTypes = new HashSet<>();
            for (URelationship relationship : user.getRelationships()) {
                relationships.add(relationship.getRightEnd().getKey());
                relationshipTypes.add(relationship.getType().getKey());
            }
            builder = builder.field("relationships", relationships);
            builder = builder.field("relationshipTypes", relationshipTypes);
        }

        for (PlainAttr<?> plainAttr : any.getPlainAttrs()) {
            List<Object> values = CollectionUtils.collect(plainAttr.getValues(),
                    new Transformer<PlainAttrValue, Object>() {

                @Override
                public Object transform(final PlainAttrValue input) {
                    return input.getValue();
                }
            }, new ArrayList<>(plainAttr.getValues().size()));
            if (plainAttr.getUniqueValue() != null) {
                values.add(plainAttr.getUniqueValue().getValue());
            }

            builder = builder.field(plainAttr.getSchema().getKey(), values);
        }

        builder = builder.endObject();

        return builder;
    }
}
