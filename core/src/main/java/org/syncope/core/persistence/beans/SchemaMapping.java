/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.beans;

import static javax.persistence.EnumType.STRING;

import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.syncope.core.persistence.validation.entity.SchemaMappingCheck;
import org.syncope.types.IntMappingType;

@Entity
@Cacheable
@SchemaMappingCheck
public class SchemaMapping extends AbstractBaseBean {

    private static final long serialVersionUID = 7383601853619332424L;

    @Id
    private Long id;

    @Column(nullable = true)
    private String intAttrName;

    @Column(nullable = false)
    @Enumerated(STRING)
    private IntMappingType intMappingType;

    /**
     * Resource that has fields to be mapped over user attribute schemas.
     */
    @ManyToOne
    @NotNull
    private ExternalResource resource;

    /**
     * Target resource's field to be mapped.
     */
    @Column(nullable = true)
    private String extAttrName;

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
     * Specify if the mapped target resource's field is nullable.
     */
    @Column(nullable = false)
    private String mandatoryCondition;

    public SchemaMapping() {
        super();

        accountid = getBooleanAsInteger(false);
        password = getBooleanAsInteger(false);
        mandatoryCondition = Boolean.FALSE.toString();
    }

    public Long getId() {
        return id;
    }

    public boolean isAccountid() {
        return isBooleanAsInteger(accountid);
    }

    public void setAccountid(boolean accountid) {
        this.accountid = getBooleanAsInteger(accountid);
    }

    public String getExtAttrName() {
        return extAttrName;
    }

    public void setExtAttrName(String extAttrName) {
        this.extAttrName = extAttrName;
    }

    public String getMandatoryCondition() {
        return mandatoryCondition;
    }

    public void setMandatoryCondition(String mandatoryCondition) {
        this.mandatoryCondition = mandatoryCondition;
    }

    public boolean isPassword() {
        return isBooleanAsInteger(password);
    }

    public void setPassword(boolean password) {
        this.password = getBooleanAsInteger(password);
    }

    public ExternalResource getResource() {
        return resource;
    }

    public void setResource(ExternalResource resource) {
        this.resource = resource;
    }

    public String getIntAttrName() {
        return intAttrName;
    }

    public void setIntAttrName(String intAttrName) {
        this.intAttrName = intAttrName;
    }

    public IntMappingType getIntMappingType() {
        return intMappingType;
    }

    public void setIntMappingType(IntMappingType intMappingType) {
        this.intMappingType = intMappingType;
    }
}
