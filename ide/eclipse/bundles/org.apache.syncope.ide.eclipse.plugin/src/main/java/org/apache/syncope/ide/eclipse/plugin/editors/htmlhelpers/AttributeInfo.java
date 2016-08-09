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
package org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers;

import java.util.ArrayList;
import java.util.List;

public class AttributeInfo {

    private String attributeName;
    private boolean hasValue;
    private boolean required = false;
    private String description;
    private List<String> values = new ArrayList<String>();
    public enum AttributeTypeOptions {
        NONE, ALIGN, VALIGN, INPUT_TYPE, CSS, FILE, ID, IDREF, IDREFS, TARGET
    }
    private AttributeTypeOptions attributeType;

    public AttributeInfo(final String attributeName, final boolean hasValue) {
        this(attributeName, hasValue, AttributeTypeOptions.NONE);
    }

    public AttributeInfo(final String attributeName, final boolean hasValue,
            final AttributeTypeOptions attributeType) {
        this(attributeName, hasValue, attributeType, false);
    }

    public AttributeInfo(final String attributeName, final boolean hasValue,
            final AttributeTypeOptions attributeType, final boolean required) {
        this.attributeName = attributeName;
        this.hasValue      = hasValue;
        this.attributeType = attributeType;
        this.required      = required;
    }

    public AttributeTypeOptions getAttributeType() {
        return this.attributeType;
    }

    public void setAttributeType(final AttributeTypeOptions type) {
        this.attributeType = type;
    }

    public String getAttributeName() {
        return this.attributeName;
    }

    public boolean hasValue() {
        return this.hasValue;
    }

    public boolean isRequired() {
        return this.required;
    }

    public void addValue(final String value) {
        this.values.add(value);
    }

    public String[] getValues() {
        return this.values.toArray(new String[this.values.size()]);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }
}
