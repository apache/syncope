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
package org.apache.syncope.core.persistence.neo4j.entity;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.persistence.api.entity.AnyTemplateRealm;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.TicketExpirationPolicy;
import org.apache.syncope.core.persistence.common.validation.RealmCheck;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAccessPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAccountPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAttrReleasePolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAuthPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPasswordPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jTicketExpirationPolicy;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.PostLoad;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jRealm.NODE)
@RealmCheck
public class Neo4jRealm extends AbstractGeneratedKeyNode implements Realm {

    private static final long serialVersionUID = 1807304075247552949L;

    public static final String NODE = "Realm";

    public static final String PARENT_REL = "PARENT";

    public static final String REALM_PASSWORD_POLICY_REL = "REALM_PASSWORD_POLICY";

    public static final String REALM_ACCOUNT_POLICY_REL = "REALM_ACCOUNT_POLICY";

    public static final String REALM_AUTH_POLICY_REL = "REALM_AUTH_POLICY";

    public static final String REALM_ACCESS_POLICY_REL = "REALM_ACCESS_POLICY";

    public static final String REALM_ATTR_RELEASE_POLICY_REL = "REALM_ATTR_RELEASE_POLICY";

    public static final String REALM_TICKET_EXPIRATION_POLICY_REL = "REALM_TICKET_EXPIRATION_POLICY";

    public static final String REALM_LOGIC_ACTIONS_REL = "REALM_LOGIC_ACTIONS";

    public static final String REALM_RESOURCE_REL = "REALM_RESOURCE";

    @Size(min = 1)
    private String name;

    @Relationship(type = PARENT_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jRealm parent;

    @NotNull
    private String fullPath;

    @Relationship(type = REALM_PASSWORD_POLICY_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jPasswordPolicy passwordPolicy;

    @Relationship(type = REALM_ACCOUNT_POLICY_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jAccountPolicy accountPolicy;

    @Relationship(type = REALM_AUTH_POLICY_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jAuthPolicy authPolicy;

    @Relationship(type = REALM_ACCESS_POLICY_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jAccessPolicy accessPolicy;

    @Relationship(type = REALM_ATTR_RELEASE_POLICY_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jAttrReleasePolicy attrReleasePolicy;

    @Relationship(type = REALM_TICKET_EXPIRATION_POLICY_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jTicketExpirationPolicy ticketExpirationPolicy;

    @Relationship(type = REALM_LOGIC_ACTIONS_REL, direction = Relationship.Direction.OUTGOING)
    private SortedSet<Neo4jImplementationRelationship> actions = new TreeSet<>();

    @Transient
    private List<Neo4jImplementation> sortedActions = new SortedSetList<>(
            actions, Neo4jImplementationRelationship.builder());

    @Relationship(type = Neo4jAnyTemplateRealm.REALM_ANY_TEMPLATE_REL, direction = Relationship.Direction.INCOMING)
    private List<Neo4jAnyTemplateRealm> templates = new ArrayList<>();

    @Relationship(type = REALM_RESOURCE_REL, direction = Relationship.Direction.OUTGOING)
    private List<Neo4jExternalResource> resources = new ArrayList<>();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Realm getParent() {
        return parent;
    }

    @Override
    public String getFullPath() {
        return fullPath;
    }

    @Override
    public AccountPolicy getAccountPolicy() {
        return accountPolicy == null && getParent() != null ? getParent().getAccountPolicy() : accountPolicy;
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        return passwordPolicy == null && getParent() != null ? getParent().getPasswordPolicy() : passwordPolicy;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public void setParent(final Realm parent) {
        checkType(parent, Neo4jRealm.class);
        this.parent = (Neo4jRealm) parent;
    }

    public void setFullPath(final String fullPath) {
        this.fullPath = fullPath;
    }

    @Override
    public void setAccountPolicy(final AccountPolicy accountPolicy) {
        checkType(accountPolicy, Neo4jAccountPolicy.class);
        this.accountPolicy = (Neo4jAccountPolicy) accountPolicy;
    }

    @Override
    public void setPasswordPolicy(final PasswordPolicy passwordPolicy) {
        checkType(passwordPolicy, Neo4jPasswordPolicy.class);
        this.passwordPolicy = (Neo4jPasswordPolicy) passwordPolicy;
    }

    @Override
    public AuthPolicy getAuthPolicy() {
        return authPolicy;
    }

    @Override
    public void setAuthPolicy(final AuthPolicy authPolicy) {
        checkType(authPolicy, Neo4jAuthPolicy.class);
        this.authPolicy = (Neo4jAuthPolicy) authPolicy;
    }

    @Override
    public AccessPolicy getAccessPolicy() {
        return accessPolicy;
    }

    @Override
    public void setAccessPolicy(final AccessPolicy accessPolicy) {
        checkType(accessPolicy, Neo4jAccessPolicy.class);
        this.accessPolicy = (Neo4jAccessPolicy) accessPolicy;
    }

    @Override
    public boolean add(final Implementation action) {
        checkType(action, Neo4jImplementation.class);
        checkImplementationType(action, IdRepoImplementationType.LOGIC_ACTIONS);
        return sortedActions.contains((Neo4jImplementation) action) || sortedActions.add((Neo4jImplementation) action);
    }

    @Override
    public void setAttrReleasePolicy(final AttrReleasePolicy policy) {
        checkType(policy, Neo4jAttrReleasePolicy.class);
        this.attrReleasePolicy = (Neo4jAttrReleasePolicy) policy;
    }

    @Override
    public AttrReleasePolicy getAttrReleasePolicy() {
        return this.attrReleasePolicy;
    }

    @Override
    public TicketExpirationPolicy getTicketExpirationPolicy() {
        return this.ticketExpirationPolicy;
    }

    @Override
    public void setTicketExpirationPolicy(final TicketExpirationPolicy policy) {
        checkType(policy, Neo4jTicketExpirationPolicy.class);
        this.ticketExpirationPolicy = (Neo4jTicketExpirationPolicy) policy;
    }

    @Override
    public List<? extends Implementation> getActions() {
        return sortedActions;
    }

    @Override
    public boolean add(final AnyTemplateRealm template) {
        checkType(template, Neo4jAnyTemplateRealm.class);
        return this.templates.add((Neo4jAnyTemplateRealm) template);
    }

    @Override
    public Optional<? extends AnyTemplateRealm> getTemplate(final AnyType anyType) {
        return templates.stream().
                filter(template -> anyType != null && anyType.equals(template.getAnyType())).
                findFirst();
    }

    @Override
    public List<? extends AnyTemplateRealm> getTemplates() {
        return templates;
    }

    @Override
    public boolean add(final ExternalResource resource) {
        checkType(resource, Neo4jExternalResource.class);
        return resources.contains((Neo4jExternalResource) resource) || resources.add((Neo4jExternalResource) resource);
    }

    @Override
    public List<String> getResourceKeys() {
        return getResources().stream().map(ExternalResource::getKey).toList();
    }

    @Override
    public List<? extends ExternalResource> getResources() {
        return resources;
    }

    @PostLoad
    public void postLoad() {
        sortedActions = new SortedSetList<>(actions, Neo4jImplementationRelationship.builder());
    }
}
