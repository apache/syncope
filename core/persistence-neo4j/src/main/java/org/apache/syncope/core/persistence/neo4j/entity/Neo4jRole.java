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

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.core.persistence.api.entity.Application;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.Privilege;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.common.validation.RoleCheck;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.PostLoad;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jRole.NODE)
@RoleCheck
public class Neo4jRole extends AbstractProvidedKeyNode implements Role {

    private static final long serialVersionUID = -7657701119422588832L;

    public static final String NODE = "SyncopeRole";

    public static final String ROLE_REALM_REL = "ROLE_REALM";

    public static final String ROLE_PRIVILEGE_REL = "ROLE_PRIVILEGE";

    protected static final TypeReference<Set<String>> TYPEREF = new TypeReference<Set<String>>() {
    };

    private String entitlements;

    @Transient
    private Set<String> entitlementsSet = new HashSet<>();

    private String dynMembershipCond;

    private String anyLayout;

    @Relationship(type = ROLE_REALM_REL, direction = Relationship.Direction.OUTGOING)
    private List<Neo4jRealm> realms = new ArrayList<>();

    @Relationship(direction = Relationship.Direction.INCOMING)
    private List<Neo4jDynRealm> dynRealms = new ArrayList<>();

    @Relationship(type = ROLE_PRIVILEGE_REL, direction = Relationship.Direction.OUTGOING)
    private Set<Neo4jPrivilege> privileges = new HashSet<>();

    @Override
    public Set<String> getEntitlements() {
        return entitlementsSet;
    }

    @Override
    public String getDynMembershipCond() {
        return dynMembershipCond;
    }

    @Override
    public void setDynMembershipCond(final String dynMembershipCond) {
        this.dynMembershipCond = dynMembershipCond;
    }

    @Override
    public boolean add(final Realm realm) {
        checkType(realm, Neo4jRealm.class);
        return realms.contains((Neo4jRealm) realm) || realms.add((Neo4jRealm) realm);
    }

    @Override
    public List<? extends Realm> getRealms() {
        return realms;
    }

    @Override
    public boolean add(final DynRealm dynamicRealm) {
        checkType(dynamicRealm, Neo4jDynRealm.class);
        return dynRealms.contains((Neo4jDynRealm) dynamicRealm) || dynRealms.add((Neo4jDynRealm) dynamicRealm);
    }

    @Override
    public List<? extends DynRealm> getDynRealms() {
        return dynRealms;
    }

    @Override
    public String getAnyLayout() {
        return anyLayout;
    }

    @Override
    public void setAnyLayout(final String anyLayout) {
        this.anyLayout = anyLayout;
    }

    @Override
    public boolean add(final Privilege privilege) {
        checkType(privilege, Neo4jPrivilege.class);
        return privileges.add((Neo4jPrivilege) privilege);
    }

    @Override
    public Set<? extends Privilege> getPrivileges(final Application application) {
        return privileges.stream().
                filter(privilege -> privilege.getApplication().equals(application)).
                collect(Collectors.toSet());
    }

    @Override
    public Set<? extends Privilege> getPrivileges() {
        return privileges;
    }

    protected void json2list(final boolean clearFirst) {
        if (clearFirst) {
            getEntitlements().clear();
        }
        if (entitlements != null) {
            getEntitlements().addAll(POJOHelper.deserialize(entitlements, TYPEREF));
        }
    }

    @PostLoad
    public void postLoad() {
        json2list(false);
    }

    public void postSave() {
        json2list(true);
    }

    public void list2json() {
        entitlements = POJOHelper.serialize(getEntitlements());
    }
}
