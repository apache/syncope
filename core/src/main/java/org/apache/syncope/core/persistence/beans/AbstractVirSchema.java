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
package org.apache.syncope.core.persistence.beans;

import javax.persistence.Basic;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.apache.syncope.common.types.AttributeSchemaType;

@MappedSuperclass
public abstract class AbstractVirSchema extends AbstractBaseBean {

    @Id
    private String name;
    
    @Basic
    @Min(0)
    @Max(1)
    private Integer readonly;

    public AbstractVirSchema() {
        super();

        readonly = getBooleanAsInteger(false);
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public AttributeSchemaType getType() {
        return AttributeSchemaType.String;
    }

    public String getMandatoryCondition() {
        return Boolean.FALSE.toString().toLowerCase();
    }

    public boolean isMultivalue() {
        return Boolean.TRUE;
    }

    public boolean isUniqueConstraint() {
        return Boolean.FALSE;
    }

    public boolean isReadonly() {
        return isBooleanAsInteger(readonly);
    }

    public void setReadonly(final boolean readonly) {
        this.readonly = getBooleanAsInteger(readonly);
    }

}
