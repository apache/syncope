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
package org.syncope.console.commons;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.ConnObjectTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.client.to.UserTO;
import org.syncope.console.rest.ResourceRestClient;
import org.syncope.console.rest.UserRestClient;
import org.syncope.types.IntMappingType;

public class StatusUtils {

    /**
     * Logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(StatusUtils.class);

    @Autowired
    private UserRestClient userRestClient;

    @Autowired
    private ResourceRestClient resourceRestClient;

    public enum Status {

        ACTIVE,
        SUSPENDED,
        UNDEFINED,
        USER_NOT_FOUND;

        public boolean isActive() {
            return this == ACTIVE;
        }
    }

    public List<StatusBean> getRemoteStatuses(final UserTO userTO) {
        final List<StatusBean> statuses = new ArrayList<StatusBean>();

        for (String res : userTO.getResources()) {

            final ResourceTO resourceTO = resourceRestClient.read(res);

            final Map.Entry<IntMappingType, String> accountId =
                    getAccountId(resourceTO);

            String objectId = null;

            switch (accountId != null
                    ? accountId.getKey() : IntMappingType.SyncopeUserId) {

                case SyncopeUserId:
                    objectId = String.valueOf(userTO.getId());
                    break;
                case Username:
                    objectId = userTO.getUsername();
                    break;
                case UserSchema:
                    AttributeTO attributeTO =
                            userTO.getAttributeMap().get(accountId.getValue());
                    objectId =
                            attributeTO != null
                            && attributeTO.getValues() != null
                            && !attributeTO.getValues().isEmpty()
                            ? attributeTO.getValues().get(0) : null;
                    break;
                case UserDerivedSchema:
                    attributeTO = userTO.getDerivedAttributeMap().
                            get(accountId.getValue());
                    objectId =
                            attributeTO != null
                            && attributeTO.getValues() != null
                            && !attributeTO.getValues().isEmpty()
                            ? attributeTO.getValues().get(0) : null;
                    break;
                case UserVirtualSchema:
                    attributeTO = userTO.getVirtualAttributeMap().
                            get(accountId.getValue());
                    objectId =
                            attributeTO != null
                            && attributeTO.getValues() != null
                            && !attributeTO.getValues().isEmpty()
                            ? attributeTO.getValues().get(0) : null;
                    break;
                default:
            }

            ConnObjectTO objectTO = null;

            try {
                objectTO = userRestClient.getRemoteObject(res, objectId);
            } catch (Exception e) {
                LOG.warn("User '{}' not found on resource '{}'", objectId, res);
            }

            final StatusBean statusBean = getRemoteStatus(objectTO);
            statusBean.setResourceName(res);
            statuses.add(statusBean);
        }

        return statuses;
    }

    public StatusBean getRemoteStatus(
            final ConnObjectTO objectTO) {

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

    public Boolean isEnabled(final ConnObjectTO objectTO) {
        final String STATUSATTR = "__ENABLE__";

        final Map<String, AttributeTO> attributeTOs =
                objectTO.getAttributeMap();

        final AttributeTO status = attributeTOs.get(STATUSATTR);

        return status != null && status.getValues() != null
                && !status.getValues().isEmpty()
                ? Boolean.parseBoolean(status.getValues().get(0)) : null;
    }

    public String getAccountLink(final ConnObjectTO objectTO) {
        final String NAME = "__NAME__";

        final Map<String, AttributeTO> attributeTOs = objectTO != null
                ? objectTO.getAttributeMap()
                : Collections.EMPTY_MAP;

        final AttributeTO name = attributeTOs.get(NAME);

        return name != null && name.getValues() != null
                && !name.getValues().isEmpty()
                ? (String) name.getValues().get(0) : null;
    }

    public Map.Entry<IntMappingType, String> getAccountId(
            final ResourceTO resourceTO) {
        Map.Entry<IntMappingType, String> accountId = null;

        for (SchemaMappingTO mapping : resourceTO.getMappings()) {
            if (mapping.isAccountid()) {
                accountId = new AbstractMap.SimpleEntry<IntMappingType, String>(
                        mapping.getIntMappingType(),
                        mapping.getIntAttrName());
            }
        }

        return accountId;
    }
}
