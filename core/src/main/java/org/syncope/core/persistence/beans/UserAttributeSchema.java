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
import static javax.persistence.EnumType.STRING;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.core.persistence.AttributeType;
import org.syncope.core.persistence.validation.UserAttributeBasicValidator;
import org.syncope.core.persistence.validation.UserAttributeValidator;
import org.syncope.core.persistence.validation.ValidatorInstantiationException;

@Entity
public class UserAttributeSchema implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(
            UserAttributeSchema.class);
    @Id
    private String name;
    @Column(nullable = false)
    @Enumerated(STRING)
    private AttributeType type;
    private Boolean mandatory;
    private Boolean multivalue;
    private String conversionPattern;
    private String validatorClass;
    @Transient
    private UserAttributeValidator validator;

    public UserAttributeSchema() {
        type = AttributeType.String;
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

    public UserAttributeValidator getValidator()
            throws ValidatorInstantiationException {

        if (validator != null) {
            return validator;
        }

        if (getValidatorClass() != null && getValidatorClass().length() > 0) {
            try {
                Constructor validatorConstructor =
                        Class.forName(getValidatorClass()).getConstructor(
                        new Class[]{getClass()});
                validator = (UserAttributeValidator) validatorConstructor.newInstance(this);
            } catch (Exception e) {
                throw new ValidatorInstantiationException(
                        "Could not instantiate validator of type "
                        + getValidatorClass(), e);
            }
        } else {
            try {
                validator = new UserAttributeBasicValidator(this);
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
        final UserAttributeSchema other = (UserAttributeSchema) obj;
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

    @Override
    public String toString() {
        return "("
                + "name=" + name + ","
                + "type=" + type + ","
                + "mandatory=" + mandatory + ","
                + "multivalue=" + multivalue + ","
                + "conversionPattern=" + conversionPattern + ","
                + "validatorClass=" + validatorClass
                + ")";
    }
}
