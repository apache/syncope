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
    @NamedQuery(name = "MembershipSchema.findAll",
    query = "SELECT e FROM MembershipSchema e",
    hints = {
        @QueryHint(name = "org.hibernate.cacheable", value = "true")
    })
})
public class MembershipSchema extends AbstractSchema {

    @OneToMany(mappedBy = "schema")
    private List<MembershipAttribute> attributes;

    @ManyToMany(mappedBy = "schemas")
    private List<MembershipDerivedSchema> derivedSchemas;

    public MembershipSchema() {
        attributes = new ArrayList<MembershipAttribute>();
        derivedSchemas = new ArrayList<MembershipDerivedSchema>();
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
    public List<? extends AbstractAttribute> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(List<? extends AbstractAttribute> attributes) {
        this.attributes = (List<MembershipAttribute>) attributes;
    }

    @Override
    public <T extends AbstractDerivedSchema> boolean addDerivedSchema(
            final T derivedSchema) {

        return derivedSchemas.add((MembershipDerivedSchema) derivedSchema);
    }

    @Override
    public <T extends AbstractDerivedSchema> boolean removeDerivedSchema(
            final T derivedSchema) {

        return derivedSchemas.remove((MembershipDerivedSchema) derivedSchema);
    }

    @Override
    public List<? extends AbstractDerivedSchema> getDerivedSchemas() {
        return derivedSchemas;
    }

    @Override
    public void setDerivedSchemas(
            final List<? extends AbstractDerivedSchema> derivedSchemas) {

        this.derivedSchemas.clear();
        if (derivedSchemas != null && !derivedSchemas.isEmpty()) {
            this.derivedSchemas.addAll(
                    (List<MembershipDerivedSchema>) derivedSchemas);
        }
    }
}
