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

import org.apache.syncope.common.lib.types.AuditElements;

import javax.ws.rs.QueryParam;

import java.util.ArrayList;
import java.util.List;

public class AuditQuery extends AbstractQuery {

    private static final long serialVersionUID = -2863334226169614417L;

    private String key;

    private final List<AuditElements.Result> results = new ArrayList<>();

    private final List<String> events = new ArrayList<>();

    public String getKey() {
        return key;
    }

    @QueryParam("key")
    public void setKey(final String key) {
        this.key = key;
    }

    public List<AuditElements.Result> getResults() {
        return results;
    }

    @QueryParam("results")
    public void setResults(final List<AuditElements.Result> results) {
        if (results != null) {
            this.results.addAll(results);
        }
    }

    public List<String> getEvents() {
        return events;
    }

    @QueryParam("events")
    public void setEvents(final List<String> events) {
        if (events != null) {
            this.events.addAll(events);
        }
    }

    public static class Builder extends AbstractQuery.Builder<AuditQuery, Builder> {

        public Builder key(final String keyword) {
            getInstance().setKey(keyword);
            return this;
        }

        public Builder results(final List<AuditElements.Result> results) {
            getInstance().setResults(results);
            return this;
        }

        public Builder result(final AuditElements.Result result) {
            getInstance().getResults().add(result);
            return this;
        }

        public Builder events(final List<String> events) {
            getInstance().setEvents(events);
            return this;
        }

        public Builder event(final String event) {
            getInstance().getEvents().add(event);
            return this;
        }

        @Override
        protected AuditQuery newInstance() {
            return new AuditQuery();
        }
    }

}
