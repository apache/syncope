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
package org.apache.syncope.console.commons;

import java.io.Serializable;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;

public class StatusBean implements Serializable {

    private static final long serialVersionUID = -5207260204921071129L;

    private final Long attributableId;

    private final String attributableName;

    private final String resourceName;

    private String accountLink = null;

    private StatusUtils.Status status = StatusUtils.Status.OBJECT_NOT_FOUND;

    public StatusBean(final AbstractAttributableTO attributable, String resourceName) {
        this.attributableId = attributable.getId();
        this.attributableName = attributable instanceof UserTO
                ? ((UserTO) attributable).getUsername() : ((RoleTO) attributable).getName();
        this.resourceName = resourceName;
    }

    public String getAccountLink() {
        return accountLink;
    }

    public void setAccountLink(final String accountLink) {
        this.accountLink = accountLink;
    }

    public String getResourceName() {
        return resourceName;
    }

    public StatusUtils.Status getStatus() {
        return status;
    }

    public void setStatus(final StatusUtils.Status status) {
        this.status = status;
    }

    public Long getAttributableId() {
        return attributableId;
    }

    public String getAttributableName() {
        return attributableName;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
