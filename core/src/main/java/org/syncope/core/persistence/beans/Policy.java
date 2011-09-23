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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.annotations.Type;
import org.syncope.core.persistence.util.XmlConfiguration;
import org.syncope.core.persistence.validation.entity.PolicyCheck;
import org.syncope.types.AbstractPolicy;
import org.syncope.types.PolicyType;

@Entity
@PolicyCheck
public class Policy extends AbstractBaseBean {

    private static final long serialVersionUID = -5844833125843247458L;

    @Id
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PolicyType type;

    @Lob
    @Type(type = "org.hibernate.type.StringClobType")
    private String specification;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PolicyType getType() {
        return type;
    }

    public void setType(PolicyType type) {
        this.type = type;
    }

    public <T extends AbstractPolicy> T getSpecification() {
        T result = XmlConfiguration.<T>deserialize(specification);
        return result;
    }

    public <T extends AbstractPolicy> void setSpecification(final T policy) {
        specification = XmlConfiguration.serialize(policy);
    }
}
