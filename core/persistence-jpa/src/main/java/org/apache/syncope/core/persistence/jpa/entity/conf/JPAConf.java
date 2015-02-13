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
package org.apache.syncope.core.persistence.jpa.entity.conf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.Valid;
import org.apache.syncope.core.persistence.api.entity.DerAttr;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.conf.Conf;
import org.apache.syncope.core.persistence.jpa.entity.AbstractAttributable;

@Entity
@Table(name = JPAConf.TABLE)
@Cacheable
public class JPAConf extends AbstractAttributable<CPlainAttr, DerAttr, VirAttr> implements Conf {

    private static final long serialVersionUID = 7671699609879382195L;

    public static final String TABLE = "SyncopeConf";

    @Id
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPACPlainAttr> plainAttrs;

    public JPAConf() {
        super();

        plainAttrs = new ArrayList<>();
    }

    @Override
    public Long getKey() {
        return id;
    }

    @Override
    public void setKey(final Long key) {
        this.id = key;
    }

    @Override
    public boolean addPlainAttr(final CPlainAttr attr) {
        checkType(attr, JPACPlainAttr.class);
        return plainAttrs.add((JPACPlainAttr) attr);
    }

    @Override
    public boolean removePlainAttr(final CPlainAttr attr) {
        checkType(attr, JPACPlainAttr.class);
        return plainAttrs.remove((JPACPlainAttr) attr);
    }

    @Override
    public List<? extends CPlainAttr> getPlainAttrs() {
        return plainAttrs;
    }

    @Override
    public boolean addDerAttr(final DerAttr attr) {
        return false;
    }

    @Override
    public boolean removeDerAttr(final DerAttr derAttr) {
        return false;
    }

    @Override
    public List<? extends DerAttr> getDerAttrs() {
        return Collections.emptyList();
    }

    @Override
    public boolean addVirAttr(final VirAttr attr) {
        return false;
    }

    @Override
    public boolean removeVirAttr(final VirAttr virAttr) {
        return false;
    }

    @Override
    public List<? extends VirAttr> getVirAttrs() {
        return Collections.emptyList();
    }

}
