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
package org.apache.syncope.persistence.jpa.entity.membership;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.apache.syncope.persistence.api.entity.PlainAttr;
import org.apache.syncope.persistence.api.entity.membership.MPlainAttr;
import org.apache.syncope.persistence.api.entity.membership.MPlainAttrValue;
import org.apache.syncope.persistence.jpa.entity.AbstractPlainAttrValue;

@Entity
@Table(name = JPAMPlainAttrValue.TABLE)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class JPAMPlainAttrValue extends AbstractPlainAttrValue implements MPlainAttrValue {

    private static final long serialVersionUID = -7188881172631198385L;

    public static final String TABLE = "MPlainAttrValue";

    @Id
    private Long id;

    @ManyToOne
    @NotNull
    private JPAMPlainAttr attribute;

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
}
