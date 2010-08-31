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

/**
 * Transfer Object for attributes.
 */
public class AttributeTO extends AbstractBaseBean {

    /**
     * Name of the schema that this attribute is referring to.
     */
    private String schema;
    /**
     * Set of (string) values of this attribute.
     */
    private Set<String> values;

    /**
     * Default constructor.
     */
    public AttributeTO() {
        super();
        values = new HashSet<String>();
    }

    /**
     * @return the name of the schema that this attribute is referring to
     */
    public String getSchema() {
        return schema;
    }

    /**
     * @param schema name to be set
     */
    public void setSchema(final String schema) {
        this.schema = schema;

    }

    /**
     * @param value an attribute value to be added
     * @return wether the operation succeeded or not
     */
    public boolean addValue(final String value) {
        return value == null ? false : values.add(value);
    }

    /**
     * @param value an attribute value to be removed
     * @return wether the operation succeeded or not
     */
    public boolean removeValue(final String value) {
        return value == null ? false : values.remove(value);
    }

    /**
     * @return attribute values as strings
     */
    public Set<String> getValues() {
        return values;
    }

    /**
     * @param values set of (string) values
     */
    public void setValues(final Set<String> values) {
        this.values = values;
    }
}
