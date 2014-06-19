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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlType
public abstract class AbstractSubjectMod extends AbstractAttributableMod {

    private static final long serialVersionUID = -6404459635536484024L;

    protected final Set<String> resourcesToAdd = new HashSet<String>();

    protected final Set<String> resourcesToRemove = new HashSet<String>();

    @XmlElementWrapper(name = "resourcesToAdd")
    @XmlElement(name = "resource")
    @JsonProperty("resourcesToAdd")
    public Set<String> getResourcesToAdd() {
        return resourcesToAdd;
    }

    @XmlElementWrapper(name = "resourcesToRemove")
    @XmlElement(name = "resource")
    @JsonProperty("resourcesToRemove")
    public Set<String> getResourcesToRemove() {
        return resourcesToRemove;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && resourcesToAdd.isEmpty() && resourcesToRemove.isEmpty();
    }

}
