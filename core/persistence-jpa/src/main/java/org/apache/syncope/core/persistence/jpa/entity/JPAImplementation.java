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

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.common.validation.ImplementationCheck;

@Entity
@Table(name = JPAImplementation.TABLE)
@ImplementationCheck
@Cacheable
public class JPAImplementation extends AbstractProvidedKeyEntity implements Implementation {

    public static final String TABLE = "Implementation";

    private static final long serialVersionUID = 8700713975100295322L;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ImplementationEngine engine;

    @Column(nullable = false)
    private String type;

    @Lob
    private String body;

    @Override
    public ImplementationEngine getEngine() {
        return engine;
    }

    @Override
    public void setEngine(final ImplementationEngine engine) {
        this.engine = engine;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(final String type) {
        this.type = type;
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public void setBody(final String body) {
        this.body = body;
    }
}
