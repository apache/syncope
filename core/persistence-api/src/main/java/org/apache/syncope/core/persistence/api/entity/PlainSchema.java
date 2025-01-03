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
package org.apache.syncope.core.persistence.api.entity;

import java.util.Map;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;

public interface PlainSchema extends Schema {

    CipherAlgorithm getCipherAlgorithm();

    void setCipherAlgorithm(CipherAlgorithm cipherAlgorithm);

    String getConversionPattern();

    void setConversionPattern(String conversionPattern);

    Map<String, String> getEnumValues();

    Implementation getDropdownValueProvider();

    void setDropdownValueProvider(Implementation dropdownValueProvider);

    String getMimeType();

    void setMimeType(String mimeType);

    String getSecretKey();

    void setSecretKey(String secretKey);

    Implementation getValidator();

    void setValidator(Implementation validator);

    void setType(AttrSchemaType type);

    void setMandatoryCondition(String condition);

    void setMultivalue(boolean multivalue);

    void setReadonly(boolean readonly);

    void setUniqueConstraint(boolean uniquevalue);
}
