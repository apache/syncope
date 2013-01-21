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
package org.apache.syncope.console.commons;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.MappingItemTO;
import org.apache.syncope.common.to.MappingTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.console.rest.AbstractAttributableRestClient;
import org.apache.syncope.console.rest.ResourceRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusUtils implements Serializable {

    private static final long serialVersionUID = 7238009174387184309L;

    public enum Status {

        CREATED,
        ACTIVE,
        SUSPENDED,
        UNDEFINED,
        OBJECT_NOT_FOUND;

        public boolean isActive() {
            return this == ACTIVE;
        }
    }

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(StatusUtils.class);

    private final ResourceRestClient resourceRestClient;

    private final AbstractAttributableRestClient restClient;

    public StatusUtils(final ResourceRestClient resourceRestClient, final AbstractAttributableRestClient restClient) {
        this.resourceRestClient = resourceRestClient;
        this.restClient = restClient;
    }

    public List<StatusBean> getRemoteStatuses(final AbstractAttributableTO attributable) {
        final List<StatusBean> statuses = new ArrayList<StatusBean>();

        for (String resouceName : attributable.getResources()) {
            final ResourceTO resource = resourceRestClient.read(resouceName);

            String objectId = null;

            final Map.Entry<IntMappingType, String> accountId = getAccountId(resource, attributable);
            switch (accountId.getKey()) {

                case UserId:
                case RoleId:
                    objectId = String.valueOf(attributable.getId());
                    break;

                case Username:
                    if (attributable instanceof UserTO) {
                        objectId = ((UserTO) attributable).getUsername();
                    }
                    break;

                case RoleName:
                    if (attributable instanceof RoleTO) {
                        objectId = ((RoleTO) attributable).getName();
                    }
                    break;

                case UserSchema:
                case RoleSchema:
                    AttributeTO attributeTO = attributable.getAttributeMap().get(accountId.getValue());
                    objectId = attributeTO != null && attributeTO.getValues() != null
                            && !attributeTO.getValues().isEmpty()
                            ? attributeTO.getValues().get(0)
                            : null;
                    break;

                case UserDerivedSchema:
                case RoleDerivedSchema:
                    attributeTO = attributable.getDerivedAttributeMap().get(accountId.getValue());
                    objectId = attributeTO != null && attributeTO.getValues() != null
                            && !attributeTO.getValues().isEmpty()
                            ? attributeTO.getValues().get(0)
                            : null;
                    break;

                case UserVirtualSchema:
                case RoleVirtualSchema:
                    attributeTO = attributable.getVirtualAttributeMap().get(accountId.getValue());
                    objectId = attributeTO != null && attributeTO.getValues() != null
                            && !attributeTO.getValues().isEmpty()
                            ? attributeTO.getValues().get(0)
                            : null;
                    break;

                default:
            }

            ConnObjectTO objectTO = null;

            try {
                objectTO = restClient.getRemoteObject(resouceName, objectId);
            } catch (Exception e) {
                LOG.warn("ConnObject '{}' not found on resource '{}'", objectId, resouceName);
            }

            final StatusBean statusBean = getRemoteStatus(objectTO);
            statusBean.setResourceName(resouceName);
            statuses.add(statusBean);
        }

        return statuses;
    }

    public StatusBean getRemoteStatus(final ConnObjectTO objectTO) {

        final StatusBean statusBean = new StatusBean();

        if (objectTO != null) {
            final Boolean enabled = isEnabled(objectTO);

            final StatusUtils.Status status = enabled == null
                    ? StatusUtils.Status.UNDEFINED
                    : enabled
                    ? StatusUtils.Status.ACTIVE
                    : StatusUtils.Status.SUSPENDED;

            final String accountLink = getAccountLink(objectTO);

            statusBean.setStatus(status);
            statusBean.setAccountLink(accountLink);
        }

        return statusBean;
    }

    private Boolean isEnabled(final ConnObjectTO objectTO) {
        final String STATUSATTR = "__ENABLE__";

        final Map<String, AttributeTO> attributeTOs = objectTO.getAttributeMap();

        final AttributeTO status = attributeTOs.get(STATUSATTR);

        return status != null && status.getValues() != null && !status.getValues().isEmpty()
                ? Boolean.parseBoolean(status.getValues().get(0))
                : null;
    }

    private String getAccountLink(final ConnObjectTO objectTO) {
        final String NAME = "__NAME__";

        final Map<String, AttributeTO> attributeTOs = objectTO != null
                ? objectTO.getAttributeMap()
                : Collections.EMPTY_MAP;

        final AttributeTO name = attributeTOs.get(NAME);

        return name != null && name.getValues() != null && !name.getValues().isEmpty()
                ? (String) name.getValues().get(0)
                : null;
    }

    private Map.Entry<IntMappingType, String> getAccountId(final ResourceTO resource,
            final AbstractAttributableTO attributable) {

        Map.Entry<IntMappingType, String> accountId = null;

        MappingTO mapping = attributable instanceof UserTO ? resource.getUmapping() : resource.getRmapping();
        IntMappingType idType = attributable instanceof UserTO ? IntMappingType.UserId : IntMappingType.RoleId;

        if (mapping != null) {
            for (MappingItemTO item : mapping.getItems()) {
                if (item.isAccountid()) {
                    accountId = new AbstractMap.SimpleEntry<IntMappingType, String>(
                            item.getIntMappingType(), item.getIntAttrName());
                }
            }
        }
        if (accountId == null) {
            accountId = new AbstractMap.SimpleEntry<IntMappingType, String>(idType, null);
        }

        return accountId;
    }
}
