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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import org.syncope.core.persistence.beans.AbstractDerivedAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.SchemaMapping;

@Entity
public class RoleDerivedSchema extends AbstractDerivedSchema {

    @ManyToMany
    private Set<RoleSchema> schemas;
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "derivedSchema")
    private Set<RoleDerivedAttribute> derivedAttributes;

    public RoleDerivedSchema() {
        schemas = new HashSet<RoleSchema>();
        derivedAttributes = new HashSet<RoleDerivedAttribute>();
    }

    @Override
    public <T extends AbstractSchema> boolean addSchema(T schema) {
        return schemas.add((RoleSchema) schema);
    }

    @Override
    public <T extends AbstractSchema> boolean removeSchema(T schema) {
        return schemas.remove((RoleSchema) schema);
    }

    @Override
    public Set<RoleSchema> getSchemas() {
        return schemas;
    }

    @Override
    public void setSchemas(Set<? extends AbstractSchema> schemas) {
        this.schemas = (Set<RoleSchema>) schemas;
    }

    @Override
    public <T extends AbstractDerivedAttribute> boolean addDerivedAttribute(
            T derivedAttribute) {

        return derivedAttributes.add((RoleDerivedAttribute) derivedAttribute);
    }

    @Override
    public <T extends AbstractDerivedAttribute> boolean removeDerivedAttribute(
            T derivedAttribute) {

        return derivedAttributes.remove((RoleDerivedAttribute) derivedAttribute);
    }

    @Override
    public Set<? extends AbstractDerivedAttribute> getDerivedAttributes() {
        return derivedAttributes;
    }

    @Override
    public <T extends AbstractDerivedAttribute> void setDerivedAttributes(
            Set<T> derivedAttributes) {

        this.derivedAttributes = (Set<RoleDerivedAttribute>) derivedAttributes;
    }

    /**
     * TODO: https://code.google.com/p/syncope/issues/detail?id=27
     * @param mapping
     * @return
     */
    @Override
    public boolean addMapping(SchemaMapping mapping) {
        return true;
    }

    /**
     * TODO: https://code.google.com/p/syncope/issues/detail?id=27
     * @param mapping
     * @return
     */
    @Override
    public boolean removeMapping(SchemaMapping mapping) {
        return true;
    }

    /**
     * TODO: https://code.google.com/p/syncope/issues/detail?id=27
     * @return
     */
    @Override
    public Set<SchemaMapping> getMappings() {
        return Collections.EMPTY_SET;
    }

    /**
     * TODO: https://code.google.com/p/syncope/issues/detail?id=27
     * @param mappings
     */
    @Override
    public void setMappings(Set<SchemaMapping> mappings) {
    }
}
