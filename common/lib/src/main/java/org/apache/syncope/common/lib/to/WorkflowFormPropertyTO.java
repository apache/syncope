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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.jaxb.XmlGenericMapAdapter;
import org.apache.syncope.common.lib.types.WorkflowFormPropertyType;

@XmlRootElement(name = "workflowFormProperty")
@XmlType
public class WorkflowFormPropertyTO extends AbstractBaseBean {

    private static final long serialVersionUID = 9139969592634304261L;

    private String id;

    private String name;

    private WorkflowFormPropertyType type;

    private String value;

    private boolean readable;

    private boolean writable;

    private boolean required;

    private String datePattern;

    @XmlJavaTypeAdapter(XmlGenericMapAdapter.class)
    @JsonIgnore
    private final Map<String, String> enumValues = new HashMap<>();

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isReadable() {
        return readable;
    }

    public void setReadable(final boolean readable) {
        this.readable = readable;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(final boolean required) {
        this.required = required;
    }

    public WorkflowFormPropertyType getType() {
        return type;
    }

    public void setType(final WorkflowFormPropertyType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public boolean isWritable() {
        return writable;
    }

    public void setWritable(final boolean writable) {
        this.writable = writable;
    }

    public String getDatePattern() {
        return datePattern;
    }

    public void setDatePattern(final String datePattern) {
        this.datePattern = datePattern;
    }

    @JsonProperty
    public Map<String, String> getEnumValues() {
        return enumValues;
    }
}
