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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlType
public abstract class AbstractAttributableTO extends ConnObjectTO {

    private static final long serialVersionUID = 4083884098736820255L;

    private long key;

    private final Set<AttrTO> derAttrs = new LinkedHashSet<>();

    private final Set<AttrTO> virAttrs = new LinkedHashSet<>();

    public long getKey() {
        return key;
    }

    public void setKey(final long key) {
        this.key = key;
    }

    @XmlElementWrapper(name = "derAttrs")
    @XmlElement(name = "attribute")
    @JsonProperty("derAttrs")
    public Set<AttrTO> getDerAttrs() {
        return derAttrs;
    }

    @JsonIgnore
    public Map<String, AttrTO> getDerAttrMap() {
        Map<String, AttrTO> result = new HashMap<>(derAttrs.size());
        for (AttrTO attributeTO : derAttrs) {
            result.put(attributeTO.getSchema(), attributeTO);
        }

        return Collections.unmodifiableMap(result);
    }

    @XmlElementWrapper(name = "virAttrs")
    @XmlElement(name = "attribute")
    @JsonProperty("virAttrs")
    public Set<AttrTO> getVirAttrs() {
        return virAttrs;
    }

    @JsonIgnore
    public Map<String, AttrTO> getVirAttrMap() {
        Map<String, AttrTO> result = new HashMap<>(virAttrs.size());
        for (AttrTO attributeTO : virAttrs) {
            result.put(attributeTO.getSchema(), attributeTO);
        }

        return Collections.unmodifiableMap(result);
    }

}
