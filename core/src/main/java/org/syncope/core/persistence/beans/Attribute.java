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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.core.persistence.validation.ValidationException;

@Entity
public class Attribute implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(
            Attribute.class);
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER)
    private AttributeSchema schema;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<AttributeValue> values;

    protected Attribute() {
        values = new HashSet<AttributeValue>();
    }

    public Attribute(AttributeSchema schema)
            throws ClassNotFoundException {

        this();
        this.setSchema(schema);
    }

    public Long getId() {
        return id;
    }

    public AttributeSchema getSchema() {
        return schema;
    }

    public void setSchema(AttributeSchema schema) {
        this.schema = schema;
    }

    public Set<AttributeValue> getValues() {
        return values;
    }

    public void setValues(Set<AttributeValue> values) {
        this.values = values;
    }

    public void addValue(Object value)
            throws ValidationException {

        AttributeValue actualValue =
                getSchema().getValidator().getValue(value);

        if (!schema.isMultivalue()) {
            values.clear();
        }

        values.add(actualValue);
    }

    public void removeValue(Object value)
            throws ValidationException {

        AttributeValue actualValue =
                getSchema().getValidator().getValue(value);

        values.remove(actualValue);
        if (!values.isEmpty() && !schema.isMultivalue()) {
            values.clear();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Attribute other = (Attribute) obj;
        if (this.id != other.id
                && (this.id == null || !this.id.equals(other.id))) {

            return false;
        }
        if (this.schema != other.schema
                && (this.schema == null
                || !this.schema.equals(other.schema))) {

            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + (this.id != null ? this.id.hashCode() : 0);
        hash = 53 * hash + (this.schema != null ? this.schema.hashCode() : 0);

        return hash;
    }

    @Override
    public String toString() {
        return "("
                + "id=" + getId() + ","
                + "schema=" + schema + ","
                + "values=" + values
                + ")";
    }
}
