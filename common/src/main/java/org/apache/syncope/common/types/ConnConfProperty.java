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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.syncope.common.AbstractBaseBean;

@XmlRootElement
@XmlType
public class ConnConfProperty extends AbstractBaseBean implements Comparable<ConnConfProperty> {

    private static final long serialVersionUID = -8391413960221862238L;

    private ConnConfPropSchema schema;

    private List<?> values = new ArrayList<Object>();

    private boolean overridable;

    public ConnConfPropSchema getSchema() {
        return schema;
    }

    public void setSchema(final ConnConfPropSchema schema) {
        this.schema = schema;
    }

    @XmlElementWrapper(name = "values")
    @XmlElement(name = "value")
    public List<?> getValues() {
        return values;
    }

    public void setValues(final List<?> values) {
        this.values = values;
    }

    public boolean isOverridable() {
        return overridable;
    }

    public void setOverridable(final boolean overridable) {
        this.overridable = overridable;
    }

    @Override
    public int compareTo(final ConnConfProperty connConfProperty) {
        return this.getSchema().compareTo(connConfProperty.getSchema());
    }
}
