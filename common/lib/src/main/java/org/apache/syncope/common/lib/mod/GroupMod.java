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

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "groupMod")
@XmlType
public class GroupMod extends AnyMod {

    private static final long serialVersionUID = 7455805264680210747L;

    private String name;

    private ReferenceMod userOwner;

    private ReferenceMod groupOwner;

    private String aDynMembershipCond;

    private String uDynMembershipCond;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public ReferenceMod getUserOwner() {
        return userOwner;
    }

    public void setUserOwner(final ReferenceMod userOwner) {
        this.userOwner = userOwner;
    }

    public ReferenceMod getGroupOwner() {
        return groupOwner;
    }

    public void setGroupOwner(final ReferenceMod groupOwner) {
        this.groupOwner = groupOwner;
    }

    public String getADynMembershipCond() {
        return aDynMembershipCond;
    }

    public void setADynMembershipCond(final String aDynMembershipCond) {
        this.aDynMembershipCond = aDynMembershipCond;
    }

    public String getUDynMembershipCond() {
        return uDynMembershipCond;
    }

    public void setUDynMembershipCond(final String uDynMembershipCond) {
        this.uDynMembershipCond = uDynMembershipCond;
    }

    @JsonIgnore
    @Override
    public boolean isEmpty() {
        return super.isEmpty() && name == null && userOwner == null && groupOwner == null
                && aDynMembershipCond == null && uDynMembershipCond == null;
    }
}
