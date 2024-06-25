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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;

@Schema(allOf = { SchemaTO.class })
public class PlainSchemaTO extends SchemaTO {

    private static final long serialVersionUID = -8133983392476990308L;

    private AttrSchemaType type = AttrSchemaType.String;

    private String mandatoryCondition;

    private boolean multivalue;

    private boolean uniqueConstraint;

    private boolean readonly;

    private String conversionPattern;

    private String validator;

    private Map<String, String> enumValues = new HashMap<>();

    private String dropdownValueProvider;

    private String secretKey;

    private CipherAlgorithm cipherAlgorithm;

    private String mimeType;

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "org.apache.syncope.common.lib.to.PlainSchemaTO")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    public String getConversionPattern() {
        return conversionPattern;
    }

    public void setConversionPattern(final String conversionPattern) {
        this.conversionPattern = conversionPattern;
    }

    public String getMandatoryCondition() {
        return StringUtils.isNotBlank(mandatoryCondition)
                ? mandatoryCondition
                : "false";
    }

    public void setMandatoryCondition(final String mandatoryCondition) {
        this.mandatoryCondition = mandatoryCondition;
    }

    public boolean isMultivalue() {
        return multivalue;
    }

    public void setMultivalue(final boolean multivalue) {
        this.multivalue = multivalue;
    }

    public boolean isUniqueConstraint() {
        return uniqueConstraint;
    }

    public void setUniqueConstraint(final boolean uniqueConstraint) {
        this.uniqueConstraint = uniqueConstraint;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(final boolean readonly) {
        this.readonly = readonly;
    }

    public AttrSchemaType getType() {
        return type;
    }

    public void setType(final AttrSchemaType type) {
        this.type = type;
    }

    public String getValidator() {
        return validator;
    }

    public void setValidator(final String validator) {
        this.validator = validator;
    }

    public Map<String, String> getEnumValues() {
        return enumValues;
    }

    public String getDropdownValueProvider() {
        return dropdownValueProvider;
    }

    public void setDropdownValueProvider(final String dropdownValueProvider) {
        this.dropdownValueProvider = dropdownValueProvider;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(final String secretKey) {
        this.secretKey = secretKey;
    }

    public CipherAlgorithm getCipherAlgorithm() {
        return cipherAlgorithm;
    }

    public void setCipherAlgorithm(final CipherAlgorithm cipherAlgorithm) {
        this.cipherAlgorithm = cipherAlgorithm;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(type).
                append(mandatoryCondition).
                append(multivalue).
                append(uniqueConstraint).
                append(readonly).
                append(conversionPattern).
                append(validator).
                append(enumValues).
                append(dropdownValueProvider).
                append(secretKey).
                append(cipherAlgorithm).
                append(mimeType).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PlainSchemaTO other = (PlainSchemaTO) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(type, other.type).
                append(mandatoryCondition, other.mandatoryCondition).
                append(multivalue, other.multivalue).
                append(uniqueConstraint, other.uniqueConstraint).
                append(readonly, other.readonly).
                append(conversionPattern, other.conversionPattern).
                append(validator, other.validator).
                append(enumValues, other.enumValues).
                append(dropdownValueProvider, other.dropdownValueProvider).
                append(secretKey, other.secretKey).
                append(cipherAlgorithm, other.cipherAlgorithm).
                append(mimeType, other.mimeType).
                build();
    }
}
