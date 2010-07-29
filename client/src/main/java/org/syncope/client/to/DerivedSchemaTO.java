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
package org.syncope.client.to;

import java.util.HashSet;
import java.util.Set;
import org.syncope.client.AbstractBaseBean;

public class DerivedSchemaTO extends AbstractBaseBean {

    private String name;
    private String expression;
    private Set<String> schemas;
    private int derivedAttributes;

    public DerivedSchemaTO() {
        schemas = new HashSet<String>();
    }

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

    public boolean addSchema(String schema) {
        return schemas.add(schema);
    }

    public boolean removeSchema(String schema) {
        return schemas.remove(schema);
    }

    public Set<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(Set<String> schemas) {
        this.schemas = schemas;
    }

    public int getDerivedAttributes() {
        return derivedAttributes;
    }

    public void setDerivedAttributes(int derivedAttributes) {
        this.derivedAttributes = derivedAttributes;
    }
}
