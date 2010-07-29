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
package org.syncope.client.mod;

import java.util.HashSet;
import java.util.Set;
import org.syncope.client.AbstractBaseBean;

public class AttributeMod extends AbstractBaseBean {

    private String schema;
    private Set<String> valuesToBeAdded;
    private Set<String> valuesToBeRemoved;

    public AttributeMod() {
        valuesToBeAdded = new HashSet<String>();
        valuesToBeRemoved = new HashSet<String>();
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public boolean addValueToBeAdded(String value) {
        return valuesToBeAdded.add(value);
    }

    public boolean removeValueToBeAdded(String value) {
        return valuesToBeAdded.remove(value);
    }

    public Set<String> getValuesToBeAdded() {
        return valuesToBeAdded;
    }

    public void setValuesToBeAdded(Set<String> valuesToBeAdded) {
        this.valuesToBeAdded = valuesToBeAdded;
    }

    public boolean addValueToBeRemoved(String value) {
        return valuesToBeRemoved.add(value);
    }

    public boolean removeValueToBeRemoved(String value) {
        return valuesToBeRemoved.remove(value);
    }

    public Set<String> getValuesToBeRemoved() {
        return valuesToBeRemoved;
    }

    public void setValuesToBeRemoved(Set<String> valuesToBeRemoved) {
        this.valuesToBeRemoved = valuesToBeRemoved;
    }
}
