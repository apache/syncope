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
package org.apache.syncope.fit.core.reference;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.Objects;
import org.apache.syncope.common.lib.command.CommandArgs;

public class TestCommandArgs extends CommandArgs {

    private static final long serialVersionUID = 1408260716514938521L;

    @NotEmpty
    @Schema(description = "parent realm", example = "/even/two", defaultValue = "/",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String parentRealm = "/";

    @NotEmpty
    @Schema(description = "new realm name", example = "realm123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String realmName;

    @NotEmpty
    @Schema(description = "printer name", example = "printer123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String printerName;

    public String getParentRealm() {
        return parentRealm;
    }

    public String getRealmName() {
        return realmName;
    }

    public String getPrinterName() {
        return printerName;
    }

    public void setParentRealm(final String parentRealm) {
        this.parentRealm = parentRealm;
    }

    public void setRealmName(final String realmName) {
        this.realmName = realmName;
    }

    public void setPrinterName(final String printerName) {
        this.printerName = printerName;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.parentRealm);
        hash = 89 * hash + Objects.hashCode(this.realmName);
        hash = 89 * hash + Objects.hashCode(this.printerName);
        return hash;
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
        final TestCommandArgs other = (TestCommandArgs) obj;
        if (!Objects.equals(this.parentRealm, other.parentRealm)) {
            return false;
        }
        if (!Objects.equals(this.realmName, other.realmName)) {
            return false;
        }
        return Objects.equals(this.printerName, other.printerName);
    }

    @Override
    public String toString() {
        return "TestCommandArgs{"
                + "parentRealm=" + parentRealm
                + ", realmName=" + realmName
                + ", printerName=" + printerName
                + '}';
    }
}
