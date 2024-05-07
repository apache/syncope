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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.command.CommandTO;

@Schema(allOf = { SchedTaskTO.class }, discriminatorProperty = "_class")
public class MacroTaskTO extends SchedTaskTO {

    private static final long serialVersionUID = -2387363212408909094L;

    private String realm;

    private final List<CommandTO> commands = new ArrayList<>();

    private boolean continueOnError;

    private boolean saveExecs = true;

    private final List<FormPropertyDefTO> formPropertyDefs = new ArrayList<>();

    private String macroActions;

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "org.apache.syncope.common.lib.to.MacroTaskTO")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(final String realm) {
        this.realm = realm;
    }

    @JacksonXmlElementWrapper(localName = "commands")
    @JacksonXmlProperty(localName = "command")
    public List<CommandTO> getCommands() {
        return commands;
    }

    public boolean isContinueOnError() {
        return continueOnError;
    }

    public void setContinueOnError(final boolean continueOnError) {
        this.continueOnError = continueOnError;
    }

    public boolean isSaveExecs() {
        return saveExecs;
    }

    public void setSaveExecs(final boolean saveExecs) {
        this.saveExecs = saveExecs;
    }

    @JacksonXmlElementWrapper(localName = "formPropertyDefs")
    @JacksonXmlProperty(localName = "formPropertyDef")
    public List<FormPropertyDefTO> getFormPropertyDefs() {
        return formPropertyDefs;
    }

    public String getMacroActions() {
        return macroActions;
    }

    public void setMacroActions(final String macroActions) {
        this.macroActions = macroActions;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(realm).
                append(commands).
                append(continueOnError).
                append(saveExecs).
                append(formPropertyDefs).
                append(macroActions).
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
        final MacroTaskTO other = (MacroTaskTO) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(realm, other.realm).
                append(commands, other.commands).
                append(continueOnError, other.continueOnError).
                append(saveExecs, other.saveExecs).
                append(formPropertyDefs, other.formPropertyDefs).
                append(macroActions, other.macroActions).
                build();
    }
}
