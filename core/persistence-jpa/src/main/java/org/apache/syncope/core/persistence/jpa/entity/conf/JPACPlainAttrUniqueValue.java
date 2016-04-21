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
package org.apache.syncope.core.persistence.jpa.entity.conf;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.JPAPlainSchema;

@Entity
@Table(name = JPACPlainAttrUniqueValue.TABLE)
public class JPACPlainAttrUniqueValue extends AbstractPlainAttrValue implements CPlainAttrUniqueValue {

    private static final long serialVersionUID = -2072445894710677162L;

    public static final String TABLE = "CPlainAttrUniqueValue";

    @OneToOne(optional = false)
    private JPACPlainAttr attribute;

    @ManyToOne(optional = false)
    @JoinColumn(name = "schema_id")
    private JPAPlainSchema schema;

    @Override
    public CPlainAttr getAttr() {
        return attribute;
    }

    @Override
    public void setAttr(final PlainAttr<?> attr) {
        checkType(attr, JPACPlainAttr.class);
        this.attribute = (JPACPlainAttr) attr;
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
