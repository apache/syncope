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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.DynRealmMembership;

@Entity
@Table(name = JPADynRealmMembership.TABLE)
public class JPADynRealmMembership extends AbstractGeneratedKeyEntity implements DynRealmMembership {

    private static final long serialVersionUID = 8157856850557493134L;

    public static final String TABLE = "DynRealmMembership";

    @OneToOne
    private JPADynRealm dynRealm;

    @ManyToOne
    private JPAAnyType anyType;

    @NotNull
    private String fiql;

    @Override
    public DynRealm getDynRealm() {
        return dynRealm;
    }

    @Override
    public void setDynRealm(final DynRealm dynRealm) {
        checkType(dynRealm, JPADynRealm.class);
        this.dynRealm = (JPADynRealm) dynRealm;
    }

    @Override
    public AnyType getAnyType() {
        return anyType;
    }

    @Override
    public void setAnyType(final AnyType anyType) {
        checkType(anyType, JPAAnyType.class);
        this.anyType = (JPAAnyType) anyType;
    }

    @Override
    public String getFIQLCond() {
        return fiql;
    }

    @Override
    public void setFIQLCond(final String fiql) {
        this.fiql = fiql;
    }
}
