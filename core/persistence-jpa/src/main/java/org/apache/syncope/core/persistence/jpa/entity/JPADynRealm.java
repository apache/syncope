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

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.jpa.validation.entity.DynRealmCheck;

@Entity
@Table(name = JPADynRealm.TABLE)
@Cacheable
@DynRealmCheck
public class JPADynRealm extends AbstractProvidedKeyEntity implements DynRealm {

    private static final long serialVersionUID = -6851035842423560341L;

    public static final String TABLE = "DynRealm";

    @NotNull
    private String fiql;

    @Override
    public String getFIQLCond() {
        return fiql;
    }

    @Override
    public void setFIQLCond(final String fiql) {
        this.fiql = fiql;
    }
}
