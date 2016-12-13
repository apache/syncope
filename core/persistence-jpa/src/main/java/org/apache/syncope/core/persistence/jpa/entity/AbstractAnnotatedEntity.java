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
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.syncope.core.persistence.api.entity.AnnotatedEntity;

/**
 * Abstract wrapper for common system information.
 */
@MappedSuperclass
@EntityListeners(value = AnnotatedEntityListener.class)
public abstract class AbstractAnnotatedEntity extends AbstractGeneratedKeyEntity implements AnnotatedEntity {

    private static final long serialVersionUID = -4801685541488201219L;

    /**
     * Username of the user that has created this profile.
     * Reference to existing user cannot be used: the creator can either be <tt>admin</tt> or was deleted.
     */
    private String creator;

    /**
     * Creation date.
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    /**
     * Username of the user that has performed the last modification to this profile.
     * This field cannot be null: at creation time it needs to be initialized with the creator username.
     * The modifier can be the user itself if the last performed change was a self-modification.
     * Reference to existing user cannot be used: the creator can either be <tt>admin</tt> or was deleted.
     */
    private String lastModifier;

    /**
     * Last change date.
     * This field cannot be null: at creation time it needs to be initialized with <tt>creationDate</tt> field value.
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastChangeDate;

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
        return creationDate == null ? null : new Date(creationDate.getTime());
    }

    @Override
    public void setCreationDate(final Date creationDate) {
        this.creationDate = creationDate == null ? null : new Date(creationDate.getTime());
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
        this.lastChangeDate = lastChangeDate == null ? null : new Date(lastChangeDate.getTime());
    }
}
