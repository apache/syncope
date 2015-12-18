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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.apache.syncope.common.lib.AbstractBaseBean;

public class ExecuteQuery extends AbstractBaseBean {

    private static final long serialVersionUID = 3846547401120638351L;

    public static class Builder {

        private final ExecuteQuery instance = new ExecuteQuery();

        public Builder key(final long key) {
            instance.setKey(key);
            return this;
        }

        public Builder startAt(final Date date) {
            instance.setStartAt(date);
            return this;
        }

        public Builder dryRun(final boolean dryRun) {
            instance.setDryRun(dryRun);
            return this;
        }

        public ExecuteQuery build() {
            return instance;
        }
    }

    private Long key;

    private Date startAt;

    private Boolean dryRun;

    public Long getKey() {
        return key;
    }

    @NotNull
    @PathParam("key")
    public void setKey(final Long key) {
        this.key = key;
    }

    public Date getStartAt() {
        return startAt;
    }

    @QueryParam("startAt")
    public void setStartAt(final Date startAt) {
        this.startAt = startAt;
    }

    public Boolean getDryRun() {
        return dryRun == null ? false : dryRun;
    }

    @QueryParam("dryRun")
    @DefaultValue("false")
    public void setDryRun(final Boolean dryRun) {
        this.dryRun = dryRun;
    }

}
