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
package org.apache.syncope.persistence.jpa.entity;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.apache.syncope.persistence.api.entity.Entitlement;

@Entity
@Table(name = JPAEntitlement.TABLE)
@Cacheable
public class JPAEntitlement extends AbstractEntity<String> implements Entitlement {

    private static final long serialVersionUID = 8044745999246422483L;

    public static final String TABLE = "Entitlement";

    @Id
    private String name;

    @Column(nullable = true)
    private String description;

    @Override
    public String getKey() {
        return name;
    }

    @Override
    public void setKey(final String key) {
        this.name = key;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }
}
