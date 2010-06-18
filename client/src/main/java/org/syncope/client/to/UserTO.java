/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.client.to;

import java.util.HashSet;
import java.util.Set;

public class UserTO extends AbstractBaseTO {

    private Long id;
    private Set<AttributeTO> attributes;
    private Set<AttributeTO> derivedAttributes;

    public UserTO() {
        attributes = new HashSet<AttributeTO>();
        derivedAttributes = new HashSet<AttributeTO>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long Id) {
        this.id = Id;
    }

    public Set<AttributeTO> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<AttributeTO> attributes) {
        this.attributes = attributes;
    }

    public Set<AttributeTO> getDerivedAttributes() {
        return derivedAttributes;
    }

    public void setDerivedAttributes(Set<AttributeTO> derivedAttributes) {
        this.derivedAttributes = derivedAttributes;
    }
}
