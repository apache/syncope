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

import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;

public enum SchemaType {

    /**
     * Standard schema for normal attributes to be stored within syncope.
     */
    PLAIN(PlainSchemaTO.class),
    /**
     * Derived schema calculated based on other attributes.
     */
    DERIVED(DerSchemaTO.class),
    /**
     * Virtual schema for attributes fetched from remote resources only.
     */
    VIRTUAL(VirSchemaTO.class);

    private final Class<? extends SchemaTO> toClass;

    SchemaType(final Class<? extends SchemaTO> toClass) {
        this.toClass = toClass;
    }

    public Class<? extends SchemaTO> getToClass() {
        return toClass;
    }

    public static SchemaType fromToClass(final Class<? extends SchemaTO> toClass) {
        SchemaType schemaType = null;

        if (PlainSchemaTO.class.equals(toClass)) {
            schemaType = SchemaType.PLAIN;
        } else if (DerSchemaTO.class.equals(toClass)) {
            schemaType = SchemaType.DERIVED;
        } else if (VirSchemaTO.class.equals(toClass)) {
            schemaType = SchemaType.VIRTUAL;
        } else {
            throw new IllegalArgumentException("Unexpected class: " + toClass.getName());
        }

        return schemaType;
    }
}
