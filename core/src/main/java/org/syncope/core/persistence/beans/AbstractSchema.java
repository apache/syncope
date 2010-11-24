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
package org.syncope.core.persistence.beans;

import java.lang.reflect.Constructor;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.persistence.Basic;
import static javax.persistence.EnumType.STRING;

import javax.persistence.Column;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import org.syncope.core.persistence.validation.BasicAttributeValidator;
import org.syncope.core.persistence.validation.AbstractAttributeValidator;
import org.syncope.types.SchemaValueType;

@MappedSuperclass
public abstract class AbstractSchema extends AbstractBaseBean {

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
            new ThreadLocal<SimpleDateFormat>() {

                @Override
                protected SimpleDateFormat initialValue() {
                    return new SimpleDateFormat();
                }
            };

    private static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT =
            new ThreadLocal<DecimalFormat>() {

                @Override
                protected DecimalFormat initialValue() {
                    return new DecimalFormat();
                }
            };

    @Id
    private String name;

    @Column(nullable = false)
    @Enumerated(STRING)
    private SchemaValueType type;

    /**
     * Specify if the attribute should be stored on the local repository.
     */
    @Basic
    private Integer virtual;

    @Column(nullable = false)
    private String mandatoryCondition;

    @Basic
    private Integer multivalue;

    @Basic
    private Integer uniquevalue;

    @Basic
    private Integer readonly;

    @Column(nullable = true)
    private String conversionPattern;

    @Column(nullable = true)
    private String validatorClass;

    @Transient
    private AbstractAttributeValidator validator;

    public AbstractSchema() {
        super();

        type = SchemaValueType.String;
        virtual = getBooleanAsInteger(false);
        mandatoryCondition = Boolean.FALSE.toString();
        multivalue = getBooleanAsInteger(false);
        uniquevalue = getBooleanAsInteger(false);
        readonly = getBooleanAsInteger(false);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SchemaValueType getType() {
        return type;
    }

    public void setType(SchemaValueType type) {
        this.type = type;
    }

    public boolean isVirtual() {
        return isBooleanAsInteger(virtual);
    }

    public void setVirtual(boolean virtual) {
        this.virtual = getBooleanAsInteger(virtual);
    }

    public String getMandatoryCondition() {
        return mandatoryCondition;
    }

    public void setMandatoryCondition(String mandatoryCondition) {
        this.mandatoryCondition = mandatoryCondition;
    }

    public boolean isMultivalue() {
        return isBooleanAsInteger(multivalue);
    }

    public void setMultivalue(boolean multivalue) {
        this.multivalue = getBooleanAsInteger(multivalue);
    }

    public boolean isUniquevalue() {
        return isBooleanAsInteger(uniquevalue);
    }

    public void setUniquevalue(boolean uniquevalue) {
        this.uniquevalue = getBooleanAsInteger(uniquevalue);
    }

    public boolean isReadonly() {
        return isBooleanAsInteger(readonly);
    }

    public void setReadonly(boolean readonly) {
        this.readonly = getBooleanAsInteger(readonly);
    }

    public AbstractAttributeValidator getValidator() {
        if (validator != null) {
            return validator;
        }

        if (getValidatorClass() != null && getValidatorClass().length() > 0) {
            try {
                Constructor validatorConstructor =
                        Class.forName(getValidatorClass()).getConstructor(
                        new Class[]{getClass().getSuperclass()});
                validator =
                        (AbstractAttributeValidator) validatorConstructor.
                        newInstance(
                        this);
            } catch (Exception e) {
                LOG.error("Could not instantiate validator of type "
                        + getValidatorClass()
                        + ", reverting to AttributeBasicValidator", e);
            }
        }

        if (validator == null) {
            validator = new BasicAttributeValidator(this);
        }

        return validator;
    }

    public String getValidatorClass() {
        return validatorClass;
    }

    public void setValidatorClass(String validatorClass) {
        this.validatorClass = validatorClass;
    }

    public String getConversionPattern() {
        if (!getType().isConversionPatternNeeded()) {
            LOG.debug("Conversion pattern is not needed: {}'s type is {}",
                    this, getType());
        }

        return conversionPattern;
    }

    public void setConversionPattern(String conversionPattern) {
        if (!getType().isConversionPatternNeeded()) {
            LOG.warn("Conversion pattern will be ignored: "
                    + "this attribute type is " + getType());
        }

        this.conversionPattern = conversionPattern;
    }

    public <T extends Format> T getFormatter() {
        T result = null;

        switch (getType()) {
            case Long:
                DecimalFormat longFormatter = DECIMAL_FORMAT.get();
                longFormatter.applyPattern(getConversionPattern());

                result = (T) longFormatter;
                break;

            case Double:
                DecimalFormat doubleFormatter = DECIMAL_FORMAT.get();
                doubleFormatter.applyPattern(getConversionPattern());

                result = (T) doubleFormatter;
                break;

            case Date:
                SimpleDateFormat dateFormatter = DATE_FORMAT.get();
                dateFormatter.applyPattern(getConversionPattern());
                dateFormatter.setLenient(false);

                result = (T) dateFormatter;
                break;

            default:
        }

        return result;
    }

    public abstract <T extends AbstractAttribute> boolean addAttribute(
            T attribute);

    public abstract <T extends AbstractAttribute> boolean removeAttribute(
            T attribute);

    public abstract List<? extends AbstractAttribute> getAttributes();

    public abstract void setAttributes(
            List<? extends AbstractAttribute> attributes);

    public abstract <T extends AbstractDerivedSchema> boolean addDerivedSchema(
            T derivedSchema);

    public abstract <T extends AbstractDerivedSchema> boolean removeDerivedSchema(
            T derivedSchema);

    public abstract List<? extends AbstractDerivedSchema> getDerivedSchemas();

    public abstract void setDerivedSchemas(
            List<? extends AbstractDerivedSchema> derivedSchemas);
}
