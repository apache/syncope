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

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CSVPushSpec extends AbstractCSVSpec {

    private static final long serialVersionUID = -8198709720749169648L;

    public static class Builder extends AbstractCSVSpec.Builder<CSVPushSpec, Builder> {

        @Override
        protected CSVPushSpec newInstance() {
            return new CSVPushSpec();
        }

        public Builder(final String anyTypeKey) {
            getInstance().setAnyTypeKey(anyTypeKey);
        }

        public Builder field(final String field) {
            getInstance().getFields().add(field);
            return this;
        }

        public Builder fields(final Collection<String> fields) {
            getInstance().getFields().addAll(fields);
            return this;
        }

        public Builder plainAttr(final String plainAttr) {
            getInstance().getPlainAttrs().add(plainAttr);
            return this;
        }

        public Builder plainAttrs(final Collection<String> plainAttrs) {
            getInstance().getPlainAttrs().addAll(plainAttrs);
            return this;
        }

        public Builder derAttr(final String derAttr) {
            getInstance().getDerAttrs().add(derAttr);
            return this;
        }

        public Builder derAttrs(final Collection<String> derAttrs) {
            getInstance().getDerAttrs().addAll(derAttrs);
            return this;
        }

        public Builder virAttr(final String virAttr) {
            getInstance().getVirAttrs().add(virAttr);
            return this;
        }

        public Builder virAttrs(final Collection<String> virAttrs) {
            getInstance().getVirAttrs().addAll(virAttrs);
            return this;
        }

        public Builder ignorePaging(final boolean ignorePaging) {
            getInstance().setIgnorePaging(ignorePaging);
            return this;
        }

        public Builder propagationAction(final String propagationAction) {
            getInstance().getPropagationActions().add(propagationAction);
            return this;
        }
    }

    private List<String> fields = new ArrayList<>();

    private List<String> plainAttrs = new ArrayList<>();

    private List<String> derAttrs = new ArrayList<>();

    private List<String> virAttrs = new ArrayList<>();

    private Boolean ignorePaging;

    protected List<String> propagationActions = new ArrayList<>();

    public List<String> getFields() {
        return fields;
    }

    @QueryParam("fields")
    public void setFields(final List<String> fields) {
        this.fields = fields;
    }

    public List<String> getPlainAttrs() {
        return plainAttrs;
    }

    @QueryParam("plainAttrs")
    public void setPlainAttrs(final List<String> plainAttrs) {
        this.plainAttrs = plainAttrs;
    }

    public List<String> getDerAttrs() {
        return derAttrs;
    }

    @QueryParam("derAttrs")
    public void setDerAttrs(final List<String> derAttrs) {
        this.derAttrs = derAttrs;
    }

    public List<String> getVirAttrs() {
        return virAttrs;
    }

    @QueryParam("virAttrs")
    public void setVirAttrs(final List<String> virAttrs) {
        this.virAttrs = virAttrs;
    }

    public Boolean getIgnorePaging() {
        return ignorePaging == null ? Boolean.FALSE : ignorePaging;
    }

    @QueryParam("ignorePaging")
    @DefaultValue("false")
    public void setIgnorePaging(final Boolean ignorePaging) {
        this.ignorePaging = ignorePaging;
    }

    public List<String> getPropagationActions() {
        return propagationActions;
    }

    @QueryParam("propagationActions")
    public void setPropagationActions(final List<String> propagationActions) {
        this.propagationActions = propagationActions;
    }
}
