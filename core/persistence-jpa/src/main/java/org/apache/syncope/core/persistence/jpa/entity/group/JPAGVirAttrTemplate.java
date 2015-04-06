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
import org.apache.syncope.core.persistence.api.entity.group.GVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GVirSchema;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.jpa.entity.AbstractVirAttrTemplate;

@Entity
@Table(name = JPAGVirAttrTemplate.TABLE)
public class JPAGVirAttrTemplate extends AbstractVirAttrTemplate<GVirSchema> implements GVirAttrTemplate {

    private static final long serialVersionUID = 4896495904794493479L;

    public static final String TABLE = "GVirAttrTemplate";

    @ManyToOne
    private JPAGroup owner;

    @ManyToOne
    @JoinColumn(name = "schema_name")
    private JPAGVirSchema schema;

    @Override
    public GVirSchema getSchema() {
        return schema;
    }

    @Override
    public void setSchema(final GVirSchema schema) {
        checkType(schema, JPAGVirSchema.class);
        this.schema = (JPAGVirSchema) schema;
    }

    @Override
    public Group getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final Group group) {
        checkType(group, JPAGroup.class);
        this.owner = (JPAGroup) group;
    }
}
