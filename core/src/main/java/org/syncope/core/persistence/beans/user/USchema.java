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
package org.syncope.core.persistence.beans.user;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.AbstractDerSchema;
import org.syncope.core.persistence.beans.AbstractSchema;

@Entity
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class USchema extends AbstractSchema {

    @OneToMany(mappedBy = "schema")
    private List<UAttr> attributes;

    @ManyToMany(mappedBy = "schemas")
    private List<UDerSchema> derivedSchemas;

    public USchema() {
        attributes = new ArrayList<UAttr>();
        derivedSchemas = new ArrayList<UDerSchema>();
    }

    @Override
    public <T extends AbstractAttr> boolean addAttribute(T attribute) {
        return attributes.add((UAttr) attribute);
    }

    @Override
    public <T extends AbstractAttr> boolean removeAttribute(T attribute) {
        return attributes.remove((UAttr) attribute);
    }

    @Override
    public List<? extends AbstractAttr> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(List<? extends AbstractAttr> attributes) {
        this.attributes = (List<UAttr>) attributes;
    }

    @Override
    public <T extends AbstractDerSchema> boolean addDerivedSchema(
            T derivedSchema) {

        return derivedSchemas.add((UDerSchema) derivedSchema);
    }

    @Override
    public <T extends AbstractDerSchema> boolean removeDerivedSchema(
            T derivedSchema) {

        return derivedSchemas.remove((UDerSchema) derivedSchema);
    }

    @Override
    public List<? extends AbstractDerSchema> getDerivedSchemas() {
        return derivedSchemas;
    }

    @Override
    public void setDerivedSchemas(
            List<? extends AbstractDerSchema> derivedSchemas) {

        this.derivedSchemas = (List<UDerSchema>) derivedSchemas;
    }
}
