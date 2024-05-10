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

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.form.FormPropertyType;
import org.apache.syncope.core.persistence.api.entity.task.FormPropertyDef;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
import org.apache.syncope.core.persistence.jpa.entity.AbstractProvidedKeyEntity;
import org.apache.syncope.core.persistence.jpa.validation.entity.FormPropertyDefCheck;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPAFormPropertyDef.TABLE)
@FormPropertyDefCheck
public class JPAFormPropertyDef extends AbstractProvidedKeyEntity implements FormPropertyDef {

    private static final long serialVersionUID = -5839990371546587373L;

    public static final String TABLE = "FormPropertyDef";

    protected static final TypeReference<Map<String, String>> TYPEREF = new TypeReference<Map<String, String>>() {
    };

    @ManyToOne(optional = false)
    private JPAMacroTask macroTask;

    @NotNull
    private String name;

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

    @Lob
    private String enumValues;

    @NotNull
    private Boolean dropdownSingleSelection = Boolean.TRUE;

    @NotNull
    private Boolean dropdownFreeForm = Boolean.FALSE;

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
    public void setStringRegExp(final Pattern stringRegEx) {
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
        return Optional.ofNullable(enumValues).map(v -> POJOHelper.deserialize(v, TYPEREF)).orElse(Map.of());
    }

    @Override
    public void setEnumValues(final Map<String, String> enumValues) {
        this.enumValues = Optional.ofNullable(enumValues).map(POJOHelper::serialize).orElse(null);
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
}
