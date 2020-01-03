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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.UnmatchingRule;

public abstract class AbstractCSVSpec implements Serializable {

    private static final long serialVersionUID = 2253975790270165334L;

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
        public B action(final String action) {
            getInstance().getActions().add(action);
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

    protected boolean allowComments;

    protected UnmatchingRule unmatchingRule = UnmatchingRule.PROVISION;

    protected MatchingRule matchingRule = MatchingRule.UPDATE;

    protected List<String> actions = new ArrayList<>();

    public String getAnyTypeKey() {
        return anyTypeKey;
    }

    @NotNull
    @QueryParam("anyTypeKey")
    public void setAnyTypeKey(final String anyTypeKey) {
        this.anyTypeKey = anyTypeKey;
    }

    public char getColumnSeparator() {
        return columnSeparator;
    }

    @QueryParam("columnSeparator")
    public void setColumnSeparator(final char columnSeparator) {
        this.columnSeparator = columnSeparator;
    }

    public String getArrayElementSeparator() {
        return arrayElementSeparator;
    }

    @QueryParam("arrayElementSeparator")
    public void setArrayElementSeparator(final String arrayElementSeparator) {
        this.arrayElementSeparator = arrayElementSeparator;
    }

    public char getQuoteChar() {
        return quoteChar;
    }

    @QueryParam("quoteChar")
    public void setQuoteChar(final char quoteChar) {
        this.quoteChar = quoteChar;
    }

    public Character getEscapeChar() {
        return escapeChar;
    }

    @QueryParam("escapeChar")
    public void setEscapeChar(final Character escapeChar) {
        this.escapeChar = escapeChar;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    @QueryParam("lineSeparator")
    public void setLineSeparator(final String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    public String getNullValue() {
        return nullValue;
    }

    @QueryParam("nullValue")
    public void setNullValue(final String nullValue) {
        this.nullValue = nullValue;
    }

    public boolean isAllowComments() {
        return allowComments;
    }

    @QueryParam("allowComments")
    public void setAllowComments(final boolean allowComments) {
        this.allowComments = allowComments;
    }

    public UnmatchingRule getUnmatchingRule() {
        return unmatchingRule;
    }

    @QueryParam("unmatchingRule")
    public void setUnmatchingRule(final UnmatchingRule unmatchingRule) {
        this.unmatchingRule = unmatchingRule;
    }

    public MatchingRule getMatchingRule() {
        return matchingRule;
    }

    @QueryParam("matchingRule")
    public void setMatchingRule(final MatchingRule matchingRule) {
        this.matchingRule = matchingRule;
    }

    public List<String> getActions() {
        return actions;
    }

    @QueryParam("actions")
    public void setActions(final List<String> actions) {
        this.actions = actions;
    }
}
