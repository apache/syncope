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
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttrValue;

@Entity
@Table(name = JPAMPlainAttrUniqueValue.TABLE)
public class JPAMPlainAttrUniqueValue extends AbstractPlainAttrValue implements MPlainAttrUniqueValue {

    private static final long serialVersionUID = 3985867531873453718L;

    public static final String TABLE = "MPlainAttrUniqueValue";

    @Id
    private Long id;

    @OneToOne(optional = false)
    private JPAMPlainAttr attribute;

    @ManyToOne(optional = false)
    @JoinColumn(name = "schema_name")
    private JPAMPlainSchema schema;

    @Override
    public Long getKey() {
        return id;
    }

    @Override
    public MPlainAttr getAttr() {
        return attribute;
    }

    @Override
    public void setAttr(final PlainAttr attr) {
        checkType(attr, JPAMPlainAttr.class);
        this.attribute = (JPAMPlainAttr) attr;
    }

    @Override
    public MPlainSchema getSchema() {
        return schema;
    }

    @Override
    public void setSchema(final PlainSchema schema) {
        checkType(schema, JPAMPlainSchema.class);
        this.schema = (JPAMPlainSchema) schema;
    }
}
