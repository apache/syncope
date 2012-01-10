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

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;

import javax.validation.constraints.NotNull;
import org.syncope.core.persistence.validation.entity.PolicyCheck;
import org.syncope.core.util.XMLSerializer;
import org.syncope.types.AbstractPolicySpec;
import org.syncope.types.PolicyType;

@Entity
@PolicyCheck
public abstract class Policy extends AbstractBaseBean {

    private static final long serialVersionUID = -5844833125843247458L;

    @Id
    private Long id;

    @NotNull
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    protected PolicyType type;

    @Lob
    //@Type(type = "org.hibernate.type.StringClobType")
    private String specification;

    public Long getId() {
        return id;
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

    public <T extends AbstractPolicySpec> T getSpecification() {
        return XMLSerializer.<T>deserialize(specification);
    }

    public <T extends AbstractPolicySpec> void setSpecification(
            final T policy) {

        specification = XMLSerializer.serialize(policy);
    }
}
