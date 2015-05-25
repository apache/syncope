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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "group")
@XmlType
@JsonIgnoreProperties({ "displayName" })
public class GroupTO extends AnyTO {

    private static final long serialVersionUID = -7785920258290147542L;

    private String name;

    private Long userOwner;

    private Long groupOwner;

    private String aDynMembershipCond;

    private String uDynMembershipCond;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Long getUserOwner() {
        return userOwner;
    }

    public void setUserOwner(final Long userOwner) {
        this.userOwner = userOwner;
    }

    public Long getGroupOwner() {
        return groupOwner;
    }

    public void setGroupOwner(final Long groupOwner) {
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

}
