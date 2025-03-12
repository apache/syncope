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
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.common.validation.AnyCheck;
import org.springframework.data.neo4j.core.schema.PostLoad;
import org.springframework.data.neo4j.core.schema.Relationship;

@AnyCheck
public abstract class AbstractAny extends AbstractGeneratedKeyNode implements Any {

    private static final long serialVersionUID = -2666540708092702810L;

    /**
     * Username of the user that has created the related instance.
     */
    private String creator;

    /**
     * Creation date.
     */
    private OffsetDateTime creationDate;

    /**
     * Context information about create.
     */
    private String creationContext;

    /**
     * Username of the user that has performed the last modification to the related instance.
     */
    private String lastModifier;

    private OffsetDateTime lastChangeDate;

    /**
     * Context information about last update.
     */
    private String lastChangeContext;

    @NotNull
    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jRealm realm;

    private String status;

    @Override
    public String getCreator() {
        return creator;
    }

    @Override
    public void setCreator(final String creator) {
        this.creator = creator;
    }

    @Override
    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    @Override
    public void setCreationDate(final OffsetDateTime creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public String getCreationContext() {
        return creationContext;
    }

    @Override
    public void setCreationContext(final String creationContext) {
        this.creationContext = creationContext;
    }

    @Override
    public String getLastModifier() {
        return lastModifier;
    }

    @Override
    public void setLastModifier(final String lastModifier) {
        this.lastModifier = lastModifier;
    }

    @Override
    public OffsetDateTime getLastChangeDate() {
        if (lastChangeDate != null) {
            return lastChangeDate;
        } else if (creationDate != null) {
            return creationDate;
        }

        return null;
    }

    @Override
    public void setLastChangeDate(final OffsetDateTime lastChangeDate) {
        this.lastChangeDate = lastChangeDate;
    }

    @Override
    public String getLastChangeContext() {
        return lastChangeContext;
    }

    @Override
    public void setLastChangeContext(final String lastChangeContext) {
        this.lastChangeContext = lastChangeContext;
    }

    @Override
    public Realm getRealm() {
        return realm;
    }

    @Override
    public void setRealm(final Realm realm) {
        checkType(realm, Neo4jRealm.class);
        this.realm = (Neo4jRealm) realm;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void setStatus(final String status) {
        this.status = status;
    }

    protected abstract Map<String, PlainAttr> plainAttrs();

    @Override
    public Optional<PlainAttr> getPlainAttr(final String plainSchema) {
        return Optional.ofNullable(plainAttrs().get(plainSchema));
    }

    @SuppressWarnings("unchecked")
    protected void doComplete(final Map<String, PlainAttr> plainAttrs) {
        for (var itor = plainAttrs.entrySet().iterator(); itor.hasNext();) {
            var entry = itor.next();
            Optional.ofNullable(entry.getValue()).ifPresent(attr -> {
                attr.setSchema(entry.getKey());
                if (attr.getSchema() == null) {
                    itor.remove();
                } else {
                    attr.getValues().forEach(value -> value.setAttr(attr));
                    Optional.ofNullable(attr.getUniqueValue()).ifPresent(value -> value.setAttr(attr));
                }
            });
        }
    }

    @PostLoad
    public void completePlainAttrs() {
        doComplete(plainAttrs());
    }
}
