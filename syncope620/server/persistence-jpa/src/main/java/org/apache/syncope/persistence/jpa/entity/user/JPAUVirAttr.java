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
package org.apache.syncope.persistence.jpa.entity.user;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.syncope.persistence.api.entity.Attributable;
import org.apache.syncope.persistence.api.entity.VirSchema;
import org.apache.syncope.persistence.api.entity.user.UVirAttr;
import org.apache.syncope.persistence.api.entity.user.UVirSchema;
import org.apache.syncope.persistence.api.entity.user.User;
import org.apache.syncope.persistence.jpa.entity.AbstractVirAttr;

@Entity
@Table(name = JPAUVirAttr.TABLE)
public class JPAUVirAttr extends AbstractVirAttr implements UVirAttr {

    private static final long serialVersionUID = 2943450934283989741L;

    public static final String TABLE = "UVirAttr";

    @ManyToOne
    private JPAUser owner;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAUVirSchema virSchema;

    @Override
    public User getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final Attributable<?, ?, ?> owner) {
        checkType(owner, JPAUser.class);
        this.owner = (JPAUser) owner;
    }

    @Override
    public UVirSchema getSchema() {
        return virSchema;
    }

    @Override
    public void setSchema(final VirSchema virSchema) {
        checkType(virSchema, JPAUVirSchema.class);
        this.virSchema = (JPAUVirSchema) virSchema;
    }
}
