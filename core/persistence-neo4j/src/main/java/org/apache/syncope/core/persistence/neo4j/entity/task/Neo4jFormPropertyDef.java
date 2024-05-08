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
package org.apache.syncope.core.persistence.neo4j.entity.task;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.common.lib.form.FormPropertyType;
import org.apache.syncope.core.persistence.api.entity.task.FormPropertyDef;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
import org.apache.syncope.core.persistence.common.validation.FormPropertyDefCheck;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractProvidedKeyNode;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jFormPropertyDef.NODE)
@FormPropertyDefCheck
public class Neo4jFormPropertyDef extends AbstractProvidedKeyNode implements FormPropertyDef {

    private static final long serialVersionUID = -5839990371546587373L;

    public static final String NODE = "FormPropertyDef";

    @NotNull
    @Relationship(type = Neo4jMacroTask.MACRO_TASK_FORM_PROPERTY_DEF_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jMacroTask macroTask;

    @NotNull
    private String name;

    @NotNull
    private FormPropertyType type;

    @NotNull
    private Boolean readable = Boolean.TRUE;

    @NotNull
    private Boolean writable = Boolean.TRUE;

    @NotNull
    private Boolean required = Boolean.FALSE;

    private String datePattern;

    private String enumValues;

    @Override
    public Neo4jMacroTask getMacroTask() {
        return macroTask;
    }

    @Override
    public void setMacroTask(final MacroTask macroTask) {
        checkType(macroTask, Neo4jMacroTask.class);
        this.macroTask = (Neo4jMacroTask) macroTask;
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
    public String getDatePattern() {
        return datePattern;
    }

    @Override
    public void setDatePattern(final String datePattern) {
        this.datePattern = datePattern;
    }

    @Override
    public Map<String, String> getEnumValues() {
        return Optional.ofNullable(enumValues).
                map(v -> POJOHelper.deserialize(v, new TypeReference<Map<String, String>>() {
        })).orElse(Map.of());
    }

    @Override
    public void setEnumValues(final Map<String, String> enumValues) {
        this.enumValues = Optional.ofNullable(enumValues).map(POJOHelper::serialize).orElse(null);
    }
}
