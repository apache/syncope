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
package org.syncope.core.beans;

import static javax.persistence.EnumType.STRING;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.core.enums.AttributeType;

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

    private boolean isConversionPatternNeeded() {
        return type == AttributeType.Date
                || type == AttributeType.Double
                || type == AttributeType.Long;
    }

    public String getConversionPattern() {
        if (!isConversionPatternNeeded()) {
            log.warn("Conversion pattern is not needed: "
                    + "this attribute type is "
                    + getType());
        }

        return conversionPattern;
    }

    public void setConversionPattern(String conversionPattern) {
        if (!isConversionPatternNeeded()) {
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
                        ((DecimalFormat) getType().getFormatter());
                longFormatter.applyPattern(getConversionPattern());

                result = (T) longFormatter;
                break;

            case Double:
                DecimalFormat doubleFormatter =
                        ((DecimalFormat) getType().getFormatter());
                doubleFormatter.applyPattern(getConversionPattern());

                result = (T) doubleFormatter;
                break;

            case Date:
                SimpleDateFormat dateFormatter =
                        (SimpleDateFormat) getType().getFormatter();
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

        return hash;
    }

    @Override
    public String toString() {
        return "("
                + "name=" + name + ","
                + "type=" + type + ","
                + "mandatory=" + mandatory + ","
                + "multivalue=" + multivalue + ","
                + "conversionPattern=" + conversionPattern
                + ")";
    }
}
