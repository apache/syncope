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

import java.lang.reflect.Constructor;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.apache.syncope.common.types.AttributeSchemaType;
import org.apache.syncope.core.persistence.validation.attrvalue.AbstractValidator;
import org.apache.syncope.core.persistence.validation.attrvalue.BasicValidator;
import org.apache.syncope.core.persistence.validation.entity.SchemaCheck;
import org.apache.syncope.core.persistence.validation.entity.SchemaNameCheck;

@MappedSuperclass
@SchemaCheck
@SchemaNameCheck
public abstract class AbstractSchema extends AbstractBaseBean {

    public static String enumValuesSeparator = ";";

    private static final long serialVersionUID = -8621028596062054739L;

    @Id
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AttributeSchemaType type;

    @Column(nullable = false)
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

    @Transient
    private AbstractValidator validator;

    public AbstractSchema() {
        super();

        type = AttributeSchemaType.String;
        mandatoryCondition = Boolean.FALSE.toString();
        multivalue = getBooleanAsInteger(false);
        uniqueConstraint = getBooleanAsInteger(false);
        readonly = getBooleanAsInteger(false);
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public AttributeSchemaType getType() {
        return type;
    }

    public void setType(final AttributeSchemaType type) {
        this.type = type;
    }

    public String getMandatoryCondition() {
        return mandatoryCondition;
    }

    public void setMandatoryCondition(final String mandatoryCondition) {
        this.mandatoryCondition = mandatoryCondition;
    }

    public boolean isMultivalue() {
        return isBooleanAsInteger(multivalue);
    }

    public void setMultivalue(boolean multivalue) {
        this.multivalue = getBooleanAsInteger(multivalue);
    }

    public boolean isUniqueConstraint() {
        return isBooleanAsInteger(uniqueConstraint);
    }

    public void setUniqueConstraint(final boolean uniquevalue) {
        this.uniqueConstraint = getBooleanAsInteger(uniquevalue);
    }

    public boolean isReadonly() {
        return isBooleanAsInteger(readonly);
    }

    public void setReadonly(final boolean readonly) {
        this.readonly = getBooleanAsInteger(readonly);
    }

    public AbstractValidator getValidator() {
        if (validator != null) {
            return validator;
        }

        if (getValidatorClass() != null && getValidatorClass().length() > 0) {
            try {
                Constructor validatorConstructor = Class.forName(getValidatorClass()).getConstructor(
                        new Class[] { getClass().getSuperclass() });
                validator = (AbstractValidator) validatorConstructor.newInstance(this);
            } catch (Exception e) {
                LOG.error("Could not instantiate validator of type " + getValidatorClass()
                        + ", reverting to " + BasicValidator.class.getSimpleName(), e);
            }
        }

        if (validator == null) {
            validator = new BasicValidator(this);
        }

        return validator;
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

    public void setEnumerationKeys(String enumerationKeys) {
        this.enumerationKeys = enumerationKeys;
    }

    public String getConversionPattern() {
        if (!getType().isConversionPatternNeeded()) {
            LOG.debug("Conversion pattern is not needed: {}'s type is {}", this, getType());
        }

        return conversionPattern;
    }

    public void setConversionPattern(final String conversionPattern) {
        if (!getType().isConversionPatternNeeded()) {
            LOG.warn("Conversion pattern will be ignored: " + "this attribute type is " + getType());
        }

        this.conversionPattern = conversionPattern;
    }
}
