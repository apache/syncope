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
package org.apache.syncope.core.persistence.jpa.entity.keymaster;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.core.persistence.api.entity.keymaster.NetworkServiceEntity;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;

@Entity
@Table(name = JPANetworkService.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "type", "address" }))
public class JPANetworkService extends AbstractGeneratedKeyEntity implements NetworkServiceEntity {

    private static final long serialVersionUID = 8742750097008236475L;

    public static final String TABLE = "NetworkService";

    @NotNull
    @Enumerated(EnumType.STRING)
    private NetworkService.Type type;

    @NotNull
    private String address;

    @Override
    public NetworkService.Type getType() {
        return type;
    }

    @Override
    public void setType(final NetworkService.Type type) {
        this.type = type;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public void setAddress(final String address) {
        this.address = address;
    }
}
