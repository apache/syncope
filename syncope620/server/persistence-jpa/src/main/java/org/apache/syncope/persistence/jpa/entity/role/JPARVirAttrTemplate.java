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
package org.apache.syncope.persistence.jpa.entity.role;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.syncope.persistence.api.entity.role.RVirAttrTemplate;
import org.apache.syncope.persistence.api.entity.role.RVirSchema;
import org.apache.syncope.persistence.api.entity.role.Role;
import org.apache.syncope.persistence.jpa.entity.AbstractAttrTemplate;

@Entity
@Table(name = JPARVirAttrTemplate.TABLE)
public class JPARVirAttrTemplate extends AbstractAttrTemplate<RVirSchema> implements RVirAttrTemplate {

    private static final long serialVersionUID = 4896495904794493479L;

    public static final String TABLE = "RVirAttrTemplate";

    @ManyToOne
    private JPARole owner;

    @ManyToOne
    @JoinColumn(name = "schema_name")
    private JPARVirSchema schema;

    @Override
    public RVirSchema getSchema() {
        return schema;
    }

    @Override
    public void setSchema(final RVirSchema schema) {
        checkType(schema, JPARVirSchema.class);
        this.schema = (JPARVirSchema) schema;
    }

    @Override
    public Role getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final Role role) {
        checkType(role, JPARole.class);
        this.owner = (JPARole) role;
    }
}
