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

import java.util.ArrayList;
import java.util.List;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import org.syncope.core.persistence.validation.ParseException;
import org.syncope.core.persistence.validation.ValidationFailedException;

@MappedSuperclass
public abstract class AbstractAttribute extends AbstractBaseBean {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    public Long getId() {
        return id;
    }

    public <T extends AbstractAttributeValue> T addValue(String value,
            T attributeValue) throws ParseException, ValidationFailedException {

        T actualValue = getSchema().getValidator().getValue(value,
                attributeValue);
        actualValue.setAttribute(this);

        if (!getSchema().isMultivalue()) {
            getAttributeValues().clear();
        }

        addAttributeValue(actualValue);

        return actualValue;
    }

    public abstract <T extends AbstractAttributable> T getOwner();

    public abstract <T extends AbstractAttributable> void setOwner(T owner);

    public abstract <T extends AbstractSchema> T getSchema();

    public abstract <T extends AbstractSchema> void setSchema(T schema);

    public abstract <T extends AbstractAttributeValue> boolean addAttributeValue(
            T attributeValue);

    public abstract <T extends AbstractAttributeValue> boolean removeAttributeValue(
            T attributeValue);

    public <T extends AbstractAttributeValue> List<String> getAttributeValuesAsStrings() {
        List<T> values = getAttributeValues();

        List<String> result = new ArrayList<String>(values.size());
        for (T attributeValue : values) {
            result.add(attributeValue.getValueAsString());
        }

        return result;
    }

    public abstract <T extends AbstractAttributeValue> List<T> getAttributeValues();

    public abstract <T extends AbstractAttributeValue> void setAttributeValues(
            List<T> attributeValues);
}
