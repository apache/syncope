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
package org.apache.syncope.server.persistence.jpa.entity.user;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.syncope.server.persistence.api.entity.Mapping;
import org.apache.syncope.server.persistence.api.entity.user.UMappingItem;
import org.apache.syncope.server.persistence.jpa.entity.AbstractMappingItem;

@Entity
@Table(name = JPAUMappingItem.TABLE)
public class JPAUMappingItem extends AbstractMappingItem implements UMappingItem {

    private static final long serialVersionUID = 2936446317887310833L;

    public static final String TABLE = "UMappingItem";

    @Id
    private Long id;

    @ManyToOne
    private JPAUMapping mapping;

    @Override
    public Long getKey() {
        return id;
    }

    @Override
    public Mapping<UMappingItem> getMapping() {
        return mapping;
    }

    @Override
    public void setMapping(final Mapping<?> mapping) {
        checkType(mapping, JPAUMapping.class);
        this.mapping = (JPAUMapping) mapping;
    }
}
