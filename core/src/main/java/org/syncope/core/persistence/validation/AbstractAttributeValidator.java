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
package org.syncope.core.persistence.validation;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.AbstractAttributeValue;

public abstract class AbstractAttributeValidator implements AttributeValidator {

    final protected AbstractSchema schema;

    public AbstractAttributeValidator(AbstractSchema schema) {

        this.schema = schema;
    }

    @Override
    public <T extends AbstractAttributeValue> T getValue(String value,
            T attributeValue)
            throws ParseException, ValidationFailedException {

        attributeValue = parseValue(value, attributeValue);
        doValidate(attributeValue);

        return attributeValue;
    }

    private <T extends AbstractAttributeValue> T parseValue(String value,
            T attributeValue) throws ParseException {
        Exception exception = null;

        switch (schema.getType()) {

            case String:
                attributeValue.setStringValue(value);
                break;

            case Boolean:
                attributeValue.setBooleanValue(Boolean.parseBoolean(value));
                break;

            case Long:
                try {
                    attributeValue.setLongValue(Long.valueOf(
                            schema.getFormatter(DecimalFormat.class).parse(
                            value).longValue()));
                } catch (java.text.ParseException pe) {
                    exception = pe;
                }
                break;

            case Double:
                try {
                    attributeValue.setDoubleValue(Double.valueOf(
                            schema.getFormatter(DecimalFormat.class).parse(
                            value).doubleValue()));
                } catch (java.text.ParseException pe) {
                    exception = pe;
                }
                break;

            case Date:
                try {
                    final SimpleDateFormat formatter =
                            schema.getFormatter(SimpleDateFormat.class);
                    formatter.setLenient(false);
                    attributeValue.setDateValue(
                            new Date(formatter.parse(value).getTime()));
                } catch (java.text.ParseException pe) {
                    exception = pe;
                }
                break;
        }

        if (exception != null) {
            throw new ParseException("While trying to parse '" + value + "'",
                    exception);
        }

        return attributeValue;
    }

    protected abstract <T extends AbstractAttributeValue> void doValidate(
            T attributeValue) throws ValidationFailedException;
}
