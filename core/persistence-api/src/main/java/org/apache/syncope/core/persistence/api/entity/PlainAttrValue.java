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

import java.time.OffsetDateTime;
import org.apache.syncope.common.lib.types.AttrSchemaType;

public interface PlainAttrValue extends Entity {

    PlainAttr<?> getAttr();

    byte[] getBinaryValue();

    Boolean getBooleanValue();

    OffsetDateTime getDateValue();

    Double getDoubleValue();

    Long getLongValue();

    String getStringValue();

    <V> V getValue();

    String getValueAsString();

    String getValueAsString(AttrSchemaType type);

    String getValueAsString(PlainSchema schema);

    void parseValue(PlainSchema schema, String value);

    void setAttr(PlainAttr<?> attr);

    void setBinaryValue(byte[] binaryValue);

    void setBooleanValue(Boolean booleanValue);

    void setDateValue(OffsetDateTime dateValue);

    void setDoubleValue(Double doubleValue);

    void setLongValue(Long longValue);

    void setStringValue(String stringValue);
}
