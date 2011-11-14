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

import java.util.ArrayList;
import java.util.List;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.syncope.client.AbstractBaseBean;

public class AttributeMod extends AbstractBaseBean {

    private static final long serialVersionUID = -913573979137431406L;

    private String schema;

    private List<String> valuesToBeAdded;

    private List<String> valuesToBeRemoved;

    public AttributeMod() {
        super();

        valuesToBeAdded = new ArrayList<String>();
        valuesToBeRemoved = new ArrayList<String>();
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

    public List<String> getValuesToBeAdded() {
        return valuesToBeAdded;
    }

    public void setValuesToBeAdded(List<String> valuesToBeAdded) {
        this.valuesToBeAdded = valuesToBeAdded;
    }

    public boolean addValueToBeRemoved(String value) {
        return valuesToBeRemoved.add(value);
    }

    public boolean removeValueToBeRemoved(String value) {
        return valuesToBeRemoved.remove(value);
    }

    public List<String> getValuesToBeRemoved() {
        return valuesToBeRemoved;
    }

    public void setValuesToBeRemoved(List<String> valuesToBeRemoved) {
        this.valuesToBeRemoved = valuesToBeRemoved;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return valuesToBeAdded.isEmpty()
                && valuesToBeRemoved.isEmpty();
    }
}
