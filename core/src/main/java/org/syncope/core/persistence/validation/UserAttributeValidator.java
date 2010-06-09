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
import org.syncope.core.persistence.beans.UserAttributeSchema;
import org.syncope.core.persistence.beans.UserAttributeValue;
import org.syncope.core.persistence.beans.UserAttributeValueAsBoolean;
import org.syncope.core.persistence.beans.UserAttributeValueAsDate;
import org.syncope.core.persistence.beans.UserAttributeValueAsDouble;
import org.syncope.core.persistence.beans.UserAttributeValueAsLong;
import org.syncope.core.persistence.beans.UserAttributeValueAsString;

public abstract class UserAttributeValidator {

    final protected UserAttributeSchema schema;
    final protected Class userAttributeClass;

    public UserAttributeValidator(UserAttributeSchema schema)
            throws ClassNotFoundException {

        this.schema = schema;
        this.userAttributeClass = Class.forName(
                schema.getType().getClassName());
    }

    public UserAttributeValue getValue(Object value)
            throws ValidationException {

        if (!userAttributeClass.isInstance(value)) {
            throw new ParseException(
                    new ClassCastException("Passed value is instance of "
                    + value.getClass().getName()
                    + ", while this attribute has type "
                    + userAttributeClass.getName()));
        }

        UserAttributeValue result = value instanceof String
                ? parseValue((String) value) : parseValue(value);
        doValidate(result);

        return result;
    }

    protected UserAttributeValue parseValue(String value)
            throws ParseException {

        UserAttributeValue result = null;
        Exception exception = null;

        switch (schema.getType()) {

            case String:
                result = new UserAttributeValueAsString(value);
                break;

            case Boolean:
                result = new UserAttributeValueAsBoolean(
                        Boolean.parseBoolean(value));
                break;

            case Long:
                try {
                    result = new UserAttributeValueAsLong(
                            Long.valueOf(schema.getFormatter(
                            DecimalFormat.class).parse(value).longValue()));
                } catch (java.text.ParseException pe) {
                    exception = pe;
                }
                break;

            case Double:
                try {
                    result = new UserAttributeValueAsDouble(
                            Double.valueOf(schema.getFormatter(
                            DecimalFormat.class).parse(value).doubleValue()));
                } catch (java.text.ParseException pe) {
                    exception = pe;
                }
                break;

            case Date:
                try {
                    result = new UserAttributeValueAsDate(
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

    protected UserAttributeValue parseValue(Object value)
            throws ParseException {

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

    protected abstract void doValidate(
            UserAttributeValue userAttributeValue)
            throws ValidationFailedException;
}
