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
package org.apache.syncope.core.persistence.api.entity.task;

import java.util.Map;
import org.apache.syncope.common.lib.form.FormPropertyType;
import org.apache.syncope.core.persistence.api.entity.ProvidedKeyEntity;

public interface FormPropertyDef extends ProvidedKeyEntity {

    MacroTask getMacroTask();

    void setMacroTask(MacroTask macroTask);

    String getName();

    void setName(String name);

    FormPropertyType getType();

    void setType(FormPropertyType type);

    boolean isReadable();

    void setReadable(boolean readable);

    boolean isWritable();

    void setWritable(boolean writable);

    boolean isRequired();

    void setRequired(boolean required);

    String getDatePattern();

    void setDatePattern(String datePattern);

    Map<String, String> getEnumValues();

    void setEnumValues(Map<String, String> enumValues);
}
