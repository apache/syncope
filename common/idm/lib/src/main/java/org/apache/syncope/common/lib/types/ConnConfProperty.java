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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.BaseBean;

public class ConnConfProperty implements BaseBean {

    private static final long serialVersionUID = -8391413960221862238L;

    private ConnConfPropSchema schema;

    private final List<Object> values = new ArrayList<>();

    private boolean overridable;

    public ConnConfPropSchema getSchema() {
        return schema;
    }

    public void setSchema(final ConnConfPropSchema schema) {
        this.schema = schema;
    }

    @JacksonXmlElementWrapper(localName = "values")
    @JacksonXmlProperty(localName = "value")
    public List<Object> getValues() {
        return values;
    }

    public boolean isOverridable() {
        return overridable;
    }

    public void setOverridable(final boolean overridable) {
        this.overridable = overridable;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(schema).
                append(values).
                append(overridable).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ConnConfProperty other = (ConnConfProperty) obj;
        return new EqualsBuilder().
                append(schema, other.schema).
                append(values, other.values).
                append(overridable, other.overridable).
                build();
    }
}
