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
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;

@Entity
@Table(name = JPAMappingItem.TABLE)
@Cacheable
public class JPAMappingItem extends AbstractGeneratedKeyEntity implements MappingItem {

    private static final long serialVersionUID = 7383601853619332424L;

    public static final String TABLE = "MappingItem";

    @ManyToOne
    private JPAMapping mapping;

    @NotNull
    private String intAttrName;

    /**
     * Target resource's field to be mapped.
     */
    @NotNull
    private String extAttrName;

    /**
     * Specify if the mapped target resource's field is nullable.
     */
    @NotNull
    private String mandatoryCondition;

    /**
     * Specify if the mapped target resource's field is the id.
     */
    @NotNull
    @Basic
    @Min(0)
    @Max(1)
    private Integer connObjectKey;

    /**
     * Specify if the mapped target resource's field is the password.
     */
    @NotNull
    @Basic
    @Min(0)
    @Max(1)
    private Integer password;

    @NotNull
    @Enumerated(EnumType.STRING)
    private MappingPurpose purpose;

    /**
     * (Optional) JEXL expression to apply to values before propagation.
     */
    @Column(name = "propJEXL")
    private String propagationJEXLTransformer;

    /**
     * (Optional) JEXL expression to apply to values before pull.
     */
    @Column(name = "pullJEXL")
    private String pullJEXLTransformer;

    /**
     * (Optional) classes for MappingItem transformation.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "transformerClassName")
    @CollectionTable(name = "MappingItem_Transformer",
            joinColumns =
            @JoinColumn(name = "mappingItem_id", referencedColumnName = "id"))
    private List<String> mappingItemTransformerClassNames = new ArrayList<>();

    public JPAMappingItem() {
        super();

        mandatoryCondition = Boolean.FALSE.toString();

        connObjectKey = getBooleanAsInteger(false);
        password = getBooleanAsInteger(false);
    }

    @Override
    public Mapping getMapping() {
        return mapping;
    }

    @Override
    public void setMapping(final Mapping mapping) {
        checkType(mapping, JPAMapping.class);
        this.mapping = (JPAMapping) mapping;
    }

    @Override
    public String getExtAttrName() {
        return extAttrName;
    }

    @Override
    public void setExtAttrName(final String extAttrName) {
        this.extAttrName = extAttrName;
    }

    @Override
    public String getMandatoryCondition() {
        return mandatoryCondition;
    }

    @Override
    public void setMandatoryCondition(final String mandatoryCondition) {
        this.mandatoryCondition = mandatoryCondition;
    }

    @Override
    public String getIntAttrName() {
        return intAttrName;
    }

    @Override
    public void setIntAttrName(final String intAttrName) {
        this.intAttrName = intAttrName;
    }

    @Override
    public boolean isConnObjectKey() {
        return isBooleanAsInteger(connObjectKey);
    }

    @Override
    public void setConnObjectKey(final boolean connObjectKey) {
        this.connObjectKey = getBooleanAsInteger(connObjectKey);
    }

    @Override
    public boolean isPassword() {
        return isBooleanAsInteger(password);
    }

    @Override
    public void setPassword(final boolean password) {
        this.password = getBooleanAsInteger(password);
    }

    @Override
    public MappingPurpose getPurpose() {
        return purpose;
    }

    @Override
    public void setPurpose(final MappingPurpose purpose) {
        this.purpose = purpose;
    }

    @Override
    public String getPropagationJEXLTransformer() {
        return propagationJEXLTransformer;
    }

    @Override
    public void setPropagationJEXLTransformer(final String propagationJEXLTransformer) {
        this.propagationJEXLTransformer = propagationJEXLTransformer;
    }

    @Override
    public String getPullJEXLTransformer() {
        return pullJEXLTransformer;
    }

    @Override
    public void setPullJEXLTransformer(final String pullJEXLTransformer) {
        this.pullJEXLTransformer = pullJEXLTransformer;
    }

    @Override
    public List<String> getMappingItemTransformerClassNames() {
        return mappingItemTransformerClassNames;
    }

}
