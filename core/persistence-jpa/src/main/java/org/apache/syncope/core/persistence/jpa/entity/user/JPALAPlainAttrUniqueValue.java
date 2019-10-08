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
package org.apache.syncope.core.persistence.jpa.entity.user;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.JPAPlainSchema;

@Entity
@Table(name = JPALAPlainAttrUniqueValue.TABLE)
public class JPALAPlainAttrUniqueValue extends AbstractPlainAttrValue implements LAPlainAttrUniqueValue {

    private static final long serialVersionUID = 1200617357906733442L;

    public static final String TABLE = "LAPlainAttrUniqueValue";

    @OneToOne(optional = false)
    private JPALAPlainAttr attribute;

    @ManyToOne(optional = false)
    @JoinColumn(name = "schema_id")
    private JPAPlainSchema schema;

    @Override
    public LAPlainAttr getAttr() {
        return attribute;
    }

    @Override
    public void setAttr(final PlainAttr<?> attr) {
        checkType(attr, JPALAPlainAttr.class);
        this.attribute = (JPALAPlainAttr) attr;
    }

    @Override
    public PlainSchema getSchema() {
        return schema;
    }

    @Override
    public void setSchema(final PlainSchema schema) {
        checkType(schema, JPAPlainSchema.class);
        this.schema = (JPAPlainSchema) schema;
    }
}
