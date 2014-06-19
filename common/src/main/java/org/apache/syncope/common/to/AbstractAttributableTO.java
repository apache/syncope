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
package org.apache.syncope.common.to;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlType
public abstract class AbstractAttributableTO extends ConnObjectTO {

    private static final long serialVersionUID = 4083884098736820255L;

    private long id;

    private final List<AttributeTO> derAttrs = new ArrayList<AttributeTO>();

    private final List<AttributeTO> virAttrs = new ArrayList<AttributeTO>();

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @JsonIgnore
    public Map<String, AttributeTO> getDerAttrMap() {
        Map<String, AttributeTO> result = new HashMap<String, AttributeTO>(derAttrs.size());
        for (AttributeTO attributeTO : derAttrs) {
            result.put(attributeTO.getSchema(), attributeTO);
        }
        result = Collections.unmodifiableMap(result);

        return result;
    }

    @JsonIgnore
    public Map<String, AttributeTO> getVirAttrMap() {
        Map<String, AttributeTO> result = new HashMap<String, AttributeTO>(virAttrs.size());
        for (AttributeTO attributeTO : virAttrs) {
            result.put(attributeTO.getSchema(), attributeTO);
        }
        result = Collections.unmodifiableMap(result);

        return result;
    }

    @XmlElementWrapper(name = "derivedAttributes")
    @XmlElement(name = "attribute")
    @JsonProperty("derivedAttributes")
    public List<AttributeTO> getDerAttrs() {
        return derAttrs;
    }

    @XmlElementWrapper(name = "virtualAttributes")
    @XmlElement(name = "attribute")
    @JsonProperty("virtualAttributes")
    public List<AttributeTO> getVirAttrs() {
        return virAttrs;
    }
}
