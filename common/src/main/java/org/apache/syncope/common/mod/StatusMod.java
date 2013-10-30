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
package org.apache.syncope.common.mod;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.AbstractBaseBean;

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
     * Id of user to for which status update is requested.
     */
    private long id;

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
    private final List<String> resourceNames = new ArrayList<String>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public List<String> getResourceNames() {
        return resourceNames;
    }

}
