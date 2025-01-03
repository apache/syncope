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
package org.apache.syncope.common.rest.api.beans;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;

public class CSVPullSpec extends AbstractCSVSpec {

    private static final long serialVersionUID = -5079876176017613587L;

    public static class Builder extends AbstractCSVSpec.Builder<CSVPullSpec, Builder> {

        @Override
        protected CSVPullSpec newInstance() {
            return new CSVPullSpec();
        }

        public Builder(final String anyTypeKey, final String keyColumn) {
            getInstance().setAnyTypeKey(anyTypeKey);
            getInstance().setKeyColumn(keyColumn);
        }

        public Builder remediation(final boolean remediation) {
            instance.setRemediation(remediation);
            return this;
        }

        public Builder ignoreColumns(final String... ignoreColumns) {
            instance.getIgnoreColumns().addAll(Stream.of(ignoreColumns).toList());
            return this;
        }

        public Builder destinationRealm(final String destinationRealm) {
            instance.setDestinationRealm(destinationRealm);
            return this;
        }

        public Builder conflictResolutionAction(final ConflictResolutionAction conflictResolutionAction) {
            instance.setConflictResolutionAction(conflictResolutionAction);
            return this;
        }

        public Builder inboundCorrelationRule(final String inboundCorrelationRule) {
            instance.setInboundCorrelationRule(inboundCorrelationRule);
            return this;
        }
    }

    private String destinationRealm = SyncopeConstants.ROOT_REALM;

    private String keyColumn;

    private Set<String> ignoreColumns = new HashSet<>();

    private Boolean remediation;

    private ConflictResolutionAction conflictResolutionAction = ConflictResolutionAction.IGNORE;

    private String inboundCorrelationRule;

    public String getDestinationRealm() {
        return destinationRealm;
    }

    @QueryParam("destinationRealm")
    public void setDestinationRealm(final String destinationRealm) {
        this.destinationRealm = destinationRealm;
    }

    public String getKeyColumn() {
        return keyColumn;
    }

    @NotNull
    @QueryParam("keyColumn")
    public void setKeyColumn(final String keyColumn) {
        this.keyColumn = keyColumn;
    }

    public Set<String> getIgnoreColumns() {
        return ignoreColumns;
    }

    @QueryParam("ignoreColumns")
    public void setIgnoreColumns(final Set<String> ignoreColumns) {
        this.ignoreColumns = ignoreColumns;
    }

    public Boolean getRemediation() {
        return remediation == null ? Boolean.FALSE : remediation;
    }

    @QueryParam("remediation")
    @DefaultValue("false")
    public void setRemediation(final boolean remediation) {
        this.remediation = remediation;
    }

    public ConflictResolutionAction getConflictResolutionAction() {
        return conflictResolutionAction;
    }

    @QueryParam("conflictResolutionAction")
    public void setConflictResolutionAction(final ConflictResolutionAction conflictResolutionAction) {
        this.conflictResolutionAction = conflictResolutionAction;
    }

    public String getInboundCorrelationRule() {
        return inboundCorrelationRule;
    }

    @QueryParam("inboundCorrelationRule")
    public void setInboundCorrelationRule(final String inboundCorrelationRule) {
        this.inboundCorrelationRule = inboundCorrelationRule;
    }
}
