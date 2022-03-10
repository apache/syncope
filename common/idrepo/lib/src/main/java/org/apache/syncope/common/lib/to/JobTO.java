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

import java.time.OffsetDateTime;
import org.apache.syncope.common.lib.BaseBean;
import org.apache.syncope.common.lib.types.JobType;

public class JobTO implements BaseBean {

    private static final long serialVersionUID = -7254450981751326711L;

    private JobType type;

    private String refKey;

    private String refDesc;

    private boolean running;

    private boolean scheduled;

    private OffsetDateTime start;

    private String status;

    public JobType getType() {
        return type;
    }

    public void setType(final JobType type) {
        this.type = type;
    }

    public String getRefKey() {
        return refKey;
    }

    public void setRefKey(final String refKey) {
        this.refKey = refKey;
    }

    public String getRefDesc() {
        return refDesc;
    }

    public void setRefDesc(final String refDesc) {
        this.refDesc = refDesc;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(final boolean running) {
        this.running = running;
    }

    public boolean isScheduled() {
        return scheduled;
    }

    public void setScheduled(final boolean scheduled) {
        this.scheduled = scheduled;
    }

    public OffsetDateTime getStart() {
        return start;
    }

    public void setStart(final OffsetDateTime start) {
        this.start = start;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }
}
