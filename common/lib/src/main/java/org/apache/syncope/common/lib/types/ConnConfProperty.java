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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.syncope.common.lib.AbstractBaseBean;

@XmlRootElement
@XmlType
public class ConnConfProperty extends AbstractBaseBean implements Comparable<ConnConfProperty> {

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

    @XmlElementWrapper(name = "values")
    @XmlElement(name = "value")
    @JsonProperty("values")
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
    public int compareTo(final ConnConfProperty connConfProperty) {
        return ObjectUtils.compare(this.getSchema(), connConfProperty.getSchema());
    }
}
