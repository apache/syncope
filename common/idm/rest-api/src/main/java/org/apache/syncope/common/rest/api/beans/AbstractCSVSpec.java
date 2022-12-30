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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.rest.api.service.JAXRSService;

public abstract class AbstractCSVSpec implements Serializable {

    private static final long serialVersionUID = 2253975790270165334L;

    private static final String PARAM_COLUMNSEPARATOR = "columnSeparator";

    private static final String PARAM_ARRAYELEMENTSEPARATOR = "arrayElementSeparator";

    private static final String PARAM_QUOTECHAR = "quoteChar";

    private static final String PARAM_ESCAPECHAR = "escapeChar";

    private static final String PARAM_LINESEPARATOR = "lineSeparator";

    private static final String PARAM_NULLVALUE = "nullValue";

    private static final String PARAM_ALLOWCOMMENTS = "allowComments";

    private static final String PARAM_MATCHING_RULE = "matchingRule";

    private static final String PARAM_UNMATCHING_RULE = "unmatchingRule";

    protected abstract static class Builder<T extends AbstractCSVSpec, B extends Builder<T, B>> {

        protected T instance;

        protected abstract T newInstance();

        protected T getInstance() {
            if (instance == null) {
                instance = newInstance();
            }
            return instance;
        }

        @SuppressWarnings("unchecked")
        public B columnSeparator(final char columnSeparator) {
            getInstance().setColumnSeparator(columnSeparator);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B arrayElementSeparator(final String arrayElementSeparator) {
            getInstance().setArrayElementSeparator(arrayElementSeparator);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B quoteChar(final char quoteChar) {
            getInstance().setQuoteChar(quoteChar);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B escapeChar(final char escapeChar) {
            getInstance().setEscapeChar(escapeChar);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B lineSeparator(final String lineSeparatorChar) {
            getInstance().setLineSeparator(lineSeparatorChar);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B nullValue(final String nullValue) {
            getInstance().setNullValue(nullValue);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B allowComments(final boolean allowComments) {
            getInstance().setAllowComments(allowComments);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B unmatchingRule(final UnmatchingRule unmatchingRule) {
            getInstance().setUnmatchingRule(unmatchingRule);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B matchingRule(final MatchingRule matchingRule) {
            getInstance().setMatchingRule(matchingRule);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B provisioningAction(final String provisioningActions) {
            getInstance().getProvisioningActions().add(provisioningActions);
            return (B) this;
        }

        public T build() {
            return getInstance();
        }
    }

    protected String anyTypeKey;

    protected char columnSeparator = ',';

    protected String arrayElementSeparator = ";";

    protected char quoteChar = '"';

    protected Character escapeChar;

    protected String lineSeparator = "\n";

    protected String nullValue = "";

    protected Boolean allowComments;

    protected UnmatchingRule unmatchingRule = UnmatchingRule.PROVISION;

    protected MatchingRule matchingRule = MatchingRule.UPDATE;

    protected List<String> provisioningActions = new ArrayList<>();

    @Parameter(name = JAXRSService.PARAM_ANYTYPEKEY, description = "any object type", schema =
            @Schema(implementation = String.class))
    public String getAnyTypeKey() {
        return anyTypeKey;
    }

    @NotNull
    @QueryParam(JAXRSService.PARAM_ANYTYPEKEY)
    public void setAnyTypeKey(final String anyTypeKey) {
        this.anyTypeKey = anyTypeKey;
    }

    @Parameter(name = PARAM_COLUMNSEPARATOR, description = "separator for column values", schema =
            @Schema(implementation = char.class, defaultValue = ","))
    public char getColumnSeparator() {
        return columnSeparator;
    }

    @QueryParam(PARAM_COLUMNSEPARATOR)
    public void setColumnSeparator(final char columnSeparator) {
        this.columnSeparator = columnSeparator;
    }

    @Parameter(name = PARAM_ARRAYELEMENTSEPARATOR, description = "separator for array elements within a "
            + "column", schema =
            @Schema(implementation = String.class, defaultValue = ";"))
    public String getArrayElementSeparator() {
        return arrayElementSeparator;
    }

    @QueryParam(PARAM_ARRAYELEMENTSEPARATOR)
    public void setArrayElementSeparator(final String arrayElementSeparator) {
        this.arrayElementSeparator = arrayElementSeparator;
    }

    @Parameter(name = PARAM_QUOTECHAR, description = "character used for quoting values "
            + "that contain quote characters or linefeeds", schema =
            @Schema(implementation = char.class, defaultValue = "\""))
    public char getQuoteChar() {
        return quoteChar;
    }

    @QueryParam(PARAM_QUOTECHAR)
    public void setQuoteChar(final char quoteChar) {
        this.quoteChar = quoteChar;
    }

    @Parameter(name = PARAM_ESCAPECHAR, description = "if any, used to escape values; "
            + "most commonly defined as backslash", schema =
            @Schema(implementation = Character.class))
    public Character getEscapeChar() {
        return escapeChar;
    }

    @QueryParam(PARAM_ESCAPECHAR)
    public void setEscapeChar(final Character escapeChar) {
        this.escapeChar = escapeChar;
    }

    @Parameter(name = PARAM_LINESEPARATOR, description = "character used to separate data rows", schema =
            @Schema(implementation = String.class, defaultValue = "\\u000a"))
    public String getLineSeparator() {
        return lineSeparator;
    }

    @QueryParam(PARAM_LINESEPARATOR)
    public void setLineSeparator(final String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    @Parameter(name = PARAM_NULLVALUE, description = "when asked to write null, this string value will be used "
            + "instead", schema =
            @Schema(implementation = String.class, defaultValue = ""))
    public String getNullValue() {
        return nullValue;
    }

    @QueryParam(PARAM_NULLVALUE)
    public void setNullValue(final String nullValue) {
        this.nullValue = nullValue;
    }

    @Parameter(name = PARAM_ALLOWCOMMENTS, description = "are hash comments, e.g. lines where the first non-whitespace "
            + "character is '#' allowed? if so, they will be skipped without processing", schema =
            @Schema(implementation = boolean.class, defaultValue = "false"))
    public Boolean getAllowComments() {
        return allowComments == null ? Boolean.FALSE : allowComments;
    }

    @QueryParam(PARAM_ALLOWCOMMENTS)
    @DefaultValue("false")
    public void setAllowComments(final boolean allowComments) {
        this.allowComments = allowComments;
    }

    @Parameter(name = PARAM_UNMATCHING_RULE, required = true, schema =
            @Schema(implementation = UnmatchingRule.class, defaultValue = "PROVISION"))
    public UnmatchingRule getUnmatchingRule() {
        return unmatchingRule;
    }

    @QueryParam(PARAM_UNMATCHING_RULE)
    public void setUnmatchingRule(final UnmatchingRule unmatchingRule) {
        this.unmatchingRule = unmatchingRule;
    }

    @Parameter(name = PARAM_MATCHING_RULE, required = true, schema =
            @Schema(implementation = MatchingRule.class, defaultValue = "UPDATE"))
    public MatchingRule getMatchingRule() {
        return matchingRule;
    }

    @QueryParam(PARAM_MATCHING_RULE)
    public void setMatchingRule(final MatchingRule matchingRule) {
        this.matchingRule = matchingRule;
    }

    public List<String> getProvisioningActions() {
        return provisioningActions;
    }

    @QueryParam("provisioningActions")
    public void setProvisioningActions(final List<String> provisioningActions) {
        this.provisioningActions = provisioningActions;
    }
}
