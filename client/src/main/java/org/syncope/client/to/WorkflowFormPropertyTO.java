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
package org.syncope.client.to;

import java.util.HashMap;
import java.util.Map;
import org.syncope.client.AbstractBaseBean;
import org.syncope.types.WorkflowFormPropertyType;

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

    private Map<String, String> enumValues;

    public WorkflowFormPropertyTO() {
        enumValues = new HashMap<String, String>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isReadable() {
        return readable;
    }

    public void setReadable(boolean readable) {
        this.readable = readable;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public WorkflowFormPropertyType getType() {
        return type;
    }

    public void setType(WorkflowFormPropertyType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isWritable() {
        return writable;
    }

    public void setWritable(boolean writable) {
        this.writable = writable;
    }

    public String getDatePattern() {
        return datePattern;
    }

    public void setDatePattern(String datePattern) {
        this.datePattern = datePattern;
    }

    public Map<String, String> getEnumValues() {
        return enumValues;
    }

    public void putEnumValue(String key, String value) {
        this.enumValues.put(key, value);
    }

    public void removeEnumValue(String key) {
        this.enumValues.remove(key);
    }

    public void setEnumValues(Map<String, String> enumValues) {
        if (enumValues != null) {
            this.enumValues = enumValues;
        }
    }
}
