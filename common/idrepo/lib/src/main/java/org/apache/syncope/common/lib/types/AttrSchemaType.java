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
package org.apache.syncope.common.lib.types;

import java.util.Date;

public enum AttrSchemaType {

    String(String.class),
    Long(Long.class),
    Double(Double.class),
    Boolean(Boolean.class),
    Date(Date.class),
    Enum(Enum.class),
    Encrypted(byte[].class),
    Binary(byte[].class);

    private final Class<?> type;

    AttrSchemaType(final Class<?> type) {
        this.type = type;
    }

    public Class<?> getType() {
        return type;
    }

    public boolean isConversionPatternNeeded() {
        return this == AttrSchemaType.Date
                || this == AttrSchemaType.Double
                || this == AttrSchemaType.Long;
    }
}
