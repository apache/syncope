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
package org.apache.syncope.core.persistence.jpa.entity;

import java.util.Date;
import java.util.Optional;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.jpa.validation.entity.AnyCheck;

@AnyCheck
@MappedSuperclass
public abstract class AbstractAny<P extends PlainAttr<?>> extends AbstractGeneratedKeyEntity implements Any<P> {

    private static final long serialVersionUID = -2666540708092702810L;

    /**
     * Username of the user that has created the related instance.
     */
    private String creator;

    /**
     * Creation date.
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    /**
     * Context information about create.
     */
    private String creationContext;

    /**
     * Username of the user that has performed the last modification to the related instance.
     */
    private String lastModifier;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastChangeDate;

    /**
     * Context information about last update.
     */
    private String lastChangeContext;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private JPARealm realm;

    @Column(nullable = true)
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
    public Date getCreationDate() {
        return Optional.ofNullable(creationDate).map(date -> new Date(date.getTime())).orElse(null);
    }

    @Override
    public void setCreationDate(final Date creationDate) {
        this.creationDate = Optional.ofNullable(creationDate).map(date -> new Date(date.getTime())).orElse(null);
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
    public Date getLastChangeDate() {
        if (lastChangeDate != null) {
            return new Date(lastChangeDate.getTime());
        } else if (creationDate != null) {
            return new Date(creationDate.getTime());
        }

        return null;
    }

    @Override
    public void setLastChangeDate(final Date lastChangeDate) {
        this.lastChangeDate = Optional.ofNullable(lastChangeDate).map(date -> new Date(date.getTime())).orElse(null);
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
        checkType(realm, JPARealm.class);
        this.realm = (JPARealm) realm;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void setStatus(final String status) {
        this.status = status;
    }
}
