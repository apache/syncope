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
import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.AbstractDerSchema;
import org.syncope.core.persistence.beans.AbstractSchema;

@Entity
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@NamedQueries({
    @NamedQuery(name = "RSchema.findAll",
    query = "SELECT e FROM RSchema e",
    hints = {
        @QueryHint(name = "org.hibernate.cacheable", value = "true")
    })
})
public class RSchema extends AbstractSchema {

    @OneToMany(mappedBy = "schema")
    private List<RAttr> attributes;

    @ManyToMany(mappedBy = "schemas")
    private List<RDerSchema> derivedSchemas;

    public RSchema() {
        attributes = new ArrayList<RAttr>();
        derivedSchemas = new ArrayList<RDerSchema>();
    }

    @Override
    public <T extends AbstractAttr> boolean addAttribute(T attribute) {
        return attributes.add((RAttr) attribute);
    }

    @Override
    public <T extends AbstractAttr> boolean removeAttribute(T attribute) {
        return attributes.remove((RAttr) attribute);
    }

    @Override
    public List<? extends AbstractAttr> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(List<? extends AbstractAttr> attributes) {
        this.attributes = (List<RAttr>) attributes;
    }

    @Override
    public <T extends AbstractDerSchema> boolean addDerivedSchema(
            T derivedSchema) {

        return derivedSchemas.add((RDerSchema) derivedSchema);
    }

    @Override
    public <T extends AbstractDerSchema> boolean removeDerivedSchema(
            T derivedSchema) {

        return derivedSchemas.remove((RDerSchema) derivedSchema);
    }

    @Override
    public List<? extends AbstractDerSchema> getDerivedSchemas() {
        return derivedSchemas;
    }

    @Override
    public void setDerivedSchemas(
            List<? extends AbstractDerSchema> derivedSchemas) {

        this.derivedSchemas = (List<RDerSchema>) derivedSchemas;
    }
}
