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
package org.apache.syncope.core.persistence.jpa.entity.membership;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.membership.MDerAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MDerSchema;
import org.apache.syncope.core.persistence.api.entity.role.Role;
import org.apache.syncope.core.persistence.jpa.entity.AbstractDerAttrTemplate;
import org.apache.syncope.core.persistence.jpa.entity.role.JPARole;

@Entity
@Table(name = JPAMDerAttrTemplate.TABLE)
public class JPAMDerAttrTemplate extends AbstractDerAttrTemplate<MDerSchema> implements MDerAttrTemplate {

    private static final long serialVersionUID = -4465930976210263434L;

    public static final String TABLE = "MDerAttrTemplate";

    @ManyToOne
    private JPARole owner;

    @ManyToOne
    @JoinColumn(name = "schema_name")
    private JPAMDerSchema schema;

    @Override
    public MDerSchema getSchema() {
        return schema;
    }

    @Override
    public void setSchema(final MDerSchema schema) {
        checkType(schema, JPAMDerSchema.class);
        this.schema = (JPAMDerSchema) schema;
    }

    @Override
    public Role getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final Role owner) {
        checkType(owner, JPARole.class);
        this.owner = (JPARole) owner;
    }
}
