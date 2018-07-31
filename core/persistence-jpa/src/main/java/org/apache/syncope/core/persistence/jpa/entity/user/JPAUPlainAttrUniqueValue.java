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
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.JPAPlainSchema;

@Entity
@Table(name = JPAUPlainAttrUniqueValue.TABLE)
public class JPAUPlainAttrUniqueValue extends AbstractPlainAttrValue implements UPlainAttrUniqueValue {

    private static final long serialVersionUID = -64080804563305387L;

    public static final String TABLE = "UPlainAttrUniqueValue";

    @OneToOne(optional = false)
    private JPAUPlainAttr attribute;

    @ManyToOne(optional = false)
    @JoinColumn(name = "schema_id")
    private JPAPlainSchema schema;

    @Override
    public UPlainAttr getAttr() {
        return attribute;
    }

    @Override
    public void setAttr(final PlainAttr<?> attr) {
        checkType(attr, JPAUPlainAttr.class);
        this.attribute = (JPAUPlainAttr) attr;
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
