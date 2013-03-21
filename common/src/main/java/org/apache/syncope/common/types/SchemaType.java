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
package org.apache.syncope.common.types;

import javax.xml.bind.annotation.XmlEnum;
import org.apache.syncope.common.to.AbstractSchemaTO;
import org.apache.syncope.common.to.DerivedSchemaTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.to.VirtualSchemaTO;

@XmlEnum
public enum SchemaType {

    /**
     * Standard schema for normal attributes to be stored within syncope.
     */
    NORMAL("schema", SchemaTO.class),
    /**
     * Derived schema calculated based on other attributes.
     */
    DERIVED("derivedSchema", DerivedSchemaTO.class),
    /**
     * Virtual schema for attributes fetched from remote resources only.
     */
    VIRTUAL("virtualSchema", VirtualSchemaTO.class);

    // TODO remove name once CXF migration is complete
    private final String name;

    private final Class<? extends AbstractSchemaTO> toClass;

    private SchemaType(final String name, final Class<? extends AbstractSchemaTO> toClass) {
        this.name = name;
        this.toClass = toClass;
    }

    public String toSpringURL() {
        return name;
    }

    public Class<? extends AbstractSchemaTO> getToClass() {
        return toClass;
    }
}
