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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import org.syncope.core.persistence.validation.ValidationException;

@MappedSuperclass
public abstract class AbstractAttribute extends AbstractBaseBean {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    public Long getId() {
        return id;
    }

    public <T extends AbstractAttributeValue> void addValue(
            Object value, T attributeValue) throws ValidationException {

        T actualValue =
                getSchema().getValidator().getValue(value, attributeValue);
        actualValue.setAttribute(this);

        if (!getSchema().isMultivalue()) {
            getAttributeValues().clear();
        }

        addAttributeValue(actualValue);
    }

    public <T extends AbstractAttributeValue> void removeValue(
            Object value, T attributeValue) throws ValidationException {

        T actualValue =
                getSchema().getValidator().getValue(value, attributeValue);

        removeAttributeValue(actualValue);
        if (!getAttributeValues().isEmpty() && !getSchema().isMultivalue()) {
            getAttributeValues().clear();
        }
    }

    public abstract <T extends AbstractAttributable> T getOwner();

    public abstract <T extends AbstractAttributable> void setOwner(T owner);

    public abstract <T extends AbstractSchema> T getSchema();

    public abstract <T extends AbstractSchema> void setSchema(T schema);

    public abstract <T extends AbstractAttributeValue> boolean addAttributeValue(T attributeValue);

    public abstract <T extends AbstractAttributeValue> boolean removeAttributeValue(T attributeValue);

    public abstract Set<? extends AbstractAttributeValue> getAttributeValues();

    public Set<String> getStringAttributeValues() {
        Set<? extends AbstractAttributeValue> values = getAttributeValues();
        Set<String> result = new HashSet<String>(values.size());

        String stringValue = "";
        for (AbstractAttributeValue attributeValue : values) {
            switch (getSchema().getType()) {

                case String:
                    stringValue = (String) attributeValue.getValue();
                    break;

                case Boolean:
                    stringValue = ((Boolean) attributeValue.getValue()).toString();
                    break;

                case Long:
                    stringValue = getSchema().getFormatter(
                            DecimalFormat.class).format(attributeValue.getValue());
                    break;

                case Double:
                    stringValue = getSchema().getFormatter(
                            DecimalFormat.class).format(attributeValue.getValue());
                    break;

                case Date:
                    stringValue = getSchema().getFormatter(
                            SimpleDateFormat.class).format(attributeValue.getValue());
                    break;
            }

            result.add(stringValue);
        }

        return result;
    }

    public abstract void setAttributeValues(
            Set<? extends AbstractAttributeValue> attributeValues);
}
