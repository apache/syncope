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
package org.syncope.core.persistence.beans.membership;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.TableGenerator;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractAttributeValue;

@Entity
public class MembershipAttributeValue extends AbstractAttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE,
    generator = "SEQ_MembershipAttributeValue")
    @TableGenerator(name = "SEQ_MembershipAttributeValue", allocationSize = 200)
    private Long id;

    @ManyToOne
    private MembershipAttribute attribute;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public <T extends AbstractAttribute> T getAttribute() {
        return (T) attribute;
    }

    @Override
    public <T extends AbstractAttribute> void setAttribute(T attribute) {
        this.attribute = (MembershipAttribute) attribute;
    }
}
