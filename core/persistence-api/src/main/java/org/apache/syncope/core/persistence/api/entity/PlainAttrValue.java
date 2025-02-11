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
package org.apache.syncope.core.persistence.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.attrvalue.ParsingValidationException;
import org.apache.syncope.core.persistence.api.utils.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlainAttrValue implements Serializable {

    private static final long serialVersionUID = -9141923816611244785L;

    protected static final Logger LOG = LoggerFactory.getLogger(PlainAttrValue.class);

    private static final Pattern SPRING_ENV_PROPERTY = Pattern.compile("^\\$\\{.*\\}$");

    @JsonIgnore
    @NotNull
    private PlainAttr attr;

    private String stringValue;

    private OffsetDateTime dateValue;

    private Boolean booleanValue;

    private Long longValue;

    private Double doubleValue;

    private byte[] binaryValue;

    public PlainAttr getAttr() {
        return attr;
    }

    public void setAttr(final PlainAttr attr) {
        this.attr = attr;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(final Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public OffsetDateTime getDateValue() {
        return dateValue;
    }

    public void setDateValue(final OffsetDateTime dateValue) {
        this.dateValue = dateValue;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(final Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public Long getLongValue() {
        return longValue;
    }

    public void setLongValue(final Long longValue) {
        this.longValue = longValue;
    }

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

    public void setStringValue(final String stringValue) {
        this.stringValue = stringValue;
    }

    public byte[] getBinaryValue() {
        return binaryValue;
    }

    public void setBinaryValue(final byte[] binaryValue) {
        this.binaryValue = ArrayUtils.clone(binaryValue);
    }

    protected String getSecretKey(final PlainSchema schema) {
        return SPRING_ENV_PROPERTY.matcher(schema.getSecretKey()).matches()
                ? ApplicationContextProvider.getApplicationContext().getEnvironment().
                        getProperty(StringUtils.substringBetween(schema.getSecretKey(), "${", "}"))
                : schema.getSecretKey();
    }

    public void parseValue(final PlainSchema schema, final String value) {
        Exception exception = null;

        switch (schema.getType()) {

            case Boolean:
                this.setBooleanValue(Boolean.valueOf(value));
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
                            : FormatUtils.parseDate(value, schema.getConversionPattern()));
                } catch (Exception pe) {
                    exception = pe;
                }
                break;

            case Encrypted:
                try {
                    this.setStringValue(ApplicationContextProvider.getApplicationContext().
                            getBean(EncryptorManager.class).getInstance(getSecretKey(schema)).
                            encode(value, schema.getCipherAlgorithm()));
                } catch (Exception pe) {
                    exception = pe;
                }
                break;

            case Binary:
                this.setBinaryValue(Base64.getDecoder().decode(value));
                break;

            case String:
            case Dropdown:
            case Enum:
            default:
                this.setStringValue(value);
        }

        if (exception != null) {
            throw new ParsingValidationException(
                    "While trying to parse '" + value + "' as " + schema.getKey(), exception);
        }
    }

    @JsonIgnore
    @SuppressWarnings("unchecked")
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
            case Dropdown:
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
                        : FormatUtils.format(getDateValue(), schema.getConversionPattern());
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
                        result = ApplicationContextProvider.getApplicationContext().getBean(EncryptorManager.class).
                                getInstance(getSecretKey(schema)).decode(getStringValue(), schema.getCipherAlgorithm());
                    } catch (Exception e) {
                        LOG.error("Could not decode encrypted value {} for schema {}", getStringValue(), schema, e);
                        result = getStringValue();
                    }
                }
                break;

            case Dropdown:
            case Enum:
            case String:
            default:
                result = getStringValue();
        }

        return result;
    }

    @JsonIgnore
    public String getValueAsString() {
        PlainSchema schema = getAttr() == null || getAttr().getSchema() == null
                ? null
                : getAttr().fetchPlainSchema();
        AttrSchemaType type = schema == null || schema.getType() == null
                ? AttrSchemaType.String
                : schema.getType();

        return getValueAsString(type, schema);
    }

    @JsonIgnore
    public String getValueAsString(final AttrSchemaType type) {
        return getValueAsString(
                type,
                getAttr() == null || getAttr().getSchema() == null
                ? null
                : getAttr().fetchPlainSchema());
    }

    @JsonIgnore
    public String getValueAsString(final PlainSchema schema) {
        return getValueAsString(schema.getType(), schema);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(stringValue).
                append(dateValue).
                append(booleanValue).
                append(longValue).
                append(doubleValue).
                append(binaryValue).
                build();
    }
}
