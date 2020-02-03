/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.persistence.jpa.entity;

import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.provisioning.api.utils.FormatUtils;
import org.apache.syncope.core.persistence.api.attrvalue.validation.ParsingValidationException;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.jpa.validation.entity.PlainAttrValueCheck;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.security.Encryptor;

@MappedSuperclass
@PlainAttrValueCheck
public abstract class AbstractPlainAttrValue extends AbstractGeneratedKeyEntity implements PlainAttrValue {

    private static final long serialVersionUID = -9141923816611244785L;

    private static final Pattern SPRING_ENV_PROPERTY = Pattern.compile("^\\$\\{.*\\}$");

    private String stringValue;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dateValue;

    private Boolean booleanValue;

    private Long longValue;

    private Double doubleValue;

    @Lob
    private byte[] binaryValue;

    @Override
    public Boolean getBooleanValue() {
        return booleanValue;
    }

    @Override
    public void setBooleanValue(final Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    @Override
    public Date getDateValue() {
        return Optional.ofNullable(dateValue).map(value -> new Date(value.getTime())).orElse(null);
    }

    @Override
    public void setDateValue(final Date dateValue) {
        this.dateValue = Optional.ofNullable(dateValue).map(value -> new Date(value.getTime())).orElse(null);
    }

    @Override
    public Double getDoubleValue() {
        return doubleValue;
    }

    @Override
    public void setDoubleValue(final Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    @Override
    public Long getLongValue() {
        return longValue;
    }

    @Override
    public void setLongValue(final Long longValue) {
        this.longValue = longValue;
    }

    @Override
    public String getStringValue() {
        // workaround for Oracle DB considering empty string values as NULL (SYNCOPE-664)
        return dateValue == null
                && booleanValue == null
                && longValue == null
                && doubleValue == null
                && binaryValue == null
                && stringValue == null
                        ? StringUtils.EMPTY
                        : stringValue;
    }

    @Override
    public void setStringValue(final String stringValue) {
        this.stringValue = stringValue;
    }

    @Override
    public byte[] getBinaryValue() {
        return binaryValue;
    }

    @Override
    public void setBinaryValue(final byte[] binaryValue) {
        this.binaryValue = ArrayUtils.clone(binaryValue);
    }

    protected String getSecretKey(final PlainSchema schema) {
        return SPRING_ENV_PROPERTY.matcher(schema.getSecretKey()).matches()
                ? ApplicationContextProvider.getApplicationContext().getEnvironment().
                        getProperty(StringUtils.substringBetween(schema.getSecretKey(), "${", "}"))
                : schema.getSecretKey();
    }

    @Override
    public void parseValue(final PlainSchema schema, final String value) {
        Exception exception = null;

        switch (schema.getType()) {

            case Boolean:
                this.setBooleanValue(Boolean.parseBoolean(value));
                break;

            case Long:
                try {
                this.setLongValue(schema.getConversionPattern() == null
                        ? Long.valueOf(value)
                        : FormatUtils.parseNumber(value, schema.getConversionPattern()).longValue());
            } catch (Exception pe) {
                exception = pe;
            }
            break;

            case Double:
                try {
                this.setDoubleValue(schema.getConversionPattern() == null
                        ? Double.valueOf(value)
                        : FormatUtils.parseNumber(value, schema.getConversionPattern()).doubleValue());
            } catch (Exception pe) {
                exception = pe;
            }
            break;

            case Date:
                try {
                this.setDateValue(schema.getConversionPattern() == null
                        ? FormatUtils.parseDate(value)
                        : new Date(FormatUtils.parseDate(value, schema.getConversionPattern()).getTime()));
            } catch (Exception pe) {
                exception = pe;
            }
            break;

            case Encrypted:
                try {
                this.setStringValue(Encryptor.getInstance(getSecretKey(schema)).
                        encode(value, schema.getCipherAlgorithm()));
            } catch (Exception pe) {
                exception = pe;
            }
            break;

            case Binary:
                this.setBinaryValue(Base64.getDecoder().decode(value));
                break;

            case String:
            case Enum:
            default:
                this.setStringValue(value);
        }

        if (exception != null) {
            throw new ParsingValidationException(
                    "While trying to parse '" + value + "' as " + schema.getKey(), exception);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getValue() {
        return (T) (booleanValue != null
                ? getBooleanValue()
                : dateValue != null
                        ? getDateValue()
                        : doubleValue != null
                                ? getDoubleValue()
                                : longValue != null
                                        ? getLongValue()
                                        : binaryValue != null
                                                ? getBinaryValue()
                                                : getStringValue());
    }

    private Object getValue(final AttrSchemaType type) {
        Object value;
        switch (type) {

            case Boolean:
                value = getBooleanValue();
                break;

            case Long:
                value = getLongValue();
                break;

            case Double:
                value = getDoubleValue();
                break;

            case Date:
                value = getDateValue();
                break;

            case Binary:
                value = getBinaryValue();
                break;

            case String:
            case Enum:
            case Encrypted:
                value = getStringValue();
                break;

            default:
                value = null;
        }

        return value;
    }

    private String getValueAsString(final AttrSchemaType type, final PlainSchema schema) {
        if (getValue(type) == null) {
            LOG.warn("Could not find expected value for type {} in {}, reverting to getValue().toString()", type, this);

            Object value = getValue();
            return Optional.ofNullable(value).map(Object::toString).orElse(null);
        }

        String result;
        switch (type) {

            case Boolean:
                result = getBooleanValue().toString();
                break;

            case Long:
                result = schema == null || schema.getConversionPattern() == null
                        ? getLongValue().toString()
                        : FormatUtils.format(getLongValue(), schema.getConversionPattern());
                break;

            case Double:
                result = schema == null || schema.getConversionPattern() == null
                        ? getDoubleValue().toString()
                        : FormatUtils.format(getDoubleValue(), schema.getConversionPattern());
                break;

            case Date:
                result = schema == null || schema.getConversionPattern() == null
                        ? FormatUtils.format(getDateValue())
                        : FormatUtils.format(getDateValue(), false, schema.getConversionPattern());
                break;

            case Binary:
                result = Base64.getEncoder().encodeToString(getBinaryValue());
                break;

            case Encrypted:
                if (schema == null
                        || !SyncopeConstants.ENCRYPTED_DECODE_CONVERSION_PATTERN.equals(schema.getConversionPattern())
                        || !schema.getCipherAlgorithm().isInvertible()) {

                    result = getStringValue();
                } else {
                    try {
                        result = Encryptor.getInstance(getSecretKey(schema)).
                                decode(getStringValue(), schema.getCipherAlgorithm());
                    } catch (Exception e) {
                        LOG.error("Could not decode encrypted value {} for schema {}", getStringValue(), schema, e);
                        result = getStringValue();
                    }
                }
                break;

            case String:
            case Enum:
            default:
                result = getStringValue();
        }

        return result;
    }

    @Override
    public String getValueAsString() {
        PlainSchema schema = getAttr() == null || getAttr().getSchema() == null
                ? null
                : getAttr().getSchema();
        AttrSchemaType type = schema == null || schema.getType() == null
                ? AttrSchemaType.String
                : getAttr().getSchema().getType();

        return getValueAsString(type, schema);
    }

    @Override
    public String getValueAsString(final AttrSchemaType type) {
        return getValueAsString(
                type,
                getAttr() == null || getAttr().getSchema() == null ? null : getAttr().getSchema());
    }

    @Override
    public String getValueAsString(final PlainSchema schema) {
        return getValueAsString(schema.getType(), schema);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(getKey()).
                append(stringValue).
                append(dateValue).
                append(booleanValue).
                append(longValue).
                append(doubleValue).
                append(binaryValue).
                build();
    }
}
