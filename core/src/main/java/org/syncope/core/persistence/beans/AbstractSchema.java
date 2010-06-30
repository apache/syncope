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

import static javax.persistence.EnumType.STRING;

import java.lang.reflect.Constructor;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import org.syncope.core.persistence.validation.AttributeBasicValidator;
import org.syncope.core.persistence.validation.AttributeValidator;
import org.syncope.core.persistence.validation.ValidatorInstantiationException;
import org.syncope.types.AttributeType;

@MappedSuperclass
public abstract class AbstractSchema extends AbstractBaseBean {

    @Id
    private String name;

    @Column(nullable = false)
    @Enumerated(STRING)
    private AttributeType type;

    /**
     * Specify if the attribute should be stored on the local repository.
     */
    private boolean virtual;

    private boolean mandatory;

    private boolean multivalue;

    @Column(nullable = true)
    private String conversionPattern;

    @Column(nullable = true)
    private String validatorClass;

    @Transient
    private AttributeValidator validator;

    public AbstractSchema() {
        type = AttributeType.String;
        virtual = false;
        mandatory = false;
        multivalue = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AttributeType getType() {
        return type;
    }

    public void setType(AttributeType type) {
        this.type = type;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isMultivalue() {
        return multivalue;
    }

    public void setMultivalue(boolean multivalue) {
        this.multivalue = multivalue;
    }

    public AttributeValidator getValidator()
            throws ValidatorInstantiationException {

        if (validator != null) {
            return validator;
        }

        if (getValidatorClass() != null && getValidatorClass().length() > 0) {
            try {
                Constructor validatorConstructor =
                        Class.forName(getValidatorClass()).getConstructor(
                        new Class[]{getClass().getSuperclass()});
                validator = (AttributeValidator) validatorConstructor.newInstance(this);
            } catch (Exception e) {
                throw new ValidatorInstantiationException(
                        "Could not instantiate validator of type " + getValidatorClass(), e);
            }
        } else {
            try {
                validator = new AttributeBasicValidator(this);
            } catch (ClassNotFoundException cnfe) {
                throw new ValidatorInstantiationException(
                        "Could not instantiate basic validator", cnfe);
            }
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
            log.warn("Conversion pattern is not needed: " + "this attribute type is " + getType());
        }

        return conversionPattern;
    }

    public void setConversionPattern(String conversionPattern) {
        if (!getType().isConversionPatternNeeded()) {
            log.warn("Conversion pattern will be ignored: " + "this attribute type is " + getType());
        }

        this.conversionPattern = conversionPattern;
    }

    public <T extends Format> T getFormatter(Class<T> reference) {
        T result = null;

        switch (getType()) {
            case Long:
                DecimalFormat longFormatter =
                        ((DecimalFormat) getType().getBasicFormatter());
                longFormatter.applyPattern(getConversionPattern());

                result = (T) longFormatter;
                break;

            case Double:
                DecimalFormat doubleFormatter =
                        ((DecimalFormat) getType().getBasicFormatter());
                doubleFormatter.applyPattern(getConversionPattern());

                result = (T) doubleFormatter;
                break;

            case Date:
                SimpleDateFormat dateFormatter =
                        (SimpleDateFormat) getType().getBasicFormatter();
                dateFormatter.applyPattern(getConversionPattern());

                result = (T) dateFormatter;
                break;
        }

        return result;
    }

    public abstract <T extends AbstractAttribute> boolean addAttribute(T attribute);

    public abstract <T extends AbstractAttribute> boolean removeAttribute(T attribute);

    public abstract Set<? extends AbstractAttribute> getAttributes();

    public abstract void setAttributes(
            Set<? extends AbstractAttribute> attributes);

    public abstract <T extends AbstractDerivedSchema> boolean addDerivedSchema(T derivedSchema);

    public abstract <T extends AbstractDerivedSchema> boolean removeDerivedSchema(T derivedSchema);

    public abstract Set<? extends AbstractDerivedSchema> getDerivedSchemas();

    public abstract void setDerivedSchemas(Set<? extends AbstractDerivedSchema> derivedSchemas);

    public abstract Set<SchemaMapping> getMappings();

    public abstract void setMappings(Set<SchemaMapping> mappings);

    public abstract boolean addMapping(SchemaMapping mapping);

    public abstract boolean removeMapping(SchemaMapping mapping);
}
