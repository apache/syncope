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
package org.syncope.core.persistence.beans;

import java.util.Set;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractDerivedSchema extends AbstractBaseBean {

    @Id
    private String name;
    private String expression;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public abstract <T extends AbstractDerivedAttribute> boolean addDerivedAttribute(T derivedAttribute);

    public abstract <T extends AbstractDerivedAttribute> boolean removeDerivedAttribute(T derivedAttribute);

    public abstract Set<? extends AbstractDerivedAttribute> getDerivedAttributes();

    public abstract <T extends AbstractDerivedAttribute> void setDerivedAttributes(Set<T> derivedAttributes);

    public abstract <T extends AbstractSchema> boolean addSchema(T schema);

    public abstract <T extends AbstractSchema> boolean removeSchema(T schema);

    public abstract Set<? extends AbstractSchema> getSchemas();

    public abstract void setSchemas(Set<? extends AbstractSchema> schemas);

    public abstract boolean addMapping(SchemaMapping mapping);

    public abstract boolean removeMapping(SchemaMapping mapping);

    public abstract Set<SchemaMapping> getMappings();

    public abstract void setMappings(Set<SchemaMapping> mappings);
}
