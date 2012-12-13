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
package org.apache.syncope.mod;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.syncope.AbstractBaseBean;

@XmlType
@XmlRootElement
public class StatusMod extends AbstractBaseBean {

    public enum Status {
        ACTIVATE, REACTIVATE, SUSPEND;
    }

    public StatusMod(long id, Status status) {
        this.id = id;
        this.status = status;
    }

    public StatusMod() {
    }

    private Status status;

    private String token;

    private static final long serialVersionUID = 1338094801957616986L;

    private long id;

    private boolean updateInternal = true;

    private boolean updateRemote = true;

    private final Set<String> excludeResources = new HashSet<String>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isUpdateInternal() {
        return updateInternal;
    }

    public void setUpdateInternal(boolean updateInternal) {
        this.updateInternal = updateInternal;
    }

    public boolean isUpdateRemote() {
        return updateRemote;
    }

    public void setUpdateRemote(boolean updateRemote) {
        this.updateRemote = updateRemote;
    }

    @XmlElementWrapper(name = "excludeResources")
    @XmlElement(name = "resource")
    public Set<String> getExcludeResources() {
        return excludeResources;
    }

    /**
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * @return the token
     */
    public String getToken() {
        return token;
    }

    /**
     * @param token
     *            the token to set
     */
    public void setToken(String token) {
        this.token = token;
    }

}
