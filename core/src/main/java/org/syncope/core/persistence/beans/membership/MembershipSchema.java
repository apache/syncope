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
package org.syncope.core.persistence.beans.membership;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.SchemaMapping;

@Entity
public class MembershipSchema extends AbstractSchema {

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "schema")
    private Set<MembershipAttribute> attributes;
    @ManyToMany(mappedBy = "schemas")
    private Set<MembershipDerivedSchema> derivedSchemas;
    /**
     * All the mappings of the attribute schema.
     */
    @OneToMany(cascade = CascadeType.MERGE,
    fetch = FetchType.EAGER, mappedBy = "membershipSchema")
    private Set<SchemaMapping> mappings;

    public MembershipSchema() {
        attributes = new HashSet<MembershipAttribute>();
        derivedSchemas = new HashSet<MembershipDerivedSchema>();
        mappings = new HashSet<SchemaMapping>();
    }

    @Override
    public <T extends AbstractAttribute> boolean addAttribute(T attribute) {
        return attributes.add((MembershipAttribute) attribute);
    }

    @Override
    public <T extends AbstractAttribute> boolean removeAttribute(T attribute) {
        return attributes.remove((MembershipAttribute) attribute);
    }

    @Override
    public Set<? extends AbstractAttribute> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(Set<? extends AbstractAttribute> attributes) {
        this.attributes = (Set<MembershipAttribute>) attributes;
    }

    @Override
    public <T extends AbstractDerivedSchema> boolean addDerivedSchema(
            T derivedSchema) {

        return derivedSchemas.add((MembershipDerivedSchema) derivedSchema);
    }

    @Override
    public <T extends AbstractDerivedSchema> boolean removeDerivedSchema(
            T derivedSchema) {

        return derivedSchemas.remove((MembershipDerivedSchema) derivedSchema);
    }

    @Override
    public Set<? extends AbstractDerivedSchema> getDerivedSchemas() {
        return derivedSchemas;
    }

    @Override
    public void setDerivedSchemas(
            Set<? extends AbstractDerivedSchema> derivedSchemas) {

        this.derivedSchemas = (Set<MembershipDerivedSchema>) derivedSchemas;
    }

    @Override
    public Set<SchemaMapping> getMappings() {
        if (this.mappings == null) {
            this.mappings = new HashSet<SchemaMapping>();
        }
        return this.mappings;
    }

    @Override
    public void setMappings(Set<SchemaMapping> mappings) {
        this.mappings = mappings;
    }

    @Override
    public boolean addMapping(SchemaMapping mapping) {
        if (this.mappings == null) {
            this.mappings = new HashSet<SchemaMapping>();
        }
        return this.mappings.add(mapping);
    }

    @Override
    public boolean removeMapping(SchemaMapping mapping) {
        if (this.mappings == null) {
            return true;
        }
        return this.mappings.remove(mapping);
    }
}
