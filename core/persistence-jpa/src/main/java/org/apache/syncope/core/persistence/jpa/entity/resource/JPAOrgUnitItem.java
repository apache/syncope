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
package org.apache.syncope.core.persistence.jpa.entity.resource;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Cacheable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnitItem;

@Entity
@Table(name = JPAOrgUnitItem.TABLE)
@Cacheable
public class JPAOrgUnitItem extends AbstractItem implements OrgUnitItem {

    private static final long serialVersionUID = 7872073846646341777L;

    public static final String TABLE = "OrgUnitItem";

    @ManyToOne
    private JPAOrgUnit orgUnit;

    /**
     * (Optional) classes for MappingItem transformation.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "transformerClassName")
    @CollectionTable(name = TABLE + "_Transformer",
            joinColumns =
            @JoinColumn(name = "orgUnitItem_id", referencedColumnName = "id"))
    private List<String> transformerClassNames = new ArrayList<>();

    @Override
    public OrgUnit getOrgUnit() {
        return orgUnit;
    }

    @Override
    public void setOrgUnit(final OrgUnit mapping) {
        checkType(mapping, JPAOrgUnit.class);
        this.orgUnit = (JPAOrgUnit) mapping;
    }

    @Override
    public List<String> getTransformerClassNames() {
        return transformerClassNames;
    }
}
