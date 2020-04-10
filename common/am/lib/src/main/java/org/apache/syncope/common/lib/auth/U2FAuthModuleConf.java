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
package org.apache.syncope.common.lib.auth;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "u2fAuthModuleConf")
@XmlType
public class U2FAuthModuleConf extends AbstractAuthModuleConf {

    private static final long serialVersionUID = -1235771400318503131L;

    private long expireRegistrations = 30;

    private String expireRegistrationsTimeUnit = "SECONDS";

    private long expireDevices = 30;

    private String expireDevicesTimeUnit = "DAYS";

    public long getExpireRegistrations() {
        return expireRegistrations;
    }

    public void setExpireRegistrations(final long expireRegistrations) {
        this.expireRegistrations = expireRegistrations;
    }

    public String getExpireRegistrationsTimeUnit() {
        return expireRegistrationsTimeUnit;
    }

    public void setExpireRegistrationsTimeUnit(final String expireRegistrationsTimeUnit) {
        this.expireRegistrationsTimeUnit = expireRegistrationsTimeUnit;
    }

    public long getExpireDevices() {
        return expireDevices;
    }

    public void setExpireDevices(final long expireDevices) {
        this.expireDevices = expireDevices;
    }

    public String getExpireDevicesTimeUnit() {
        return expireDevicesTimeUnit;
    }

    public void setExpireDevicesTimeUnit(final String expireDevicesTimeUnit) {
        this.expireDevicesTimeUnit = expireDevicesTimeUnit;
    }
}
