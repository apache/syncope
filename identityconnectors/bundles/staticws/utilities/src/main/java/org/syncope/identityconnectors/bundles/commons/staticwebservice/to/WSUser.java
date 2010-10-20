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
package org.syncope.identityconnectors.bundles.commons.staticwebservice.to;

import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class WSUser extends AbstractData {

    private String accountid;

    private Set<WSAttributeValue> attributes;

    public String getAccountid() {
        return accountid;
    }

    public void setAccountid(String accountid) {
        this.accountid = accountid;
    }

    public Set<WSAttributeValue> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<WSAttributeValue> attributes) {
        this.attributes = attributes;
    }

    public void addAttribute(WSAttributeValue attribute) {
        if (attributes == null)
            attributes = new HashSet<WSAttributeValue>();

        this.attributes.add(attribute);
    }

    public WSUser() {
    }

    public WSUser(String accountid, Set<WSAttributeValue> attributes) {
        this.accountid = accountid;
        this.attributes = attributes;
    }
}
