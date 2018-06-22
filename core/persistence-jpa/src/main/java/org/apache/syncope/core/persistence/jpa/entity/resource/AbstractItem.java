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

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.entity.resource.Item;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;

@MappedSuperclass
public abstract class AbstractItem extends AbstractGeneratedKeyEntity implements Item {

    private static final long serialVersionUID = 5552380143129988272L;

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
    private String mandatoryCondition = Boolean.FALSE.toString();

    /**
     * Specify if the mapped target resource's field is the id.
     */
    @NotNull
    private Boolean connObjectKey = false;

    /**
     * Specify if the mapped target resource's field is the password.
     */
    @NotNull
    private Boolean password = false;

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
        return connObjectKey;
    }

    @Override
    public void setConnObjectKey(final boolean connObjectKey) {
        this.connObjectKey = connObjectKey;
    }

    @Override
    public boolean isPassword() {
        return password;
    }

    @Override
    public void setPassword(final boolean password) {
        this.password = password;
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

}
