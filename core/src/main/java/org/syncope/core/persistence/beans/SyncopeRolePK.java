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

import javax.persistence.Embeddable;

@Embeddable
public class SyncopeRolePK extends AbstractBaseBean {

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

    public final void setName(String name) throws IllegalArgumentException {
        if (ROOT_ROLE.equals(name)) {
            throw new IllegalArgumentException(
                    ROOT_ROLE + " is a reserved role name");
        }

        this.name = name;
    }

    public String getParent() {
        return parent;
    }

    public final void setParent(String parent) {
        if (parent == null || parent.length() == 0) {
            this.parent = ROOT_ROLE;
        } else {
            this.parent = parent;
        }
    }
}
