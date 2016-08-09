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

import java.util.Date;
import javax.validation.constraints.NotNull;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.apache.syncope.common.lib.AbstractBaseBean;

public class BulkExecDeleteQuery extends AbstractBaseBean {

    private static final long serialVersionUID = 3846547401120638351L;

    public static class Builder {

        private final BulkExecDeleteQuery instance = new BulkExecDeleteQuery();

        public Builder key(final String key) {
            instance.setKey(key);
            return this;
        }

        public Builder startedBefore(final Date date) {
            instance.setStartedBefore(date);
            return this;
        }

        public Builder startedAfter(final Date date) {
            instance.setStartedAfter(date);
            return this;
        }

        public Builder endedBefore(final Date date) {
            instance.setEndedBefore(date);
            return this;
        }

        public Builder endedAfter(final Date date) {
            instance.setEndedAfter(date);
            return this;
        }

        public BulkExecDeleteQuery build() {
            return instance;
        }
    }

    private String key;

    private Date startedBefore;

    private Date startedAfter;

    private Date endedBefore;

    private Date endedAfter;

    public String getKey() {
        return key;
    }

    @NotNull
    @PathParam("key")
    public void setKey(final String key) {
        this.key = key;
    }

    public Date getStartedBefore() {
        return startedBefore;
    }

    @QueryParam("startedBefore")
    public void setStartedBefore(final Date startedBefore) {
        this.startedBefore = startedBefore;
    }

    public Date getStartedAfter() {
        return startedAfter;
    }

    @QueryParam("startedAfter")
    public void setStartedAfter(final Date startedAfter) {
        this.startedAfter = startedAfter;
    }

    public Date getEndedBefore() {
        return endedBefore;
    }

    @QueryParam("endedBefore")
    public void setEndedBefore(final Date endedBefore) {
        this.endedBefore = endedBefore;
    }

    public Date getEndedAfter() {
        return endedAfter;
    }

    @QueryParam("endedAfter")
    public void setEndedAfter(final Date endedAfter) {
        this.endedAfter = endedAfter;
    }

}
