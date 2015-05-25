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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "anyObjectMod")
@XmlType
public class AnyObjectMod extends AnyMod {

    private static final long serialVersionUID = -3474517624611170097L;

    private final List<Long> relationshipsToAdd = new ArrayList<>();

    private final List<Long> relationshipsToRemove = new ArrayList<>();

    private final List<Long> membershipsToAdd = new ArrayList<>();

    private final List<Long> membershipsToRemove = new ArrayList<>();

    @XmlElementWrapper(name = "relationshipsToAdd")
    @XmlElement(name = "relationship")
    @JsonProperty("relationshipsToAdd")
    public List<Long> getRelationshipsToAdd() {
        return relationshipsToAdd;
    }

    @XmlElementWrapper(name = "urelationshipsToRemove")
    @XmlElement(name = "urelationship")
    @JsonProperty("urelationshipsToRemove")
    public List<Long> getRelationshipsToRemove() {
        return relationshipsToRemove;
    }

    @XmlElementWrapper(name = "membershipsToAdd")
    @XmlElement(name = "membership")
    @JsonProperty("membershipsToAdd")
    public List<Long> getMembershipsToAdd() {
        return membershipsToAdd;
    }

    @XmlElementWrapper(name = "membershipsToRemove")
    @XmlElement(name = "membership")
    @JsonProperty("membershipsToRemove")
    public List<Long> getMembershipsToRemove() {
        return membershipsToRemove;
    }

}
