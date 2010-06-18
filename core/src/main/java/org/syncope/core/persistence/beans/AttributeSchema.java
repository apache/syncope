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
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import org.syncope.core.persistence.validation.AttributeBasicValidator;
import org.syncope.core.persistence.validation.AttributeValidator;
import org.syncope.core.persistence.validation.ValidatorInstantiationException;
import org.syncope.types.AttributeType;

@Entity
public class AttributeSchema extends AbstractBaseBean {

    @Id
    private String name;
    @Column(nullable = false)
    @Enumerated(STRING)
    private AttributeType type;
    private Boolean mandatory;
    private Boolean multivalue;
    @Column(nullable = true)
    private String conversionPattern;
    @Column(nullable = true)
    private String validatorClass;
    @Transient
    private AttributeValidator validator;
    @OneToMany(mappedBy = "schema")
    private Set<Attribute> attributes;
    @ManyToMany(mappedBy = "attributeSchemas")
    private Set<DerivedAttributeSchema> derivedAttributeSchemas;

    public AttributeSchema() {
        type = AttributeType.String;
        mandatory = false;
        multivalue = false;

        attributes = new HashSet<Attribute>();
        derivedAttributeSchemas = new HashSet<DerivedAttributeSchema>();
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

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    public Boolean isMultivalue() {
        return multivalue;
    }

    public void setMultivalue(Boolean multivalue) {
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
                        new Class[]{getClass()});
                validator = (AttributeValidator) validatorConstructor.newInstance(this);
            } catch (Exception e) {
                throw new ValidatorInstantiationException(
                        "Could not instantiate validator of type "
                        + getValidatorClass(), e);
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
            log.warn("Conversion pattern is not needed: "
                    + "this attribute type is "
                    + getType());
        }

        return conversionPattern;
    }

    public void setConversionPattern(String conversionPattern) {
        if (!getType().isConversionPatternNeeded()) {
            log.warn("Conversion pattern will be ignored: "
                    + "this attribute type is "
                    + getType());
        }

        this.conversionPattern = conversionPattern;
    }

    public Set<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<Attribute> attributes) {
        this.attributes = attributes;
    }

    public boolean addDerivedAttributeSchema(
            DerivedAttributeSchema derivedAttributeSchema) {

        return derivedAttributeSchemas.add(derivedAttributeSchema);
    }

    public boolean removeDerivedAttributeSchema(
            DerivedAttributeSchema derivedAttributeSchema) {

        return derivedAttributeSchemas.remove(derivedAttributeSchema);
    }

    public Set<DerivedAttributeSchema> getDerivedAttributeSchemas() {
        return derivedAttributeSchemas;
    }

    public void setDerivedAttributeSchemas(
            Set<DerivedAttributeSchema> derivedAttributeSchemas) {

        this.derivedAttributeSchemas = derivedAttributeSchemas;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AttributeSchema other = (AttributeSchema) obj;
        if ((this.name == null) ? (other.name != null)
                : !this.name.equals(other.name)) {

            return false;
        }
        if (this.type != other.type
                && (this.type == null || !this.type.equals(other.type))) {

            return false;
        }
        if (this.mandatory != other.mandatory
                && (this.mandatory == null
                || !this.mandatory.equals(other.mandatory))) {

            return false;
        }
        if (this.multivalue != other.multivalue
                && (this.multivalue == null
                || !this.multivalue.equals(other.multivalue))) {

            return false;
        }
        if ((this.conversionPattern == null)
                ? (other.conversionPattern != null)
                : !this.conversionPattern.equals(other.conversionPattern)) {

            return false;
        }
        if ((this.validatorClass == null)
                ? (other.validatorClass != null)
                : !this.validatorClass.equals(other.validatorClass)) {

            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 67 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 67 * hash + (this.mandatory != null
                ? this.mandatory.hashCode() : 0);
        hash = 67 * hash + (this.multivalue != null
                ? this.multivalue.hashCode() : 0);
        hash = 67 * hash + (this.conversionPattern != null
                ? this.conversionPattern.hashCode() : 0);
        hash = 67 * hash + (this.validatorClass != null
                ? this.validatorClass.hashCode() : 0);

        return hash;
    }
}
