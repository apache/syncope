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
package org.syncope.client.to;

import org.syncope.client.AbstractBaseBean;
import org.syncope.types.IntMappingType;

public class SchemaMappingTO extends AbstractBaseBean {

    private Long id;

    /**
     * Attribute schema to be mapped.
     * Consider that we can associate tha same attribute schema more
     * than once, with different aliases, to different resource attributes.
     */
    private String intAttrName;

    /**
     * Schema type to be mapped.
     */
    private IntMappingType intMappingType;

    /**
     * External resource's field to be mapped.
     */
    private String extAttrName;

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

    public String getExtAttrName() {
        return extAttrName;
    }

    public void setExtAttrName(String extAttrName) {
        this.extAttrName = extAttrName;
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
