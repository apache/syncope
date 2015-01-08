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

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;

@XmlRootElement(name = "schema")
@XmlType
public class PlainSchemaTO extends AbstractSchemaTO {

    private static final long serialVersionUID = -8133983392476990308L;

    private AttrSchemaType type = AttrSchemaType.String;

    private String mandatoryCondition;

    private boolean multivalue;

    private boolean uniqueConstraint;

    private boolean readonly;

    private String conversionPattern;

    private String validatorClass;

    private String enumerationValues;

    private String enumerationKeys;

    private String secretKey;

    private CipherAlgorithm cipherAlgorithm;

    private String mimeType;

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

    public String getValidatorClass() {
        return validatorClass;
    }

    public void setValidatorClass(final String validatorClass) {
        this.validatorClass = validatorClass;
    }

    public String getEnumerationValues() {
        return enumerationValues;
    }

    public void setEnumerationValues(final String enumerationValues) {
        this.enumerationValues = enumerationValues;
    }

    public String getEnumerationKeys() {
        return enumerationKeys;
    }

    public void setEnumerationKeys(final String enumerationKeys) {
        this.enumerationKeys = enumerationKeys;
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

}
