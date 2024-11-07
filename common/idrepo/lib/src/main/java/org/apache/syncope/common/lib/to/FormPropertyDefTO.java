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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.form.FormPropertyType;

public class FormPropertyDefTO implements NamedEntityTO {

    private static final long serialVersionUID = -6862109424380661428L;

    private String key;

    private String name;

    private final Map<Locale, String> labels = new HashMap<>();

    private FormPropertyType type;

    private boolean readable = true;

    private boolean writable = true;

    private boolean required;

    private Pattern stringRegEx;

    private String datePattern;

    private final Map<String, String> enumValues = new LinkedHashMap<>();

    private boolean dropdownSingleSelection = true;

    private boolean dropdownFreeForm;

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @JsonIgnore
    public String getLabel(final Locale locale) {
        return labels.getOrDefault(locale, key);
    }

    public Map<Locale, String> getLabels() {
        return labels;
    }

    public FormPropertyType getType() {
        return type;
    }

    public void setType(final FormPropertyType type) {
        this.type = type;
    }

    public boolean isReadable() {
        return readable;
    }

    public void setReadable(final boolean readable) {
        this.readable = readable;
    }

    public boolean isWritable() {
        return writable;
    }

    public void setWritable(final boolean writable) {
        this.writable = writable;
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

    public String getDatePattern() {
        return datePattern;
    }

    public void setDatePattern(final String datePattern) {
        this.datePattern = datePattern;
    }

    public Map<String, String> getEnumValues() {
        return enumValues;
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

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(name).
                append(labels).
                append(type).
                append(readable).
                append(writable).
                append(required).
                append(stringRegEx).
                append(datePattern).
                append(enumValues).
                append(dropdownSingleSelection).
                append(dropdownFreeForm).
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
        FormPropertyDefTO other = (FormPropertyDefTO) obj;
        return new EqualsBuilder().
                append(key, other.key).
                append(name, other.name).
                append(labels, other.labels).
                append(type, other.type).
                append(readable, other.readable).
                append(writable, other.writable).
                append(required, other.required).
                append(stringRegEx, other.stringRegEx).
                append(datePattern, other.datePattern).
                append(enumValues, other.enumValues).
                append(dropdownSingleSelection, other.dropdownSingleSelection).
                append(dropdownFreeForm, other.dropdownFreeForm).
                build();
    }
}
