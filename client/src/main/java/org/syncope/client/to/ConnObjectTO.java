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
package org.syncope.client.to;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.syncope.client.AbstractBaseBean;

public class ConnObjectTO extends AbstractBaseBean {

    private static final long serialVersionUID = 5139554911265442497L;

    private List<AttributeTO> attributes;

    public ConnObjectTO() {
        super();

        attributes = new ArrayList<AttributeTO>();
    }

    public boolean addAttribute(final AttributeTO attribute) {
        return attributes.add(attribute);
    }

    public boolean removeAttribute(final AttributeTO attribute) {
        return attributes.remove(attribute);
    }

    public List<AttributeTO> getAttributes() {
        return attributes;
    }

    public void setAttributes(final List<AttributeTO> attributes) {
        this.attributes = attributes;
    }

    @JsonIgnore
    public Map<String, AttributeTO> getAttributeMap() {
        Map<String, AttributeTO> result =
                new HashMap<String, AttributeTO>(attributes.size());
        for (AttributeTO attributeTO : attributes) {
            result.put(attributeTO.getSchema(), attributeTO);
        }

        return result;
    }
}
