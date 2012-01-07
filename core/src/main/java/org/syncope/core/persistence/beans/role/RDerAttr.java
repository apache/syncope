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
package org.syncope.core.persistence.beans.role;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractDerAttr;
import org.syncope.core.persistence.beans.AbstractDerSchema;

@Entity
public class RDerAttr extends AbstractDerAttr {

    private static final long serialVersionUID = 8007080005675899946L;

    @ManyToOne
    private SyncopeRole owner;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private RDerSchema derivedSchema;

    @Override
    public <T extends AbstractAttributable> T getOwner() {
        return (T) owner;
    }

    @Override
    public <T extends AbstractAttributable> void setOwner(T owner) {
        this.owner = (SyncopeRole) owner;
    }

    @Override
    public <T extends AbstractDerSchema> T getDerivedSchema() {
        return (T) derivedSchema;
    }

    @Override
    public <T extends AbstractDerSchema> void setDerivedSchema(
            T derivedSchema) {

        this.derivedSchema = (RDerSchema) derivedSchema;
    }
}
