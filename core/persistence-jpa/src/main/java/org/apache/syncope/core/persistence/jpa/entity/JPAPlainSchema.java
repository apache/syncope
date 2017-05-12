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

import java.lang.reflect.Constructor;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.attrvalue.validation.Validator;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.jpa.attrvalue.validation.BasicValidator;
import org.apache.syncope.core.persistence.jpa.validation.entity.PlainSchemaCheck;

@Entity
@Table(name = JPAPlainSchema.TABLE)
@PrimaryKeyJoinColumn(name = "id", referencedColumnName = "id")
@PlainSchemaCheck
public class JPAPlainSchema extends AbstractSchema implements PlainSchema {

    private static final long serialVersionUID = -8621028596062054739L;

    public static final String TABLE = "PlainSchema";

    @OneToOne(fetch = FetchType.EAGER)
    private JPAAnyTypeClass anyTypeClass;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AttrSchemaType type;

    @NotNull
    private String mandatoryCondition;

    @Basic
    @Min(0)
    @Max(1)
    private Integer multivalue;

    @Basic
    @Min(0)
    @Max(1)
    private Integer uniqueConstraint;

    @Basic
    @Min(0)
    @Max(1)
    private Integer readonly;

    @Column(nullable = true)
    private String conversionPattern;

    @Column(nullable = true)
    private String validatorClass;

    @Column(nullable = true)
    @Lob
    private String enumerationValues;

    @Column(nullable = true)
    @Lob
    private String enumerationKeys;

    @Column(nullable = true)
    private String secretKey;

    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    private CipherAlgorithm cipherAlgorithm;

    @Column(nullable = true)
    private String mimeType;

    @Transient
    private Validator validator;

    public JPAPlainSchema() {
        super();

        type = AttrSchemaType.String;
        mandatoryCondition = Boolean.FALSE.toString();
        multivalue = getBooleanAsInteger(false);
        uniqueConstraint = getBooleanAsInteger(false);
        readonly = getBooleanAsInteger(false);
    }

    @Override
    public AnyTypeClass getAnyTypeClass() {
        return anyTypeClass;
    }

    @Override
    public void setAnyTypeClass(final AnyTypeClass anyTypeClass) {
        checkType(anyTypeClass, JPAAnyTypeClass.class);
        this.anyTypeClass = (JPAAnyTypeClass) anyTypeClass;
    }

    @Override
    public AttrSchemaType getType() {
        return type;
    }

    @Override
    public void setType(final AttrSchemaType type) {
        this.type = type;
    }

    @Override
    public String getMandatoryCondition() {
        return mandatoryCondition;
    }

    @Override
    public void setMandatoryCondition(final String condition) {
        this.mandatoryCondition = condition;
    }

    @Override
    public boolean isMultivalue() {
        return isBooleanAsInteger(multivalue);
    }

    @Override
    public void setMultivalue(final boolean multivalue) {
        this.multivalue = getBooleanAsInteger(multivalue);
    }

    @Override
    public boolean isUniqueConstraint() {
        return isBooleanAsInteger(uniqueConstraint);
    }

    @Override
    public void setUniqueConstraint(final boolean uniquevalue) {
        this.uniqueConstraint = getBooleanAsInteger(uniquevalue);
    }

    @Override
    public boolean isReadonly() {
        return isBooleanAsInteger(readonly);
    }

    @Override
    public void setReadonly(final boolean readonly) {
        this.readonly = getBooleanAsInteger(readonly);
    }

    @Override
    public Validator getValidator() {
        if (validator != null) {
            return validator;
        }

        if (getValidatorClass() != null && getValidatorClass().length() > 0) {
            try {
                Constructor<?> validatorConstructor = Class.forName(getValidatorClass()).
                        getConstructor(new Class<?>[] { PlainSchema.class });
                validator = (Validator) validatorConstructor.newInstance(this);
            } catch (Exception e) {
                LOG.error("Could not instantiate validator of type {}, reverting to {}",
                        getValidatorClass(), BasicValidator.class.getSimpleName(), e);
            }
        }

        if (validator == null) {
            validator = new BasicValidator(this);
        }

        return validator;
    }

    @Override
    public String getValidatorClass() {
        return validatorClass;
    }

    @Override
    public void setValidatorClass(final String validatorClass) {
        this.validatorClass = validatorClass;
    }

    @Override
    public String getEnumerationValues() {
        return enumerationValues;
    }

    @Override
    public void setEnumerationValues(final String enumerationValues) {
        this.enumerationValues = enumerationValues;
    }

    @Override
    public String getEnumerationKeys() {
        return enumerationKeys;
    }

    @Override
    public void setEnumerationKeys(final String enumerationKeys) {
        this.enumerationKeys = enumerationKeys;
    }

    @Override
    public String getConversionPattern() {
        return conversionPattern;
    }

    @Override
    public void setConversionPattern(final String conversionPattern) {
        this.conversionPattern = conversionPattern;
    }

    @Override
    public String getSecretKey() {
        return secretKey;
    }

    @Override
    public void setSecretKey(final String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public CipherAlgorithm getCipherAlgorithm() {
        return cipherAlgorithm;
    }

    @Override
    public void setCipherAlgorithm(final CipherAlgorithm cipherAlgorithm) {
        this.cipherAlgorithm = cipherAlgorithm;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

}
