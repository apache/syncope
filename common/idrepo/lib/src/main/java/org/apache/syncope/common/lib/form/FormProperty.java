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
package org.apache.syncope.common.lib.form;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class FormProperty implements Serializable {

    private static final long serialVersionUID = 9139969592634304261L;

    private String id;

    private String name;

    private FormPropertyType type;

    private boolean readable;

    private boolean writable;

    private boolean required;

    private Pattern stringRegEx;

    private String datePattern;

    private final List<FormPropertyValue> enumValues = new ArrayList<>();

    private final List<FormPropertyValue> dropdownValues = new ArrayList<>();

    private boolean dropdownSingleSelection = true;

    private boolean dropdownFreeForm;

    private String value;

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

    public Pattern getStringRegEx() {
        return stringRegEx;
    }

    public void setStringRegEx(final Pattern stringRegEx) {
        this.stringRegEx = stringRegEx;
    }

    public FormPropertyType getType() {
        return type;
    }

    public void setType(final FormPropertyType type) {
        this.type = type;
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

    @JacksonXmlElementWrapper(localName = "enumValues")
    @JacksonXmlProperty(localName = "enumValue")
    public List<FormPropertyValue> getEnumValues() {
        return enumValues;
    }

    @JacksonXmlElementWrapper(localName = "dropdownValues")
    @JacksonXmlProperty(localName = "dropdownValue")
    public List<FormPropertyValue> getDropdownValues() {
        return dropdownValues;
    }

    public boolean isDropdownSingleSelection() {
        return dropdownSingleSelection;
    }

    public void setDropdownSingleSelection(final boolean dropdownSingleSelection) {
        this.dropdownSingleSelection = dropdownSingleSelection;
    }

    public boolean isDropdownFreeForm() {
        return dropdownFreeForm;
    }

    public void setDropdownFreeForm(final boolean dropdownFreeForm) {
        this.dropdownFreeForm = dropdownFreeForm;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(id).
                append(name).
                append(type).
                append(readable).
                append(writable).
                append(required).
                append(stringRegEx).
                append(datePattern).
                append(enumValues).
                append(dropdownValues).
                append(dropdownSingleSelection).
                append(dropdownFreeForm).
                append(value).
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
        FormProperty other = (FormProperty) obj;
        return new EqualsBuilder().
                append(id, other.id).
                append(name, other.name).
                append(type, other.type).
                append(readable, other.readable).
                append(writable, other.writable).
                append(required, other.required).
                append(stringRegEx, other.stringRegEx).
                append(datePattern, other.datePattern).
                append(enumValues, other.enumValues).
                append(dropdownValues, other.dropdownValues).
                append(dropdownSingleSelection, other.dropdownSingleSelection).
                append(dropdownFreeForm, other.dropdownFreeForm).
                append(value, other.value).
                build();
    }
}
