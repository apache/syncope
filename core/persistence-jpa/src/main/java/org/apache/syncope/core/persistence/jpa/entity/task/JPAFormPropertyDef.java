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
package org.apache.syncope.core.persistence.jpa.entity.task;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.syncope.common.lib.form.FormPropertyType;
import org.apache.syncope.core.persistence.api.entity.task.FormPropertyDef;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
import org.apache.syncope.core.persistence.common.validation.FormPropertyDefCheck;
import org.apache.syncope.core.persistence.jpa.converters.Locale2StringMapConverter;
import org.apache.syncope.core.persistence.jpa.converters.String2StringMapConverter;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;

@Entity
@Table(name = JPAFormPropertyDef.TABLE)
@FormPropertyDefCheck
public class JPAFormPropertyDef extends AbstractGeneratedKeyEntity implements FormPropertyDef {

    private static final long serialVersionUID = -5839990371546587373L;

    public static final String TABLE = "FormPropertyDef";

    private int idx;

    @ManyToOne(optional = false)
    private JPAMacroTask macroTask;

    @NotNull
    private String name;

    @Convert(converter = Locale2StringMapConverter.class)
    @Lob
    private Map<Locale, String> labels = new HashMap<>();

    @NotNull
    @Enumerated(EnumType.STRING)
    private FormPropertyType type;

    @NotNull
    private Boolean readable = Boolean.TRUE;

    @NotNull
    private Boolean writable = Boolean.TRUE;

    @NotNull
    private Boolean required = Boolean.FALSE;

    private String stringRegEx;

    private String datePattern;

    @Convert(converter = String2StringMapConverter.class)
    @Lob
    private Map<String, String> enumValues = new HashMap<>();

    @NotNull
    private Boolean dropdownSingleSelection = Boolean.TRUE;

    @NotNull
    private Boolean dropdownFreeForm = Boolean.FALSE;

    private String mimeType;

    public void setIdx(final int idx) {
        this.idx = idx;
    }

    @Override
    public JPAMacroTask getMacroTask() {
        return macroTask;
    }

    @Override
    public void setMacroTask(final MacroTask macroTask) {
        checkType(macroTask, JPAMacroTask.class);
        this.macroTask = (JPAMacroTask) macroTask;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public Optional<String> getLabel(final Locale locale) {
        return Optional.ofNullable(labels.get(locale));
    }

    @Override
    public Map<Locale, String> getLabels() {
        return labels;
    }

    @Override
    public FormPropertyType getType() {
        return type;
    }

    @Override
    public void setType(final FormPropertyType type) {
        this.type = type;
    }

    @Override
    public boolean isReadable() {
        return readable == null ? true : readable;
    }

    @Override
    public void setReadable(final boolean readable) {
        this.readable = readable;
    }

    @Override
    public boolean isWritable() {
        return writable == null ? true : writable;
    }

    @Override
    public void setWritable(final boolean writable) {
        this.writable = writable;
    }

    @Override
    public boolean isRequired() {
        return required == null ? false : required;
    }

    @Override
    public void setRequired(final boolean required) {
        this.required = required;
    }

    @Override
    public Pattern getStringRegEx() {
        return Optional.ofNullable(stringRegEx).map(Pattern::compile).orElse(null);
    }

    @Override
    public void setStringRegEx(final Pattern stringRegEx) {
        this.stringRegEx = Optional.ofNullable(stringRegEx).map(Pattern::pattern).orElse(null);
    }

    @Override
    public String getDatePattern() {
        return datePattern;
    }

    @Override
    public void setDatePattern(final String datePattern) {
        this.datePattern = datePattern;
    }

    @Override
    public Map<String, String> getEnumValues() {
        return enumValues;
    }

    @Override
    public void setEnumValues(final Map<String, String> enumValues) {
        this.enumValues = enumValues;
    }

    @Override
    public boolean isDropdownSingleSelection() {
        return dropdownSingleSelection == null ? false : dropdownSingleSelection;
    }

    @Override
    public void setDropdownSingleSelection(final boolean dropdownSingleSelection) {
        this.dropdownSingleSelection = dropdownSingleSelection;
    }

    @Override
    public boolean isDropdownFreeForm() {
        return dropdownFreeForm == null ? false : dropdownFreeForm;
    }

    @Override
    public void setDropdownFreeForm(final boolean dropdownFreeForm) {
        this.dropdownFreeForm = dropdownFreeForm;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }
}
