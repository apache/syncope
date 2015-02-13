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
package org.apache.syncope.core.persistence.jpa.entity.role;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.role.RPlainAttr;
import org.apache.syncope.core.persistence.api.entity.role.RPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttrValue;

@Entity
@Table(name = JPARPlainAttrValue.TABLE)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class JPARPlainAttrValue extends AbstractPlainAttrValue implements RPlainAttrValue {

    private static final long serialVersionUID = -766808291128424707L;

    public static final String TABLE = "RPlainAttrValue";

    @Id
    private Long id;

    @ManyToOne
    @NotNull
    private JPARPlainAttr attribute;

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
}
