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
package org.apache.syncope.common.lib.mod;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;

@XmlRootElement(name = "statusMod")
@XmlType
public class StatusMod extends AbstractBaseBean {

    private static final long serialVersionUID = 3230910033784302656L;

    @XmlEnum
    @XmlType(name = "statusModType")
    public enum ModType {

        ACTIVATE,
        SUSPEND,
        REACTIVATE;

    }

    /**
     * Key of user to for which status update is requested.
     */
    private long key;

    private ModType type;

    /**
     * Update token (if required).
     */
    private String token;

    /**
     * Whether update should be performed on internal storage.
     */
    private boolean onSyncope = true;

    /**
     * External resources for which update is needed to be propagated.
     */
    private final List<String> resourceNames = new ArrayList<>();

    public long getKey() {
        return key;
    }

    @PathParam("key")
    public void setKey(final long key) {
        this.key = key;
    }

    public ModType getType() {
        return type;
    }

    public void setType(final ModType type) {
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public boolean isOnSyncope() {
        return onSyncope;
    }

    public void setOnSyncope(final boolean onSyncope) {
        this.onSyncope = onSyncope;
    }

    @XmlElementWrapper(name = "resources")
    @XmlElement(name = "resource")
    @JsonProperty("resources")
    public List<String> getResources() {
        return resourceNames;
    }

}
