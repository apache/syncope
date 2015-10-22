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
package org.apache.syncope.core.persistence.jpa.entity;

import java.util.Collection;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import org.apache.syncope.core.persistence.api.entity.DerAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.misc.jexl.JexlUtils;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.DerSchema;

@MappedSuperclass
public abstract class AbstractDerAttr<O extends Any<?, ?>>
        extends AbstractAttr<DerSchema, O> implements DerAttr<O> {

    private static final long serialVersionUID = 4740924251090424771L;

    @ManyToOne(fetch = FetchType.EAGER)
    @Column(name = "schema_name")
    private JPADerSchema schema;

    @Override
    public DerSchema getSchema() {
        return schema;
    }

    @Override
    public void setSchema(final DerSchema schema) {
        checkType(schema, JPADerSchema.class);
        this.schema = (JPADerSchema) schema;
        checkSchema(this.schema);
    }

    /**
     * @param attributes the set of attributes against which evaluate this derived attribute
     * @return the value of this derived attribute
     */
    @Override
    public String getValue(final Collection<? extends PlainAttr<?>> attributes) {
        return JexlUtils.evaluate(getSchema().getExpression(), getOwner(), attributes);
    }
}
