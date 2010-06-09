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

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class UserDerivedAttributeSchema implements Serializable {

    @Id
    private String name;
    private String expression;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UserDerivedAttributeSchema other = 
                (UserDerivedAttributeSchema) obj;
        
        if ((this.name == null)
                ? (other.name != null) : !this.name.equals(other.name)) {

            return false;
        }
        if ((this.expression == null)
                ? (other.expression != null)
                : !this.expression.equals(other.expression)) {

            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 79 * hash + (this.expression != null
                ? this.expression.hashCode() : 0);

        return hash;
    }

    @Override
    public String toString() {
        return "("
                + "name=" + getName() + ","
                + "expression=" + getExpression()
                + ")";
    }
}
