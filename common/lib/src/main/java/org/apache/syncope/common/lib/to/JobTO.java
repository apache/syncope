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

import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;

@XmlRootElement(name = "job")
@XmlType
public class JobTO extends AbstractBaseBean {

    private static final long serialVersionUID = -7254450981751326711L;

    private Long referenceKey;

    private String referenceName;

    private boolean running;

    private boolean scheduled;

    private String status;

    private Date start;

    public Long getReferenceKey() {
        return referenceKey;
    }

    public void setReferenceKey(final Long referenceKey) {
        this.referenceKey = referenceKey;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public void setReferenceName(final String referenceName) {
        this.referenceName = referenceName;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public Date getStart() {
        return start == null
                ? null
                : new Date(start.getTime());
    }

    public void setStart(final Date start) {
        this.start = start == null
                ? null
                : new Date(start.getTime());
    }
}
