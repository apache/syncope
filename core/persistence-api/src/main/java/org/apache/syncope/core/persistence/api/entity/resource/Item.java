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
package org.apache.syncope.core.persistence.api.entity.resource;

import java.util.List;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.entity.Entity;

public interface Item extends Entity {

    String getExtAttrName();

    void setExtAttrName(String extAttrName);

    String getIntAttrName();

    void setIntAttrName(String intAttrName);

    String getMandatoryCondition();

    void setMandatoryCondition(String condition);

    MappingPurpose getPurpose();

    void setPurpose(MappingPurpose purpose);

    boolean isConnObjectKey();

    void setConnObjectKey(boolean connObjectKey);

    boolean isPassword();

    void setPassword(boolean password);

    String getPropagationJEXLTransformer();

    void setPropagationJEXLTransformer(String propagationJEXLTransformer);

    String getPullJEXLTransformer();

    void setPullJEXLTransformer(String pullJEXLTransformer);

    List<String> getTransformerClassNames();
}
