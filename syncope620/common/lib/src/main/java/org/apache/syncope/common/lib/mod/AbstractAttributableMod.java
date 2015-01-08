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
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;

/**
 * Abstract base class for objects that can have attributes removed, added or updated.
 *
 * Attributes can be regular attributes, derived attributes, virtual attributes and resources.
 */
@XmlType
public abstract class AbstractAttributableMod extends AbstractBaseBean {

    private static final long serialVersionUID = 3241118574016303198L;

    protected long key;

    protected final Set<AttrMod> plainAttrsToUpdate = new HashSet<>();

    protected final Set<String> plainAttrsToRemove = new HashSet<>();

    protected final Set<String> derAttrsToAdd = new HashSet<>();

    protected final Set<String> derAttrsToRemove = new HashSet<>();

    protected final Set<AttrMod> virAttrsToUpdate = new HashSet<>();

    protected final Set<String> virAttrsToRemove = new HashSet<>();

    public long getKey() {
        return key;
    }

    public void setKey(final long key) {
        this.key = key;
    }

    @XmlElementWrapper(name = "plainAttrsToRemove")
    @XmlElement(name = "attribute")
    @JsonProperty("plainAttrsToRemove")
    public Set<String> getAttrsToRemove() {
        return plainAttrsToRemove;
    }

    @XmlElementWrapper(name = "plainAttrsToUpdate")
    @XmlElement(name = "attributeMod")
    @JsonProperty("plainAttrsToUpdate")
    public Set<AttrMod> getAttrsToUpdate() {
        return plainAttrsToUpdate;
    }

    @XmlElementWrapper(name = "derAttrsToAdd")
    @XmlElement(name = "attribute")
    @JsonProperty("derAttrsToAdd")
    public Set<String> getDerAttrsToAdd() {
        return derAttrsToAdd;
    }

    @XmlElementWrapper(name = "derAttrsToRemove")
    @XmlElement(name = "attribute")
    @JsonProperty("derAttrsToRemove")
    public Set<String> getDerAttrsToRemove() {
        return derAttrsToRemove;
    }

    @XmlElementWrapper(name = "virAttrsToRemove")
    @XmlElement(name = "attribute")
    @JsonProperty("virAttrsToRemove")
    public Set<String> getVirAttrsToRemove() {
        return virAttrsToRemove;
    }

    @XmlElementWrapper(name = "virAttrsToUpdate")
    @XmlElement(name = "attribute")
    @JsonProperty("virAttrsToUpdate")
    public Set<AttrMod> getVirAttrsToUpdate() {
        return virAttrsToUpdate;
    }

    /**
     * @return true is all backing Sets are empty.
     */
    public boolean isEmpty() {
        return plainAttrsToUpdate.isEmpty() && plainAttrsToRemove.isEmpty()
                && derAttrsToAdd.isEmpty() && derAttrsToRemove.isEmpty()
                && virAttrsToUpdate.isEmpty() && virAttrsToRemove.isEmpty();
    }
}
