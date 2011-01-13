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
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.syncope.core.persistence.beans.AbstractDerSchema;
import org.syncope.core.persistence.beans.AbstractSchema;

@Entity
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class RSchema extends AbstractSchema {

    @ManyToMany(mappedBy = "schemas")
    private List<RDerSchema> derivedSchemas;

    public RSchema() {
        derivedSchemas = new ArrayList<RDerSchema>();
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
