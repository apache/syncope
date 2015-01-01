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
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.apache.syncope.persistence.api.entity.PlainAttr;
import org.apache.syncope.persistence.api.entity.PlainSchema;
import org.apache.syncope.persistence.api.entity.role.RPlainAttr;
import org.apache.syncope.persistence.api.entity.role.RPlainAttrUniqueValue;
import org.apache.syncope.persistence.api.entity.role.RPlainSchema;
import org.apache.syncope.persistence.jpa.entity.AbstractPlainAttrValue;

@Entity
@Table(name = JPARPlainAttrUniqueValue.TABLE)
public class JPARPlainAttrUniqueValue extends AbstractPlainAttrValue implements RPlainAttrUniqueValue {

    private static final long serialVersionUID = 4681561795607192855L;

    public static final String TABLE = "RPlainAttrUniqueValue";

    @Id
    private Long id;

    @OneToOne(optional = false)
    private JPARPlainAttr attribute;

    @ManyToOne(optional = false)
    @JoinColumn(name = "schema_name")
    private JPARPlainSchema schema;

    @Override
    public Long getKey() {
        return id;
    }

    @Override
    public RPlainAttr getAttr() {
        return attribute;
    }

    @Override
    public void setAttr(final PlainAttr attr) {
        checkType(attr, JPARPlainAttr.class);
        this.attribute = (JPARPlainAttr) attr;
    }

    @Override
    public RPlainSchema getSchema() {
        return schema;
    }

    @Override
    public void setSchema(final PlainSchema schema) {
        checkType(schema, JPARPlainSchema.class);
        this.schema = (JPARPlainSchema) schema;
    }
}
