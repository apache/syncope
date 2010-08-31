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
package org.syncope.client.to;

public class RoleTO extends AbstractAttributableTO {

    private String name;
    private long parent;
    private boolean inheritAttributes;
    private boolean inheritDerivedAttributes;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public long getParent() {
        return parent;
    }

    public void setParent(final long parent) {
        this.parent = parent;
    }

    public boolean isInheritAttributes() {
        return inheritAttributes;
    }

    public void setInheritAttributes(final boolean inheritAttributes) {
        this.inheritAttributes = inheritAttributes;
    }

    public boolean isInheritDerivedAttributes() {
        return inheritDerivedAttributes;
    }

    public void setInheritDerivedAttributes(
            final boolean inheritDerivedAttributes) {
        
        this.inheritDerivedAttributes = inheritDerivedAttributes;
    }
}
