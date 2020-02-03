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
package org.apache.syncope.client.console.wizards.resources;

import org.apache.syncope.client.console.panels.ToggleableTarget;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.OrgUnitTO;
import org.apache.syncope.common.lib.to.ProvisionTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ResourceProvision implements ToggleableTarget {

    private static final long serialVersionUID = 1103991919577739952L;

    private ProvisionTO provisionTO;

    private OrgUnitTO orgUnitTO;

    private List<ItemTO> items;

    public ResourceProvision() {
        this.items = new ArrayList<>();
    }

    public ResourceProvision(final ProvisionTO provisionTO) {
        setProvisionTO(provisionTO);
    }

    public ResourceProvision(final OrgUnitTO orgUnitTO) {
        setOrgUnitTO(orgUnitTO);
    }

    public ProvisionTO getProvisionTO() {
        return provisionTO;
    }

    public final void setProvisionTO(final ProvisionTO provisionTO) {
        this.provisionTO = provisionTO;
        this.orgUnitTO = null;

        if (this.items == null) {
            this.items = new ArrayList<>();
        } else {
            this.items.clear();
        }
        if (provisionTO.getMapping() != null) {
            this.items.addAll(provisionTO.getMapping().getItems());
        }
    }

    public OrgUnitTO getOrgUnitTO() {
        return orgUnitTO;
    }

    public final void setOrgUnitTO(final OrgUnitTO orgUnitTO) {
        this.orgUnitTO = orgUnitTO;
        this.provisionTO = null;

        if (this.items == null) {
            this.items = new ArrayList<>();
        } else {
            this.items.clear();
        }
        this.items.addAll(orgUnitTO.getItems());
    }

    @Override
    public String getKey() {
        return provisionTO == null
            ? Optional.ofNullable(orgUnitTO).map(OrgUnitTO::getKey).orElse(null)
            : provisionTO.getKey();
    }

    @Override
    public String getAnyType() {
        return provisionTO == null
            ? orgUnitTO == null
            ? null
            : SyncopeConstants.REALM_ANYTYPE : provisionTO.getAnyType();
    }

    public void setAnyType(final String anyType) {
        if (SyncopeConstants.REALM_ANYTYPE.equals(anyType)) {
            setOrgUnitTO(new OrgUnitTO());
        } else {
            setProvisionTO(new ProvisionTO());
            getProvisionTO().setAnyType(anyType);
            getProvisionTO().setMapping(new MappingTO());
        }
    }

    public String getObjectClass() {
        return provisionTO == null
            ? Optional.ofNullable(orgUnitTO)
            .map(OrgUnitTO::getObjectClass)
            .orElse(null) : provisionTO.getObjectClass();
    }

    public void setObjectClass(final String objectClass) {
        if (provisionTO == null) {
            orgUnitTO.setObjectClass(objectClass);
        } else {
            provisionTO.setObjectClass(objectClass);
        }
    }

    public List<String> getAuxClasses() {
        return provisionTO == null ? List.of() : provisionTO.getAuxClasses();
    }

    public boolean isIgnoreCaseMatch() {
        return Optional.ofNullable(provisionTO)
            .map(ProvisionTO::isIgnoreCaseMatch).orElseGet(() -> orgUnitTO.isIgnoreCaseMatch());
    }

    public void setIgnoreCaseMatch(final boolean ignoreCaseMatch) {
        if (provisionTO == null) {
            orgUnitTO.setIgnoreCaseMatch(ignoreCaseMatch);
        } else {
            provisionTO.setIgnoreCaseMatch(ignoreCaseMatch);
        }
    }

    public String getConnObjectLink() {
        return provisionTO == null
            ? Optional.ofNullable(orgUnitTO).map(OrgUnitTO::getConnObjectLink).orElse(null)
            : provisionTO.getMapping().getConnObjectLink();
    }

    public void setConnObjectLink(final String connObjectLink) {
        if (provisionTO == null) {
            orgUnitTO.setConnObjectLink(connObjectLink);
        } else {
            provisionTO.getMapping().setConnObjectLink(connObjectLink);
        }
    }

    public List<ItemTO> getItems() {
        return items;
    }

}
