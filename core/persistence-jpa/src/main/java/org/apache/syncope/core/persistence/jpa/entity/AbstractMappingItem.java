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

import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.entity.MappingItem;

@MappedSuperclass
@Cacheable
public abstract class AbstractMappingItem extends AbstractEntity<Long> implements MappingItem {

    private static final long serialVersionUID = 7383601853619332424L;

    @Column(nullable = true)
    private String intAttrName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IntMappingType intMappingType;

    /**
     * Target resource's field to be mapped.
     */
    @Column(nullable = true)
    private String extAttrName;

    /**
     * Specify if the mapped target resource's field is nullable.
     */
    @Column(nullable = false)
    private String mandatoryCondition;

    /**
     * Specify if the mapped target resource's field is the key.
     */
    @Column(nullable = false)
    @Basic
    @Min(0)
    @Max(1)
    private Integer accountid;

    /**
     * Specify if the mapped target resource's field is the password.
     */
    @Column(nullable = false)
    @Basic
    @Min(0)
    @Max(1)
    private Integer password;

    /**
     * Mapping purposes: SYNCHRONIZATION, PROPAGATION, BOTH.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MappingPurpose purpose;

    public AbstractMappingItem() {
        super();

        mandatoryCondition = Boolean.FALSE.toString();

        accountid = getBooleanAsInteger(false);
        password = getBooleanAsInteger(false);
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
        final String name;

        switch (getIntMappingType()) {
            case UserId:
            case GroupId:
            case MembershipId:
                name = "id";
                break;

            case Username:
                name = "username";
                break;

            case Password:
                name = "password";
                break;

            case GroupName:
                name = "groupName";
                break;

            case GroupOwnerSchema:
                name = "groupOwnerSchema";
                break;

            default:
                name = intAttrName;
        }

        return name;
    }

    @Override
    public void setIntAttrName(final String intAttrName) {
        this.intAttrName = intAttrName;
    }

    @Override
    public IntMappingType getIntMappingType() {
        return intMappingType;
    }

    @Override
    public void setIntMappingType(final IntMappingType intMappingType) {
        this.intMappingType = intMappingType;
    }

    @Override
    public boolean isAccountid() {
        return isBooleanAsInteger(accountid);
    }

    @Override
    public void setAccountid(final boolean accountid) {
        this.accountid = getBooleanAsInteger(accountid);
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
}
