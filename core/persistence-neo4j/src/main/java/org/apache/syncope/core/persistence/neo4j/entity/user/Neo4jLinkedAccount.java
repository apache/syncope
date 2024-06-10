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
package org.apache.syncope.core.persistence.neo4j.entity.user;

import jakarta.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Privilege;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractGeneratedKeyNode;
import org.apache.syncope.core.persistence.neo4j.entity.AttributableCheck;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAttributable;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jPlainAttr;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jPrivilege;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.Encryptor;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.PostLoad;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jLinkedAccount.NODE)
@AttributableCheck
public class Neo4jLinkedAccount extends AbstractGeneratedKeyNode implements LinkedAccount, Neo4jAttributable<User> {

    private static final long serialVersionUID = -5141654998687601522L;

    public static final String NODE = "LinkedAccount";

    private static final Encryptor ENCRYPTOR = Encryptor.getInstance();

    @NotNull
    private String connObjectKeyValue;

    @NotNull
    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jUser owner;

    @NotNull
    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jExternalResource resource;

    private String username;

    private CipherAlgorithm cipherAlgorithm;

    private String password;

    private Boolean suspended = false;

    @CompositeProperty(converterRef = "laPlainAttrsConverter")
    protected Map<String, Neo4jLAPlainAttr> plainAttrs = new HashMap<>();

    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Set<Neo4jPrivilege> privileges = new HashSet<>();

    @Override
    public String getConnObjectKeyValue() {
        return connObjectKeyValue;
    }

    @Override
    public void setConnObjectKeyValue(final String connObjectKeyValue) {
        this.connObjectKeyValue = connObjectKeyValue;
    }

    @Override
    public User getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final User owner) {
        checkType(owner, Neo4jUser.class);
        this.owner = (Neo4jUser) owner;
    }

    @Override
    public ExternalResource getResource() {
        return resource;
    }

    @Override
    public void setResource(final ExternalResource resource) {
        checkType(resource, Neo4jExternalResource.class);
        this.resource = (Neo4jExternalResource) resource;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(final String username) {
        this.username = username;
    }

    @Override
    public CipherAlgorithm getCipherAlgorithm() {
        return cipherAlgorithm;
    }

    @Override
    public void setCipherAlgorithm(final CipherAlgorithm cipherAlgorithm) {
        if (this.cipherAlgorithm == null || cipherAlgorithm == null) {
            this.cipherAlgorithm = cipherAlgorithm;
        } else {
            throw new IllegalArgumentException("Cannot override existing cipher algorithm");
        }
    }

    @Override
    public boolean canDecodeSecrets() {
        return this.cipherAlgorithm != null && this.cipherAlgorithm.isInvertible();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setEncodedPassword(final String password, final CipherAlgorithm cipherAlgoritm) {
        this.password = password;
        this.cipherAlgorithm = cipherAlgoritm;
    }

    @Override
    public void setPassword(final String password) {
        try {
            this.password = ENCRYPTOR.encode(password, cipherAlgorithm == null
                    ? CipherAlgorithm.valueOf(ApplicationContextProvider.getBeanFactory().getBean(ConfParamOps.class).
                            get(AuthContextUtils.getDomain(), "password.cipher.algorithm", CipherAlgorithm.AES.name(),
                                    String.class))
                    : cipherAlgorithm);
        } catch (Exception e) {
            LOG.error("Could not encode password", e);
            this.password = null;
        }
    }

    @Override
    public void setSuspended(final Boolean suspended) {
        this.suspended = suspended;
    }

    @Override
    public Boolean isSuspended() {
        return suspended;
    }

    @Override
    public boolean add(final LAPlainAttr attr) {
        checkType(attr, Neo4jLAPlainAttr.class);
        return plainAttrs.put(((Neo4jPlainAttr<User>) attr).getSchemaKey(), (Neo4jLAPlainAttr) attr) != null;
    }

    @Override
    public boolean remove(final LAPlainAttr attr) {
        checkType(attr, Neo4jLAPlainAttr.class);
        return plainAttrs.put(((Neo4jPlainAttr<User>) attr).getSchemaKey(), null) != null;
    }

    @Override
    public Optional<? extends LAPlainAttr> getPlainAttr(final String plainSchema) {
        return Optional.ofNullable(plainAttrs.get(plainSchema));
    }

    @Override
    public List<? extends LAPlainAttr> getPlainAttrs() {
        return plainAttrs.entrySet().stream().
                filter(e -> e.getValue() != null).
                sorted(Comparator.comparing(Map.Entry::getKey)).
                map(Map.Entry::getValue).toList();
    }

    @Override
    public boolean add(final Privilege privilege) {
        checkType(privilege, Neo4jPrivilege.class);
        return privileges.add((Neo4jPrivilege) privilege);
    }

    @Override
    public Set<? extends Privilege> getPrivileges() {
        return privileges;
    }

    @PostLoad
    public void completePlainAttrs() {
        for (var itor = plainAttrs.entrySet().iterator(); itor.hasNext();) {
            var entry = itor.next();
            Optional.ofNullable(entry.getValue()).ifPresent(attr -> {
                attr.setSchemaKey(entry.getKey());
                if (attr.getSchema() == null) {
                    itor.remove();
                } else {
                    ((Neo4jLAPlainAttr) attr).setOwner(getOwner());
                    ((Neo4jLAPlainAttr) attr).setAccount(this);
                    attr.getValues().forEach(value -> value.setAttr(attr));
                    Optional.ofNullable(attr.getUniqueValue()).ifPresent(value -> value.setAttr(attr));
                }
            });
        }
    }
}
