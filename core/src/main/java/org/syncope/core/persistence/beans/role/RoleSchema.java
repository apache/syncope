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
package org.syncope.core.persistence.beans.role;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.SchemaMapping;

@Entity
public class RoleSchema extends AbstractSchema {

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "schema")
    Set<RoleAttribute> attributes;

    @ManyToMany(mappedBy = "schemas")
    Set<RoleDerivedSchema> derivedSchemas;

    /**
     * All the mappings of the attribute schema.
     */
    @OneToMany(cascade = javax.persistence.CascadeType.ALL,
    fetch = FetchType.EAGER, mappedBy = "roleSchema")
    Set<SchemaMapping> mappings;

    public RoleSchema() {
        attributes = new HashSet<RoleAttribute>();
        derivedSchemas = new HashSet<RoleDerivedSchema>();
    }

    @Override
    public <T extends AbstractAttribute> boolean addAttribute(T attribute) {
        return attributes.add((RoleAttribute) attribute);
    }

    @Override
    public <T extends AbstractAttribute> boolean removeAttribute(T attribute) {
        return attributes.remove((RoleAttribute) attribute);
    }

    @Override
    public Set<? extends AbstractAttribute> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(Set<? extends AbstractAttribute> attributes) {
        this.attributes = (Set<RoleAttribute>) attributes;
    }

    @Override
    public <T extends AbstractDerivedSchema> boolean addDerivedSchema(
            T derivedSchema) {

        return derivedSchemas.add((RoleDerivedSchema) derivedSchema);
    }

    @Override
    public <T extends AbstractDerivedSchema> boolean removeDerivedSchema(
            T derivedSchema) {

        return derivedSchemas.remove((RoleDerivedSchema) derivedSchema);
    }

    @Override
    public Set<? extends AbstractDerivedSchema> getDerivedSchemas() {
        return derivedSchemas;
    }

    @Override
    public void setDerivedSchemas(
            Set<? extends AbstractDerivedSchema> derivedSchemas) {

        this.derivedSchemas = (Set<RoleDerivedSchema>) derivedSchemas;
    }

    @Override
    public Set<SchemaMapping> getMappings() {
        return mappings;
    }

    @Override
    public void setMappings(Set<SchemaMapping> mappings) {
        this.mappings = mappings;
    }

    @Override
    public boolean addMapping(SchemaMapping mapping) {
        if (this.mappings == null) this.mappings = new HashSet<SchemaMapping>();
        return this.mappings.add(mapping);
    }

    @Override
    public boolean removeMapping(SchemaMapping mapping) {
        if (this.mappings == null) this.mappings = new HashSet<SchemaMapping>();
        return this.mappings.remove(mapping);
    }
}
