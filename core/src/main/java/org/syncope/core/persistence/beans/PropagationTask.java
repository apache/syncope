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

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import org.hibernate.annotations.Type;
import org.identityconnectors.framework.common.objects.Attribute;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.validation.entity.PropagationTaskCheck;
import org.syncope.core.util.XMLSerializer;
import org.syncope.types.PropagationMode;
import org.syncope.types.PropagationOperation;

/**
 * Encapsulate all information about a propagation task.
 */
@Entity
@PropagationTaskCheck
public class PropagationTask extends Task {

    private static final long serialVersionUID = 7086054884614511210L;

    /**
     * @see PropagationMode
     */
    @Enumerated(EnumType.STRING)
    private PropagationMode propagationMode;

    /**
     * @see PropagationOperation
     */
    @Enumerated(EnumType.STRING)
    private PropagationOperation propagationOperation;

    /**
     * The accountId on the external resource.
     */
    private String accountId;

    /**
     * The (optional) former accountId on the external resource.
     */
    private String oldAccountId;

    /**
     * Attributes to be propagated.
     */
    @Lob
    @Type(type = "org.hibernate.type.StringClobType")
    private String xmlAttributes;

    /**
     * User whose data are propagated.
     */
    @ManyToOne
    private SyncopeUser syncopeUser;

    /**
     * ExternalResource to which the propagation happens.
     */
    @ManyToOne
    private ExternalResource resource;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getOldAccountId() {
        return oldAccountId;
    }

    public void setOldAccountId(String oldAccountId) {
        this.oldAccountId = oldAccountId;
    }

    public Set<Attribute> getAttributes() {
        return XMLSerializer.<Set<Attribute>>deserialize(xmlAttributes);
    }

    public void setAttributes(final Set<Attribute> attributes) {
        xmlAttributes = XMLSerializer.serialize(attributes);
    }

    public PropagationMode getPropagationMode() {
        return propagationMode;
    }

    public void setPropagationMode(PropagationMode propagationMode) {
        this.propagationMode = propagationMode;
    }

    public PropagationOperation getPropagationOperation() {
        return propagationOperation;
    }

    public void setPropagationOperation(
            PropagationOperation propagationOperation) {

        this.propagationOperation = propagationOperation;
    }

    public ExternalResource getResource() {
        return resource;
    }

    public void setResource(ExternalResource resource) {
        this.resource = resource;
    }

    public SyncopeUser getSyncopeUser() {
        return syncopeUser;
    }

    public void setSyncopeUser(SyncopeUser syncopeUser) {
        this.syncopeUser = syncopeUser;
    }
}
