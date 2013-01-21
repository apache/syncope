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
package org.apache.syncope.core.persistence.beans;

import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.apache.syncope.common.types.IntMappingType;

@MappedSuperclass
@Cacheable
public abstract class AbstractMappingItem extends AbstractBaseBean {

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

    public AbstractMappingItem() {
        super();

        mandatoryCondition = Boolean.FALSE.toString();

        accountid = getBooleanAsInteger(false);
        password = getBooleanAsInteger(false);
    }

    public abstract Long getId();

    public abstract <T extends AbstractMapping> T getMapping();

    public abstract <T extends AbstractMapping> void setMapping(T mapping);

    public String getExtAttrName() {
        return extAttrName;
    }

    public void setExtAttrName(final String extAttrName) {
        this.extAttrName = extAttrName;
    }

    public String getMandatoryCondition() {
        return mandatoryCondition;
    }

    public void setMandatoryCondition(final String mandatoryCondition) {
        this.mandatoryCondition = mandatoryCondition;
    }

    public String getIntAttrName() {
        final String name;

        switch (getIntMappingType()) {
            case UserId:
            case RoleId:
            case MembershipId:
                name = "id";
                break;

            case Username:
                name = "username";
                break;

            case RoleName:
                name = "roleName";
                break;

            case Password:
                name = "password";
                break;

            default:
                name = intAttrName;
        }

        return name;
    }

    public void setIntAttrName(final String intAttrName) {
        this.intAttrName = intAttrName;
    }

    public IntMappingType getIntMappingType() {
        return intMappingType;
    }

    public void setIntMappingType(IntMappingType intMappingType) {
        this.intMappingType = intMappingType;
    }

    public boolean isAccountid() {
        return isBooleanAsInteger(accountid);
    }

    public void setAccountid(boolean accountid) {
        this.accountid = getBooleanAsInteger(accountid);
    }

    public boolean isPassword() {
        return isBooleanAsInteger(password);
    }

    public void setPassword(boolean password) {
        this.password = getBooleanAsInteger(password);
    }
}
