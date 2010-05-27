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

import java.io.Serializable;
import javax.persistence.Entity;

@Entity
public class UserAttributeValueAsDouble
        extends UserAttributeValue implements Serializable {

    Double actualValue;

    public UserAttributeValueAsDouble() {
    }

    public UserAttributeValueAsDouble(Double actualValue) {
        super();
        this.actualValue = actualValue;
    }

    public Double getActualValue() {
        return actualValue;
    }

    public void setActualValue(Double actualValue) {
        this.actualValue = actualValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }

        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UserAttributeValueAsDouble other =
                (UserAttributeValueAsDouble) obj;
        if (this.actualValue != other.actualValue
                && (this.actualValue == null
                || !this.actualValue.equals(other.actualValue))) {

            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.actualValue != null
                ? this.actualValue.hashCode() : 0);
        return super.hashCode() + hash;
    }

    @Override
    public String toString() {
        return "("
                + "id=" + getId() + ","
                + "actualValue=" + actualValue
                + ")";
    }
}
