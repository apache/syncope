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
package org.syncope.client.to;

import org.syncope.client.AbstractBaseBean;
import org.syncope.types.SourceMappingType;

public class SchemaMappingTO extends AbstractBaseBean {

    private Long id;

    /**
     * Attribute schema to be mapped.
     * Consider that we can associate tha same attribute schema more
     * than once, with different aliases, to different resource attributes.
     */
    private String sourceAttrName;

    /**
     * Schema type to be mapped.
     */
    private SourceMappingType sourceMappingType;

    /**
     * Target resource's field to be mapped.
     */
    private String destAttrName;

    /**
     * Specify if the mapped target resource's field is the key.
     */
    private boolean accountid;

    /**
     * Specify if the mapped target resource's field is the password.
     */
    private boolean password;

    /**
     * Specify if the mapped target resource's field is nullable.
     */
    private String mandatoryCondition = "false";

    public boolean isAccountid() {
        return accountid;
    }

    public void setAccountid(boolean accountid) {
        this.accountid = accountid;
    }

    public String getDestAttrName() {
        return destAttrName;
    }

    public void setDestAttrName(String destAttrName) {
        this.destAttrName = destAttrName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMandatoryCondition() {
        return mandatoryCondition;
    }

    public void setMandatoryCondition(String mandatoryCondition) {
        this.mandatoryCondition = mandatoryCondition;
    }

    public boolean isPassword() {
        return password;
    }

    public void setPassword(boolean password) {
        this.password = password;
    }

    public String getSourceAttrName() {
        return sourceAttrName;
    }

    public void setSourceAttrName(String sourceAttrName) {
        this.sourceAttrName = sourceAttrName;
    }

    public SourceMappingType getSourceMappingType() {
        return sourceMappingType;
    }

    public void setSourceMappingType(SourceMappingType sourceMappingType) {
        this.sourceMappingType = sourceMappingType;
    }
}
