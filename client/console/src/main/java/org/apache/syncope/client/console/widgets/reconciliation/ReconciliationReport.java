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
package org.apache.syncope.client.console.widgets.reconciliation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.syncope.common.lib.AbstractBaseBean;

public class ReconciliationReport extends AbstractBaseBean {

    private static final long serialVersionUID = 931063230006747313L;

    private final Date run;

    private Anys users;

    private Anys groups;

    private final List<Anys> anyObjects = new ArrayList<>();

    public ReconciliationReport(final Date run) {
        if (run != null) {
            this.run = new Date(run.getTime());
        } else {
            this.run = null;
        }
    }

    public Date getRun() {
        return run == null ? null : new Date(run.getTime());
    }

    public Anys getUsers() {
        return users;
    }

    public void setUsers(final Anys users) {
        this.users = users;
    }

    public Anys getGroups() {
        return groups;
    }

    public void setGroups(final Anys groups) {
        this.groups = groups;
    }

    public List<Anys> getAnyObjects() {
        return anyObjects;
    }

}
