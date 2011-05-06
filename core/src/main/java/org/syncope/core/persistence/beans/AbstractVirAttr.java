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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import org.springframework.context.ConfigurableApplicationContext;
import org.syncope.core.persistence.propagation.PropagationManager;
import org.syncope.core.util.ApplicationContextManager;
import org.syncope.types.SourceMappingType;

@MappedSuperclass
public abstract class AbstractVirAttr extends AbstractBaseBean {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    @Transient
    protected List<String> values;

    public Long getId() {
        return id;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    /**
     * @see http://commons.apache.org/jexl/reference/index.html
     * @return the value of this virtual attribute
     */
    public List<String> getValues() {

        LOG.debug("{}: retrieve value for attribute {}",
                new Object[]{getOwner(), getVirtualSchema().getName()});

        if (values != null) {
            return values;
        }

        ConfigurableApplicationContext context =
                ApplicationContextManager.getApplicationContext();

        PropagationManager propagationManager =
                (PropagationManager) context.getBean("propagationManager");

        final Set<String> retrievedValues = propagationManager.getObjectAttributeValue(
                getOwner(),
                getVirtualSchema().getName(),
                SourceMappingType.UserVirtualSchema);

        LOG.debug("Retrieved external values {}", retrievedValues);

        try {

            return new ArrayList<String>(retrievedValues);

        } catch (Throwable t) {
            // NullPointerException and ArrayIndexOutOfBoundsException
            return null;
        }
    }

    public abstract <T extends AbstractAttributable> T getOwner();

    public abstract <T extends AbstractAttributable> void setOwner(T owner);

    public abstract <T extends AbstractVirSchema> T getVirtualSchema();

    public abstract <T extends AbstractVirSchema> void setVirtualSchema(
            T derivedSchema);
}
