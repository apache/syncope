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
package org.apache.syncope.core.persistence.jpa.entity.group;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.group.GDerAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GDerSchema;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.jpa.entity.AbstractDerAttrTemplate;

@Entity
@Table(name = JPAGDerAttrTemplate.TABLE)
public class JPAGDerAttrTemplate extends AbstractDerAttrTemplate<GDerSchema> implements GDerAttrTemplate {

    private static final long serialVersionUID = 624868884107016649L;

    public static final String TABLE = "GDerAttrTemplate";

    @ManyToOne
    private JPAGroup owner;

    @ManyToOne
    @JoinColumn(name = "schema_name")
    private JPAGDerSchema schema;

    @Override
    public GDerSchema getSchema() {
        return schema;
    }

    @Override
    public void setSchema(final GDerSchema schema) {
        checkType(schema, JPAGDerSchema.class);
        this.schema = (JPAGDerSchema) schema;
    }

    @Override
    public Group getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final Group owner) {
        checkType(owner, JPAGroup.class);
        this.owner = (JPAGroup) owner;
    }
}
