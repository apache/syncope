/*
 *  Copyright 2010 ilgrosso.
 * 
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
import javax.persistence.Embeddable;

@Embeddable
public class SyncopeRolePK implements Serializable {

    final public static String ROOT_ROLE = "/";
    private String name;
    private String parent;

    public SyncopeRolePK() {
    }

    public SyncopeRolePK(String name, String parent)
            throws IllegalArgumentException {

        setName(name);
        setParent(parent);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) throws IllegalArgumentException {
        if (ROOT_ROLE.equals(name)) {
            throw new IllegalArgumentException(
                    ROOT_ROLE + " is a reserved role name");
        }

        this.name = name;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        if (parent == null || parent.length() == 0) {
            this.parent = ROOT_ROLE;
        } else {
            this.parent = parent;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SyncopeRolePK other = (SyncopeRolePK) obj;
        if (this.parent != other.parent
                && (this.parent == null || !this.parent.equals(other.parent))) {

            return false;
        }
        if ((this.name == null)
                ? (other.name != null) : !this.name.equals(other.name)) {

            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this.parent != null ? this.parent.hashCode() : 0);
        hash = 89 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "("
                + "name=" + name + ","
                + "parent=" + parent
                + ")";
    }
}
