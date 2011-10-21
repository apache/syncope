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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.MappedSuperclass;
import org.syncope.core.persistence.validation.entity.SyncopeUserCheck;

@MappedSuperclass
public abstract class AbstractAttributable extends AbstractBaseBean {

    private static final long serialVersionUID = -4801685541488201119L;

    /**
     * Provisioning target resources.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    protected Set<ExternalResource> externalResources;

    public <T extends AbstractAttr> T getAttribute(final String schemaName) {
        T result = null;
        T attribute;
        for (Iterator<? extends AbstractAttr> itor =
                getAttributes().iterator();
                result == null && itor.hasNext();) {

            attribute = (T) itor.next();
            if (attribute.getSchema() != null
                    && schemaName.equals(attribute.getSchema().getName())) {

                result = attribute;
            }
        }

        return result;
    }

    public <T extends AbstractDerAttr> T getDerivedAttribute(
            final String derivedSchemaName) {

        T result = null;
        T derivedAttribute;
        for (Iterator<? extends AbstractDerAttr> itor =
                getDerivedAttributes().iterator();
                result == null && itor.hasNext();) {

            derivedAttribute = (T) itor.next();
            if (derivedAttribute.getDerivedSchema() != null
                    && derivedSchemaName.equals(
                    derivedAttribute.getDerivedSchema().getName())) {

                result = derivedAttribute;
            }
        }

        return result;
    }

    public <T extends AbstractVirAttr> T getVirtualAttribute(
            final String virtualSchemaName) {

        T result = null;
        T virtualAttribute;
        for (Iterator<? extends AbstractVirAttr> itor =
                getVirtualAttributes().iterator();
                result == null && itor.hasNext();) {

            virtualAttribute = (T) itor.next();
            if (virtualAttribute.getVirtualSchema() != null
                    && virtualSchemaName.equals(
                    virtualAttribute.getVirtualSchema().getName())) {

                result = virtualAttribute;
            }
        }

        return result;
    }

    public boolean addExternalResource(
            final ExternalResource externalResource) {

        if (externalResources == null) {
            externalResources = new HashSet<ExternalResource>();
        }
        return externalResources.add(externalResource);
    }

    public boolean removeExternalResource(
            final ExternalResource externalResource) {

        return externalResources == null
                ? true
                : externalResources.remove(externalResource);
    }

    public Set<ExternalResource> getExternalResources() {
        return externalResources == null
                ? Collections.EMPTY_SET
                : externalResources;
    }

    public Set<String> getExternalResourceNames() {
        Set<String> resourceNames;

        if (externalResources == null) {
            resourceNames = Collections.EMPTY_SET;
        } else {
            resourceNames = new HashSet<String>(externalResources.size());
            for (ExternalResource resource : externalResources) {
                resourceNames.add(resource.getName());
            }
        }

        return resourceNames;
    }

    public void setExternalResources(
            final Set<ExternalResource> externalResources) {

        this.externalResources = externalResources;
    }

    protected Map<AbstractSchema, AbstractAttr> getAttributesMap() {
        final Map<AbstractSchema, AbstractAttr> map =
                new HashMap<AbstractSchema, AbstractAttr>();

        for (AbstractAttr attr : getAttributes()) {
            map.put((AbstractSchema) attr.getSchema(), attr);
        }

        return map;
    }

    protected Map<AbstractDerSchema, AbstractDerAttr> getDerivedAttributesMap() {
        final Map<AbstractDerSchema, AbstractDerAttr> map =
                new HashMap<AbstractDerSchema, AbstractDerAttr>();

        for (AbstractDerAttr attr : getDerivedAttributes()) {
            map.put((AbstractDerSchema) attr.getDerivedSchema(), attr);
        }

        return map;
    }

    protected Map<AbstractVirSchema, AbstractVirAttr> getVirtualAttributesMap() {
        final Map<AbstractVirSchema, AbstractVirAttr> map =
                new HashMap<AbstractVirSchema, AbstractVirAttr>();

        for (AbstractVirAttr attr : getVirtualAttributes()) {
            map.put((AbstractVirSchema) attr.getVirtualSchema(), attr);
        }

        return map;
    }

    public abstract Long getId();

    public abstract <T extends AbstractAttr> boolean addAttribute(
            T attribute);

    public abstract <T extends AbstractAttr> boolean removeAttribute(
            T attribute);

    public abstract List<? extends AbstractAttr> getAttributes();

    public abstract void setAttributes(
            List<? extends AbstractAttr> attributes);

    public abstract <T extends AbstractDerAttr> boolean addDerivedAttribute(
            T derivedAttribute);

    public abstract <T extends AbstractDerAttr> boolean removeDerivedAttribute(
            T derivedAttribute);

    public abstract List<? extends AbstractDerAttr> getDerivedAttributes();

    public abstract void setDerivedAttributes(
            List<? extends AbstractDerAttr> derivedAttributes);

    public abstract <T extends AbstractVirAttr> boolean addVirtualAttribute(
            T virtualAttributes);

    public abstract <T extends AbstractVirAttr> boolean removeVirtualAttribute(
            T virtualAttribute);

    public abstract List<? extends AbstractVirAttr> getVirtualAttributes();

    public abstract void setVirtualAttributes(
            List<? extends AbstractVirAttr> virtualAttributes);
}
