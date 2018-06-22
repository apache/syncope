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

import java.util.Locale;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.SchemaLabel;

@Entity
@Table(name = JPASchemaLabel.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "schema_id", "locale" }))
public class JPASchemaLabel extends AbstractGeneratedKeyEntity implements SchemaLabel {

    public static final String TABLE = "SchemaLabel";

    private static final long serialVersionUID = -546019894866607764L;

    @ManyToOne
    private AbstractSchema schema;

    @Column(nullable = false)
    private Locale locale;

    @Column(nullable = false)
    private String display;

    @Override
    public AbstractSchema getSchema() {
        return schema;
    }

    @Override
    public void setSchema(final Schema schema) {
        checkType(schema, AbstractSchema.class);
        this.schema = (AbstractSchema) schema;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public void setLocale(final Locale locale) {
        this.locale = locale;
    }

    @Override
    public String getDisplay() {
        return display;
    }

    @Override
    public void setDisplay(final String display) {
        this.display = display;
    }
}
