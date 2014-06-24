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
package org.apache.syncope.core.persistence.beans.conf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.Valid;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractDerAttr;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;

@Entity
@Cacheable
public class SyncopeConf extends AbstractAttributable {

    private static final long serialVersionUID = -5281258853142421875L;

    @Id
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<CAttr> attrs;

    public SyncopeConf() {
        super();

        attrs = new ArrayList<CAttr>();
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public <T extends AbstractAttr> boolean addAttr(final T attr) {
        if (!(attr instanceof CAttr)) {
            throw new ClassCastException("attribute is expected to be typed CAttr: " + attr.getClass().getName());
        }
        return attrs.add((CAttr) attr);
    }

    @Override
    public <T extends AbstractAttr> boolean removeAttr(final T attr) {
        if (!(attr instanceof CAttr)) {
            throw new ClassCastException("attribute is expected to be typed CAttr: " + attr.getClass().getName());
        }
        return attrs.remove((CAttr) attr);
    }

    @Override
    public List<? extends AbstractAttr> getAttrs() {
        return attrs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setAttrs(final List<? extends AbstractAttr> attrs) {
        this.attrs.clear();
        if (attrs != null && !attrs.isEmpty()) {
            this.attrs.addAll((List<CAttr>) attrs);
        }
    }

    @Override
    public <T extends AbstractDerAttr> boolean addDerAttr(final T derAttr) {
        return false;
    }

    @Override
    public <T extends AbstractDerAttr> boolean removeDerAttr(final T derAttr) {
        return false;
    }

    @Override
    public List<? extends AbstractDerAttr> getDerAttrs() {
        return Collections.emptyList();
    }

    @Override
    public void setDerAttrs(final List<? extends AbstractDerAttr> derAttrs) {
        // no support for derived attributes
    }

    @Override
    public <T extends AbstractVirAttr> boolean addVirAttr(final T virAttr) {
        return false;
    }

    @Override
    public <T extends AbstractVirAttr> boolean removeVirAttr(final T virAttr) {
        return false;
    }

    @Override
    public List<? extends AbstractVirAttr> getVirAttrs() {
        return Collections.emptyList();
    }

    @Override
    public void setVirAttrs(final List<? extends AbstractVirAttr> virAttrs) {
        // no support for virtual attributes
    }

}
