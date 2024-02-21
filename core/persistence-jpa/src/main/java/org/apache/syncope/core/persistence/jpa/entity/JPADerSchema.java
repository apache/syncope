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

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.DerSchema;

@Entity
@Table(name = JPADerSchema.TABLE)
@PrimaryKeyJoinColumn(name = "id", referencedColumnName = "id")
public class JPADerSchema extends AbstractSchema implements DerSchema {

    private static final long serialVersionUID = -6173643493348674060L;

    public static final String TABLE = "DerSchema";

    @OneToOne(fetch = FetchType.EAGER)
    private JPAAnyTypeClass anyTypeClass;

    @NotNull
    private String expression;

    @Override
    public AnyTypeClass getAnyTypeClass() {
        return anyTypeClass;
    }

    @Override
    public void setAnyTypeClass(final AnyTypeClass anyTypeClass) {
        checkType(anyTypeClass, JPAAnyTypeClass.class);
        this.anyTypeClass = (JPAAnyTypeClass) anyTypeClass;
    }

    @Override
    public String getExpression() {
        return expression;
    }

    @Override
    public void setExpression(final String expression) {
        this.expression = expression;
    }

    @Override
    public AttrSchemaType getType() {
        return AttrSchemaType.String;
    }

    @Override
    public String getMandatoryCondition() {
        return Boolean.FALSE.toString().toLowerCase();
    }

    @Override
    public boolean isMultivalue() {
        return Boolean.TRUE;
    }

    @Override
    public boolean isUniqueConstraint() {
        return Boolean.FALSE;
    }

    @Override
    public boolean isReadonly() {
        return Boolean.FALSE;
    }
}
