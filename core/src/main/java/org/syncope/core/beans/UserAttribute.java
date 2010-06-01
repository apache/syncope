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
package org.syncope.core.beans;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import javax.persistence.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
public class UserAttribute implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(
            UserAttribute.class);
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER)
    private UserAttributeSchema schema;
    @Transient
    private Class userAttributeClass;
    @OneToMany(cascade = CascadeType.ALL,
    fetch = FetchType.EAGER)
    private Set<UserAttributeValue> values;

    protected UserAttribute() {
        values = new HashSet<UserAttributeValue>();
    }

    public UserAttribute(UserAttributeSchema schema)
            throws ClassNotFoundException {

        this();
        this.setSchema(schema);
    }

    public Long getId() {
        return id;
    }

    public UserAttributeSchema getSchema() {
        return schema;
    }

    public void setSchema(UserAttributeSchema schema)
            throws ClassNotFoundException {

        this.schema = schema;

        if (schema == null) {
            throw new NullPointerException(
                    "Cannot set a NULL UserAttributeSchema!");
        }

        this.schema = schema;
        this.userAttributeClass = Class.forName(
                schema.getType().getClassName());
    }

    public Set<UserAttributeValue> getValues() {
        return values;
    }

    public void setValues(Set<UserAttributeValue> values) {
        this.values = values;
    }

    public boolean addValue(String value) {
        UserAttributeValue actualValue = null;
        try {
            actualValue = getValue(value);
        } catch (ParseException e) {
            log.error("While parsing '" + value + "' as "
                    + userAttributeClass.getClass().getName(), e);
        }

        boolean result = false;
        if (actualValue != null) {
            if (!schema.isMultivalue()) {
                values.clear();
            }
            result = values.add(actualValue);
        }

        return result;
    }

    public boolean addValue(Object value)
            throws ClassCastException {

        if (!userAttributeClass.isInstance(value)) {
            log.error("'" + value + "' is not an instance of "
                    + userAttributeClass.getClass().getName());

            throw getClassCastException(value);
        }

        if (!schema.isMultivalue()) {
            values.clear();
        }

        return values.add(getValue(value));
    }

    public boolean removeValue(String value) {
        UserAttributeValue actualValue = null;
        try {
            actualValue = getValue(value);
        } catch (ParseException e) {
            log.error("While parsing '" + value + "' as "
                    + userAttributeClass.getClass().getName(), e);
        }

        boolean result = false;
        if (actualValue != null) {
            result = values.remove(actualValue);
            if (!values.isEmpty()
                    && !schema.isMultivalue()) {

                values.clear();
            }
        }

        return result;
    }

    public boolean removeValue(Object value)
            throws ClassCastException {

        if (!userAttributeClass.isInstance(value)) {
            log.error("'" + value + "' is not an instance of "
                    + userAttributeClass.getClass().getName());

            throw getClassCastException(value);
        }

        boolean result = values.remove(getValue(value));
        if (!values.isEmpty()
                && !schema.isMultivalue()) {

            values.clear();
        }

        return result;
    }

    private UserAttributeValue getValue(String value)
            throws ParseException {

        UserAttributeValue result = null;

        switch (schema.getType()) {

            case String:
                result = new UserAttributeValueAsString(value);
                break;

            case Boolean:
                result = new UserAttributeValueAsBoolean(
                        Boolean.parseBoolean(value));
                break;

            case Long:
                result = new UserAttributeValueAsLong(
                        Long.valueOf(schema.getFormatter(
                        DecimalFormat.class).parse(value).longValue()));
                break;

            case Double:
                result = new UserAttributeValueAsDouble(
                        Double.valueOf(schema.getFormatter(
                        DecimalFormat.class).parse(value).doubleValue()));
                break;

            case Date:
                result = new UserAttributeValueAsDate(
                        new Date(schema.getFormatter(
                        SimpleDateFormat.class).parse(value).getTime()));
                break;
        }

        return result;
    }

    private UserAttributeValue getValue(Object value) {

        UserAttributeValue result = null;

        switch (schema.getType()) {

            case String:
                result = new UserAttributeValueAsString((String) value);
                break;

            case Boolean:
                result = new UserAttributeValueAsBoolean((Boolean) value);
                break;

            case Long:
                result = new UserAttributeValueAsLong((Long) value);
                break;

            case Double:
                result = new UserAttributeValueAsDouble((Double) value);
                break;

            case Date:
                result = new UserAttributeValueAsDate((Date) value);
                break;
        }

        return result;
    }

    private ClassCastException getClassCastException(Object value) {
        return new ClassCastException("Passed value is instance of "
                + value.getClass().getName()
                + ", while this attribute has type "
                + userAttributeClass.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UserAttribute other = (UserAttribute) obj;
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
