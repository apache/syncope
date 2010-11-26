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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import org.syncope.core.persistence.beans.AbstractDerAttr;
import org.syncope.core.persistence.beans.AbstractDerSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.SchemaMapping;

@Entity
public class MDerSchema extends AbstractDerSchema {

    @ManyToMany
    @JoinTable(name = "MSchemaDerivation")
    private Set<MSchema> schemas;
    @OneToMany(mappedBy = "derivedSchema")
    private List<MDerAttr> derivedAttributes;

    public MDerSchema() {
        schemas = new HashSet<MSchema>();
        derivedAttributes = new ArrayList<MDerAttr>();
    }

    @Override
    public <T extends AbstractSchema> boolean addSchema(T schema) {
        return schemas.add((MSchema) schema);
    }

    @Override
    public <T extends AbstractSchema> boolean removeSchema(T schema) {
        return schemas.remove((MSchema) schema);
    }

    @Override
    public Set<MSchema> getSchemas() {
        return schemas;
    }

    @Override
    public void setSchemas(Set<? extends AbstractSchema> schemas) {
        this.schemas = (Set<MSchema>) schemas;
    }

    @Override
    public <T extends AbstractDerAttr> boolean addDerivedAttribute(
            T derivedAttribute) {

        return derivedAttributes.add((MDerAttr) derivedAttribute);
    }

    @Override
    public <T extends AbstractDerAttr> boolean removeDerivedAttribute(
            T derivedAttribute) {

        return derivedAttributes.remove((MDerAttr) derivedAttribute);
    }

    @Override
    public List<? extends AbstractDerAttr> getDerivedAttributes() {
        return derivedAttributes;
    }

    @Override
    public <T extends AbstractDerAttr> void setDerivedAttributes(
            List<T> derivedAttributes) {

        this.derivedAttributes = (List<MDerAttr>) derivedAttributes;
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
    public List<SchemaMapping> getMappings() {
        return Collections.EMPTY_LIST;
    }

    /**
     * TODO: https://code.google.com/p/syncope/issues/detail?id=27
     * @param mappings
     */
    @Override
    public void setMappings(List<SchemaMapping> mappings) {
    }
}
