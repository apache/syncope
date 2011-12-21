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
package org.syncope.core.persistence.validation.attrvalue;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.client.SyncopeConstants;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.AbstractAttrValue;

public abstract class AbstractValidator implements Validator, Serializable {

    private static final long serialVersionUID = -5439345166669502493L;

    /*
     * Logger
     */
    protected static final Logger LOG = LoggerFactory.getLogger(
            AbstractValidator.class);

    protected final AbstractSchema schema;

    public AbstractValidator(final AbstractSchema schema) {
        this.schema = schema;
    }

    @Override
    public <T extends AbstractAttrValue> T getValue(final String value,
            T attributeValue)
            throws ParseException, InvalidAttrValueException {

        attributeValue = parseValue(value, attributeValue);
        doValidate(attributeValue);

        return attributeValue;
    }

    private <T extends AbstractAttrValue> T parseValue(final String value,
            T attributeValue)
            throws ParseException {

        Exception exception = null;

        switch (schema.getType()) {

            case String:
            case Enum:
                attributeValue.setStringValue(value);
                break;

            case Boolean:
                attributeValue.setBooleanValue(Boolean.parseBoolean(value));
                break;

            case Long:
                try {
                    if (schema.getFormatter() == null) {
                        attributeValue.setLongValue(Long.valueOf(value));
                    } else {
                        attributeValue.setLongValue(Long.valueOf(
                                ((DecimalFormat) schema.getFormatter()).parse(
                                value).longValue()));
                    }
                } catch (java.text.ParseException pe) {
                    exception = pe;
                }
                break;

            case Double:
                try {
                    if (schema.getFormatter() == null) {
                        attributeValue.setDoubleValue(Double.valueOf(value));
                    } else {
                        attributeValue.setDoubleValue(Double.valueOf(
                                ((DecimalFormat) schema.getFormatter()).parse(
                                value).doubleValue()));
                    }
                } catch (java.text.ParseException pe) {
                    exception = pe;
                }
                break;

            case Date:
                try {
                    if (schema.getFormatter() == null) {
                        attributeValue.setDateValue(DateUtils.parseDate(
                                value,
                                SyncopeConstants.DATE_PATTERNS));
                    } else {
                        attributeValue.setDateValue(new Date(
                                ((DateFormat) schema.getFormatter()).parse(
                                value).getTime()));
                    }
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

    protected abstract <T extends AbstractAttrValue> void doValidate(
            T attributeValue)
            throws InvalidAttrValueException;
}
