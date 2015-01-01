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
package org.apache.syncope.persistence.api.entity;

import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;

public interface MappingItem extends Entity<Long> {

    String getExtAttrName();

    String getIntAttrName();

    IntMappingType getIntMappingType();

    String getMandatoryCondition();

    Mapping<?> getMapping();

    MappingPurpose getPurpose();

    boolean isAccountid();

    boolean isPassword();

    void setAccountid(boolean accountid);

    void setExtAttrName(String extAttrName);

    void setIntAttrName(String intAttrName);

    void setIntMappingType(IntMappingType intMappingType);

    void setMandatoryCondition(String condition);

    void setMapping(Mapping<?> mapping);

    void setPassword(boolean password);

    void setPurpose(MappingPurpose purpose);
}
