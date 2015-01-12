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
package org.apache.syncope.server.persistence.jpa.entity.role;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.syncope.server.persistence.api.entity.Mapping;
import org.apache.syncope.server.persistence.api.entity.role.RMappingItem;
import org.apache.syncope.server.persistence.jpa.entity.AbstractMappingItem;

@Entity
@Table(name = JPARMappingItem.TABLE)
public class JPARMappingItem extends AbstractMappingItem implements RMappingItem {

    public static final String TABLE = "RMappingItem";

    private static final long serialVersionUID = -2670787666933476166L;

    @Id
    private Long id;

    @ManyToOne
    private JPARMapping mapping;

    @Override
    public Long getKey() {
        return id;
    }

    @Override
    public Mapping<RMappingItem> getMapping() {
        return mapping;
    }

    @Override
    public void setMapping(final Mapping<?> mapping) {
        checkType(mapping, JPARMapping.class);
        this.mapping = (JPARMapping) mapping;
    }
}
