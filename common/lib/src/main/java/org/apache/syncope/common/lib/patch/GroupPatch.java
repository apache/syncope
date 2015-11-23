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
package org.apache.syncope.common.lib.patch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.syncope.common.lib.jaxb.XmlGenericMapAdapter;

@XmlRootElement(name = "groupPatch")
@XmlType
public class GroupPatch extends AnyPatch {

    private static final long serialVersionUID = -460284378124440077L;

    private StringReplacePatchItem name;

    private LongReplacePatchItem userOwner;

    private LongReplacePatchItem groupOwner;

    private StringReplacePatchItem adynMembershipCond;

    private StringReplacePatchItem udynMembershipCond;

    @XmlJavaTypeAdapter(XmlGenericMapAdapter.class)
    @JsonIgnore
    private final Map<String, Set<String>> typeExtensions = new HashMap<>();

    public StringReplacePatchItem getName() {
        return name;
    }

    public void setName(final StringReplacePatchItem name) {
        this.name = name;
    }

    public LongReplacePatchItem getUserOwner() {
        return userOwner;
    }

    public void setUserOwner(final LongReplacePatchItem userOwner) {
        this.userOwner = userOwner;
    }

    public LongReplacePatchItem getGroupOwner() {
        return groupOwner;
    }

    public void setGroupOwner(final LongReplacePatchItem groupOwner) {
        this.groupOwner = groupOwner;
    }

    public StringReplacePatchItem getADynMembershipCond() {
        return adynMembershipCond;
    }

    public void setADynMembershipCond(final StringReplacePatchItem adynMembershipCond) {
        this.adynMembershipCond = adynMembershipCond;
    }

    public StringReplacePatchItem getUDynMembershipCond() {
        return udynMembershipCond;
    }

    public void setUDynMembershipCond(final StringReplacePatchItem udynMembershipCond) {
        this.udynMembershipCond = udynMembershipCond;
    }

    @JsonProperty
    public Map<String, Set<String>> getTypeExtensions() {
        return typeExtensions;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty()
                && name == null && userOwner == null && groupOwner == null
                && adynMembershipCond == null && udynMembershipCond == null && typeExtensions.isEmpty();
    }

}
