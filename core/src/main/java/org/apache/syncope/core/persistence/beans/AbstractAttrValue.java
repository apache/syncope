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
package org.apache.syncope.core.persistence.beans;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.types.AttributeSchemaType;
import org.apache.syncope.core.persistence.validation.attrvalue.InvalidAttrValueException;
import org.apache.syncope.core.persistence.validation.attrvalue.ParsingValidationException;
import org.apache.syncope.core.persistence.validation.entity.AttrValueCheck;
import org.apache.syncope.core.util.DataFormat;
import org.apache.syncope.core.util.Encryptor;
import org.springframework.security.crypto.codec.Base64;

@MappedSuperclass
@AttrValueCheck
public abstract class AbstractAttrValue extends AbstractBaseBean {

    private static final long serialVersionUID = -9141923816611244785L;

    private String stringValue;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dateValue;

    @Basic
    @Min(0)
    @Max(1)
    private Integer booleanValue;

    private Long longValue;

    private Double doubleValue;

    @Lob
    private byte[] binaryValue;

    public abstract Long getId();

    public Boolean getBooleanValue() {
        return booleanValue == null
                ? null
                : isBooleanAsInteger(booleanValue);
    }

    public void setBooleanValue(final Boolean booleanValue) {
        this.booleanValue = booleanValue == null
                ? null
                : getBooleanAsInteger(booleanValue);
    }

    public Date getDateValue() {
        return dateValue == null
                ? null
                : new Date(dateValue.getTime());
    }

    public void setDateValue(final Date dateValue) {
        this.dateValue = dateValue == null
                ? null
                : new Date(dateValue.getTime());
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
        return stringValue;
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

    public void parseValue(final AbstractNormalSchema schema, final String value)
            throws ParsingValidationException {

        Exception exception = null;

        switch (schema.getType()) {

            case Boolean:
                this.setBooleanValue(Boolean.parseBoolean(value));
                break;

            case Long:
                try {
                    this.setLongValue(schema.getConversionPattern() == null
                            ? Long.valueOf(value)
                            : DataFormat.parseNumber(value, schema.getConversionPattern()).longValue());
                } catch (Exception pe) {
                    exception = pe;
                }
                break;

            case Double:
                try {
                    this.setDoubleValue(schema.getConversionPattern() == null
                            ? Double.valueOf(value)
                            : DataFormat.parseNumber(value, schema.getConversionPattern()).doubleValue());
                } catch (Exception pe) {
                    exception = pe;
                }
                break;

            case Date:
                try {
                    this.setDateValue(schema.getConversionPattern() == null
                            ? DataFormat.parseDate(value)
                            : new Date(DataFormat.parseDate(value, schema.getConversionPattern()).getTime()));
                } catch (Exception pe) {
                    exception = pe;
                }
                break;

            case Encrypted:
                try {
                    this.setStringValue(Encryptor.getInstance(schema.getSecretKey()).
                            encode(value, schema.getCipherAlgorithm()));
                } catch (Exception pe) {
                    exception = pe;
                }
                break;

            case Binary:
                try {
                    this.setBinaryValue(Base64.decode(value.getBytes(SyncopeConstants.DEFAULT_ENCODING)));
                } catch (UnsupportedEncodingException pe) {
                    exception = pe;
                }
                break;

            case String:
            case Enum:
            default:
                this.setStringValue(value);
        }

        if (exception != null) {
            throw new ParsingValidationException("While trying to parse '" + value + "' as " + schema.getName(),
                    exception);
        }
    }

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
                : stringValue);
    }

    public String getValueAsString() {
        final AttributeSchemaType type = getAttribute() == null || getAttribute().getSchema() == null
                || getAttribute().getSchema().getType() == null
                ? AttributeSchemaType.String
                : getAttribute().getSchema().getType();

        return getValueAsString(type);
    }

    public String getValueAsString(final AttributeSchemaType type) {
        Exception exception = null;

        String result = null;

        switch (type) {

            case Boolean:
                result = getBooleanValue().toString();
                break;

            case Long:
                result = getAttribute() == null || getAttribute().getSchema() == null
                        || getAttribute().getSchema().getConversionPattern() == null
                        ? getLongValue().toString()
                        : DataFormat.format(getLongValue(), getAttribute().getSchema().getConversionPattern());
                break;

            case Double:
                result = getAttribute() == null || getAttribute().getSchema() == null
                        || getAttribute().getSchema().getConversionPattern() == null
                        ? getDoubleValue().toString()
                        : DataFormat.format(getDoubleValue(), getAttribute().getSchema().getConversionPattern());
                break;

            case Date:
                result = getAttribute() == null || getAttribute().getSchema() == null
                        || getAttribute().getSchema().getConversionPattern() == null
                        ? DataFormat.format(getDateValue())
                        : DataFormat.format(getDateValue(), false, getAttribute().getSchema().getConversionPattern());
                break;

            case Binary:
                try {
                    result = new String(Base64.encode(getBinaryValue()), SyncopeConstants.DEFAULT_ENCODING);
                } catch (UnsupportedEncodingException fe) {
                    exception = fe;
                }
                break;

            case String:
            case Enum:
            case Encrypted:
            default:
                result = getStringValue();
                break;
        }

        if (exception != null) {
            throw new InvalidAttrValueException("While trying to format '" + getValue() + "' as " + type,
                    exception);
        }

        return result;
    }

    public abstract <T extends AbstractAttr> T getAttribute();

    public abstract <T extends AbstractAttr> void setAttribute(T attribute);

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
