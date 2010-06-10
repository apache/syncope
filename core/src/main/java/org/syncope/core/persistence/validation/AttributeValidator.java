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
import org.syncope.core.persistence.beans.AttributeSchema;
import org.syncope.core.persistence.beans.AttributeValue;
import org.syncope.core.persistence.beans.AttributeValueAsBoolean;
import org.syncope.core.persistence.beans.AttributeValueAsDate;
import org.syncope.core.persistence.beans.AttributeValueAsDouble;
import org.syncope.core.persistence.beans.AttributeValueAsLong;
import org.syncope.core.persistence.beans.AttributeValueAsString;

public abstract class AttributeValidator {

    final protected AttributeSchema schema;
    final protected Class attributeClass;

    public AttributeValidator(AttributeSchema schema)
            throws ClassNotFoundException {

        this.schema = schema;
        this.attributeClass = Class.forName(
                schema.getType().getClassName());
    }

    public AttributeValue getValue(Object value)
            throws ValidationException {

        if (!attributeClass.isInstance(value)) {
            throw new ParseException(
                    new ClassCastException("Passed value is instance of "
                    + value.getClass().getName()
                    + ", while this attribute has type "
                    + attributeClass.getName()));
        }

        AttributeValue result = value instanceof String
                ? parseValue((String) value) : parseValue(value);
        doValidate(result);

        return result;
    }

    protected AttributeValue parseValue(String value)
            throws ParseException {

        AttributeValue result = null;
        Exception exception = null;

        switch (schema.getType()) {

            case String:
                result = new AttributeValueAsString(value);
                break;

            case Boolean:
                result = new AttributeValueAsBoolean(
                        Boolean.parseBoolean(value));
                break;

            case Long:
                try {
                    result = new AttributeValueAsLong(
                            Long.valueOf(schema.getFormatter(
                            DecimalFormat.class).parse(value).longValue()));
                } catch (java.text.ParseException pe) {
                    exception = pe;
                }
                break;

            case Double:
                try {
                    result = new AttributeValueAsDouble(
                            Double.valueOf(schema.getFormatter(
                            DecimalFormat.class).parse(value).doubleValue()));
                } catch (java.text.ParseException pe) {
                    exception = pe;
                }
                break;

            case Date:
                try {
                    result = new AttributeValueAsDate(
                            new Date(schema.getFormatter(
                            SimpleDateFormat.class).parse(value).getTime()));
                } catch (java.text.ParseException pe) {
                    exception = pe;
                }
                break;
        }

        if (exception != null) {
            throw new ParseException(
                    "While trying to parse '" + value + "'", exception);
        }

        return result;
    }

    protected AttributeValue parseValue(Object value)
            throws ParseException {

        AttributeValue result = null;

        switch (schema.getType()) {

            case String:
                result = new AttributeValueAsString((String) value);
                break;

            case Boolean:
                result = new AttributeValueAsBoolean((Boolean) value);
                break;

            case Long:
                result = new AttributeValueAsLong((Long) value);
                break;

            case Double:
                result = new AttributeValueAsDouble((Double) value);
                break;

            case Date:
                result = new AttributeValueAsDate((Date) value);
                break;
        }

        return result;
    }

    protected abstract void doValidate(
            AttributeValue attributeValue)
            throws ValidationFailedException;
}
