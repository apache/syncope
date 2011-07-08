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
package org.syncope.core.persistence.beans.user;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractVirAttr;
import org.syncope.core.persistence.beans.AbstractVirSchema;
import org.syncope.types.SourceMappingType;

@Entity
public class UVirAttr extends AbstractVirAttr {

    private static final long serialVersionUID = 2943450934283989741L;

    @ManyToOne
    private SyncopeUser owner;

    @ManyToOne(fetch = FetchType.EAGER)
    private UVirSchema virtualSchema;

    @Override
    public <T extends AbstractAttributable> T getOwner() {
        return (T) owner;
    }

    @Override
    public <T extends AbstractAttributable> void setOwner(T owner) {
        this.owner = (SyncopeUser) owner;
    }

    @Override
    public <T extends AbstractVirSchema> T getVirtualSchema() {
        return (T) virtualSchema;
    }

    @Override
    public <T extends AbstractVirSchema> void setVirtualSchema(
            T virtualSchema) {

        this.virtualSchema = (UVirSchema) virtualSchema;
    }

    @Override
    public List<String> getValues() {
        LOG.debug("{}: retrieve value for attribute {}",
                new Object[]{getOwner(), getVirtualSchema().getName()});

        if (values != null) {
            return values;
        }

        final List<Object> retrievedValues =
                retrieveValues(getOwner(),
                getVirtualSchema().getName(),
                SourceMappingType.UserVirtualSchema);

        LOG.debug("Retrieved external values {}", retrievedValues);

        List<String> stringValues = new ArrayList<String>();
        for (Object value : retrievedValues) {
            stringValues.add(value.toString());
        }

        return stringValues;
    }
}
