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

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.VirSchema;

@MappedSuperclass
public abstract class AbstractVirAttr<O extends Any<?, ?, ?>>
        extends AbstractAttr<VirSchema, O> implements VirAttr<O> {

    private static final long serialVersionUID = 5023204776925954907L;

    @Transient
    protected List<String> values = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @Column(name = "schema_name")
    private JPAVirSchema schema;

    @Override
    public List<String> getValues() {
        return values;
    }

    @Override
    public boolean add(final String value) {
        return !values.contains(value) && values.add(value);
    }

    @Override
    public boolean remove(final String value) {
        return values.remove(value);
    }

    @Override
    public VirSchema getSchema() {
        return schema;
    }

    @Override
    public void setSchema(final VirSchema schema) {
        checkType(schema, JPAVirSchema.class);
        this.schema = (JPAVirSchema) schema;
        checkSchema(this.schema);
    }
}
