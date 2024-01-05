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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIProvider;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIUserTemplate;

@Entity
@Table(name = JPAOIDCC4UIUserTemplate.TABLE, uniqueConstraints =
        @UniqueConstraint(columnNames = { "op_id" }))
public class JPAOIDCC4UIUserTemplate extends AbstractAnyTemplate implements OIDCC4UIUserTemplate {

    public static final String TABLE = "OIDCUserTemplate";

    private static final long serialVersionUID = 3964321047520954968L;

    @ManyToOne
    private JPAOIDCC4UIProvider op;

    @Override
    public OIDCC4UIProvider getOP() {
        return op;
    }

    @Override
    public void setOP(final OIDCC4UIProvider op) {
        checkType(op, JPAOIDCC4UIProvider.class);
        this.op = (JPAOIDCC4UIProvider) op;
    }
}
