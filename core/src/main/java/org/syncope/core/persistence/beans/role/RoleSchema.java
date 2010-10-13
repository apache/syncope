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

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.QueryHint;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedSchema;
import org.syncope.core.persistence.beans.AbstractSchema;

@Entity
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@NamedQueries({
    @NamedQuery(name = "RoleSchema.findAll",
    query = "SELECT e FROM RoleSchema e",
    hints = {
        @QueryHint(name = "org.hibernate.cacheable", value = "true")
    })
})
public class RoleSchema extends AbstractSchema {

    @OneToMany(mappedBy = "schema")
    private List<RoleAttribute> attributes;
    @ManyToMany(mappedBy = "schemas")
    private List<RoleDerivedSchema> derivedSchemas;

    public RoleSchema() {
        attributes = new ArrayList<RoleAttribute>();
        derivedSchemas = new ArrayList<RoleDerivedSchema>();
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
    public List<? extends AbstractAttribute> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(List<? extends AbstractAttribute> attributes) {
        this.attributes = (List<RoleAttribute>) attributes;
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
    public List<? extends AbstractDerivedSchema> getDerivedSchemas() {
        return derivedSchemas;
    }

    @Override
    public void setDerivedSchemas(
            List<? extends AbstractDerivedSchema> derivedSchemas) {

        this.derivedSchemas = (List<RoleDerivedSchema>) derivedSchemas;
    }
}
