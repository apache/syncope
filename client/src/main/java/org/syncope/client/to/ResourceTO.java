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

import java.util.ArrayList;
import java.util.List;
import org.syncope.client.AbstractBaseBean;

public class ResourceTO extends AbstractBaseBean {

    /**
     * The resource identifier is the name.
     */
    private String name;
    /**
     * The resource type is identified by the associated connector.
     */
    private Long connectorId;
    /**
     * Attribute mappings.
     */
    private List<SchemaMappingTO> mappings;
    /**
     * Force mandatory constraint.
     */
    private boolean forceMandatoryConstraint;

    public ResourceTO() {
        mappings = new ArrayList<SchemaMappingTO>();
    }

    public boolean isForceMandatoryConstraint() {
        return forceMandatoryConstraint;
    }

    public void setForceMandatoryConstraint(boolean forceMandatoryConstraint) {
        this.forceMandatoryConstraint = forceMandatoryConstraint;
    }

    public Long getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(Long connectorId) {
        this.connectorId = connectorId;
    }

    public boolean addMapping(SchemaMappingTO mapping) {
        return mappings.add(mapping);
    }

    public boolean removeMapping(SchemaMappingTO mapping) {
        return mappings.remove(mapping);
    }

    public List<SchemaMappingTO> getMappings() {
        return mappings;
    }

    public void setMappings(List<SchemaMappingTO> mappings) {
        this.mappings = mappings;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
