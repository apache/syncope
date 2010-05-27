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
public class UserAttributeValues implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(
            UserAttributeValues.class);
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne(cascade = CascadeType.REMOVE,
    fetch = FetchType.EAGER)
    private UserAttributeSchema userAttributeSchema;
    @Transient
    private Class userAttributeClass;
    @OneToMany(cascade = CascadeType.ALL,
    fetch = FetchType.EAGER)
    private Set<UserAttributeValue> attributeValues;

    protected UserAttributeValues() {
        attributeValues = new HashSet<UserAttributeValue>();
    }

    public UserAttributeValues(UserAttributeSchema userAttributeSchema)
            throws ClassNotFoundException {

        this();
        this.setUserAttributeSchema(userAttributeSchema);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserAttributeSchema getUserAttributeSchema() {
        return userAttributeSchema;
    }

    public void setUserAttributeSchema(UserAttributeSchema userAttributeSchema)
            throws ClassNotFoundException {

        this.userAttributeSchema = userAttributeSchema;

        if (userAttributeSchema == null) {
            throw new NullPointerException(
                    "Cannot set a NULL UserAttributeSchema!");
        }

        this.userAttributeSchema = userAttributeSchema;
        this.userAttributeClass = Class.forName(
                userAttributeSchema.getType().getClassName());
    }

    public Set<? extends UserAttributeValue> getAttributeValues() {
        return attributeValues;
    }

    public void setAttributeValues(Set<UserAttributeValue> attributeValues) {
        this.attributeValues = attributeValues;
    }

    public boolean addAttributeValue(String value) {
        UserAttributeValue actualValue = null;
        try {
            actualValue = getUserattributeValue(value);
        } catch (ParseException e) {
            log.error("While parsing '" + value + "' as "
                    + userAttributeClass.getClass().getName(), e);
        }

        boolean result = false;
        if (actualValue != null) {
            if (!userAttributeSchema.isMultivalue()) {
                attributeValues.clear();
            }
            result = attributeValues.add(actualValue);
        }

        return result;
    }

    public boolean addAttributeValue(Object value)
            throws ClassCastException {

        if (!userAttributeClass.isInstance(value)) {
            log.error("'" + value + "' is not an instance of "
                    + userAttributeClass.getClass().getName());

            throw getClassCastException(value);
        }

        if (!userAttributeSchema.isMultivalue()) {
            attributeValues.clear();
        }

        return attributeValues.add(getUserattributeValue(value));
    }

    public boolean removeAttributeValue(String value) {
        UserAttributeValue actualValue = null;
        try {
            actualValue = getUserattributeValue(value);
        } catch (ParseException e) {
            log.error("While parsing '" + value + "' as "
                    + userAttributeClass.getClass().getName(), e);
        }

        boolean result = false;
        if (actualValue != null) {
            result = attributeValues.remove(actualValue);
            if (!attributeValues.isEmpty()
                    && !userAttributeSchema.isMultivalue()) {

                attributeValues.clear();
            }
        }

        return result;
    }

    public boolean removeAttributeValue(Object value)
            throws ClassCastException {

        if (!userAttributeClass.isInstance(value)) {
            log.error("'" + value + "' is not an instance of "
                    + userAttributeClass.getClass().getName());

            throw getClassCastException(value);
        }

        boolean result = attributeValues.remove(getUserattributeValue(value));
        if (!attributeValues.isEmpty()
                && !userAttributeSchema.isMultivalue()) {

            attributeValues.clear();
        }

        return result;
    }

    private UserAttributeValue getUserattributeValue(String value)
            throws ParseException {

        UserAttributeValue result = null;

        switch (userAttributeSchema.getType()) {

            case String:
                result = new UserAttributeValueAsString(value);
                break;

            case Boolean:
                result = new UserAttributeValueAsBoolean(
                        Boolean.parseBoolean(value));
                break;

            case Long:
                result = new UserAttributeValueAsLong(
                        Long.valueOf(userAttributeSchema.getFormatter(
                        DecimalFormat.class).parse(value).longValue()));
                break;

            case Double:
                result = new UserAttributeValueAsDouble(
                        Double.valueOf(userAttributeSchema.getFormatter(
                        DecimalFormat.class).parse(value).doubleValue()));
                break;

            case Date:
                result = new UserAttributeValueAsDate(
                        new Date(userAttributeSchema.getFormatter(
                        SimpleDateFormat.class).parse(value).getTime()));
                break;
        }

        return result;
    }

    private UserAttributeValue getUserattributeValue(Object value) {

        UserAttributeValue result = null;

        switch (userAttributeSchema.getType()) {

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
        final UserAttributeValues other = (UserAttributeValues) obj;
        if (this.id != other.id
                && (this.id == null || !this.id.equals(other.id))) {

            return false;
        }
        if (this.userAttributeSchema != other.userAttributeSchema
                && (this.userAttributeSchema == null
                || !this.userAttributeSchema.equals(other.userAttributeSchema))) {

            return false;
        }
        if (this.attributeValues != other.attributeValues
                && (this.attributeValues == null
                || !this.attributeValues.equals(other.attributeValues))) {

            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.id != null ? this.id.hashCode() : 0);
        hash = 97 * hash + (this.userAttributeSchema != null
                ? this.userAttributeSchema.hashCode() : 0);
        hash = 97 * hash + (this.attributeValues != null
                ? this.attributeValues.hashCode() : 0);

        return hash;
    }

    @Override
    public String toString() {
        return "("
                + "id=" + getId() + ","
                + "userAttributeSchema=" + userAttributeSchema + ","
                + "attributeValues=" + attributeValues
                + ")";
    }
}
