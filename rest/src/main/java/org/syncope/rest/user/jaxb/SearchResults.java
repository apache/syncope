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
package org.syncope.rest.user.jaxb;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * TODO: implement
 */
@XmlRootElement
public class SearchResults implements Iterable<String> {

    @XmlElement(name = "result")
    private Set<String> elements;

    public SearchResults() {
        elements = new HashSet<String>();
    }

    public void addResult(String result) {
        elements.add(result);
    }

    @Override
    public Iterator<String> iterator() {
        return elements.iterator();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SearchResults)) {
            return false;
        }

        SearchResults other = (SearchResults) obj;
        return elements.equals(other.elements);
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }
}
