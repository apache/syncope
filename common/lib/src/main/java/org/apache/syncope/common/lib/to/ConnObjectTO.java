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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "connObject")
@XmlType
public class ConnObjectTO extends AbstractAnnotatedBean {

    private static final long serialVersionUID = 5139554911265442497L;

    private final Set<AttrTO> attrs = new LinkedHashSet<>();

    @XmlElementWrapper(name = "plainAttrs")
    @XmlElement(name = "attribute")
    @JsonProperty("plainAttrs")
    public Set<AttrTO> getPlainAttrs() {
        return attrs;
    }

    @JsonIgnore
    public Map<String, AttrTO> getPlainAttrMap() {
        Map<String, AttrTO> result = new HashMap<>(attrs.size());
        for (AttrTO attributeTO : attrs) {
            result.put(attributeTO.getSchema(), attributeTO);
        }

        return Collections.unmodifiableMap(result);
    }
}
