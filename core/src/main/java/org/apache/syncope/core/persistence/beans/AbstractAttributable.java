/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.persistence.beans;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractAttributable extends AbstractSysInfo {

    private static final long serialVersionUID = -4801685541488201119L;

    @SuppressWarnings("unchecked")
    public <T extends AbstractAttr> T getAttr(final String schemaName) {
        T result = null;
        for (Iterator<? extends AbstractAttr> itor = getAttrs().iterator(); result == null && itor.hasNext();) {
            final T attribute = (T) itor.next();
            if (attribute.getSchema() != null && schemaName.equals(attribute.getSchema().getName())) {
                result = attribute;
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractDerAttr> T getDerAttr(final String derSchemaName) {
        T result = null;
        for (Iterator<? extends AbstractDerAttr> itor = getDerAttrs().iterator();
                result == null && itor.hasNext();) {

            T derAttr = (T) itor.next();
            if (derAttr.getSchema() != null
                    && derSchemaName.equals(derAttr.getSchema().getName())) {

                result = derAttr;
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractVirAttr> T getVirAttr(final String virSchemaName) {
        T result = null;
        for (Iterator<? extends AbstractVirAttr> itor = getVirAttrs().iterator();
                result == null && itor.hasNext();) {

            T virAttr = (T) itor.next();
            if (virAttr.getSchema() != null
                    && virSchemaName.equals(virAttr.getSchema().getName())) {

                result = virAttr;
            }
        }

        return result;
    }

    protected Map<AbstractNormalSchema, AbstractAttr> getAttrMap() {
        final Map<AbstractNormalSchema, AbstractAttr> map = new HashMap<AbstractNormalSchema, AbstractAttr>();

        for (AbstractAttr attr : getAttrs()) {
            map.put(attr.getSchema(), attr);
        }

        return map;
    }

    protected Map<AbstractDerSchema, AbstractDerAttr> getDerAttrMap() {
        final Map<AbstractDerSchema, AbstractDerAttr> map = new HashMap<AbstractDerSchema, AbstractDerAttr>();

        for (AbstractDerAttr attr : getDerAttrs()) {
            map.put(attr.getSchema(), attr);
        }

        return map;
    }

    protected Map<AbstractVirSchema, AbstractVirAttr> getVirAttrMap() {
        final Map<AbstractVirSchema, AbstractVirAttr> map = new HashMap<AbstractVirSchema, AbstractVirAttr>();

        for (AbstractVirAttr attr : getVirAttrs()) {
            map.put(attr.getSchema(), attr);
        }

        return map;
    }

    public abstract Long getId();

    public abstract <T extends AbstractAttr> boolean addAttr(T attribute);

    public abstract <T extends AbstractAttr> boolean removeAttr(T attribute);

    public abstract List<? extends AbstractAttr> getAttrs();

    public abstract void setAttrs(List<? extends AbstractAttr> attributes);

    public abstract <T extends AbstractDerAttr> boolean addDerAttr(T derivedAttribute);

    public abstract <T extends AbstractDerAttr> boolean removeDerAttr(T derivedAttribute);

    public abstract List<? extends AbstractDerAttr> getDerAttrs();

    public abstract void setDerAttrs(List<? extends AbstractDerAttr> derivedAttributes);

    public abstract <T extends AbstractVirAttr> boolean addVirAttr(T virtualAttributes);

    public abstract <T extends AbstractVirAttr> boolean removeVirAttr(T virtualAttribute);

    public abstract List<? extends AbstractVirAttr> getVirAttrs();

    public abstract void setVirAttrs(List<? extends AbstractVirAttr> virtualAttributes);

    protected abstract Set<ExternalResource> internalGetResources();

    public boolean addResource(final ExternalResource resource) {
        return internalGetResources().add(resource);
    }

    public boolean removeResource(final ExternalResource resource) {
        return internalGetResources().remove(resource);
    }

    public Set<ExternalResource> getResources() {
        return internalGetResources();
    }

    public Set<String> getResourceNames() {
        Set<ExternalResource> ownResources = getResources();

        Set<String> result = new HashSet<String>(ownResources.size());
        for (ExternalResource resource : ownResources) {
            result.add(resource.getName());
        }

        return result;
    }

    public void setResources(final Set<ExternalResource> resources) {
        internalGetResources().clear();
        if (resources != null) {
            internalGetResources().addAll(resources);
        }
    }
}
