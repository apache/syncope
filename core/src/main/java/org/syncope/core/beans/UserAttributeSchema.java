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
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import org.syncope.core.enums.AttributeType;

@Entity
public class UserAttributeSchema implements Serializable {

    @Id
    private String name;
    @Enumerated(STRING)
    private AttributeType type;
    private String conversionPattern;
    private String conversionClass;

    public String getConversionClass() {
        return conversionClass;
    }

    public void setConversionClass(String conversionClass) {
        this.conversionClass = conversionClass;
    }

    public String getConversionPattern() {
        return conversionPattern;
    }

    public void setConversionPattern(String conversionPattern) {
        this.conversionPattern = conversionPattern;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        final UserAttributeSchema other = (UserAttributeSchema) obj;
        if ((this.name == null)
                ? (other.name != null) : !this.name.equals(other.name)) {

            return false;
        }
        if (this.type != other.type
                && (this.type == null || !this.type.equals(other.type))) {

            return false;
        }
        if ((this.conversionPattern == null)
                ? (other.conversionPattern != null)
                : !this.conversionPattern.equals(other.conversionPattern)) {

            return false;
        }
        if ((this.conversionClass == null)
                ? (other.conversionClass != null)
                : !this.conversionClass.equals(other.conversionClass)) {

            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;

        hash = 19 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 19 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 19 * hash + (this.conversionPattern != null
                ? this.conversionPattern.hashCode() : 0);
        hash = 19 * hash + (this.conversionClass != null
                ? this.conversionClass.hashCode() : 0);

        return hash;
    }

    @Override
    public String toString() {
        return "("
                + "name=" + name + ","
                + "type=" + type + ","
                + "conversionPattern=" + conversionPattern + ","
                + "conversionClass=" + conversionClass + ","
                + ")";
    }
}
