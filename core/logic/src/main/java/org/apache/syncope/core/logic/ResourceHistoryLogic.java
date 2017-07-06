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
package org.apache.syncope.core.logic;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.common.lib.to.ResourceHistoryConfTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceHistoryConfDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResourceHistoryConf;
import org.apache.syncope.core.provisioning.api.data.ResourceDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class ResourceHistoryLogic extends AbstractTransactionalLogic<ResourceHistoryConfTO> {

    @Autowired
    private ExternalResourceHistoryConfDAO resourceHistoryConfDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private ResourceDataBinder binder;

    private ResourceHistoryConfTO getResourceHistoryConfTO(final ExternalResourceHistoryConf history) {
        ResourceHistoryConfTO historyTO = new ResourceHistoryConfTO();
        historyTO.setKey(history.getKey());
        historyTO.setCreator(history.getCreator());
        historyTO.setCreation(history.getCreation());
        historyTO.setResourceTO(history.getConf());

        return historyTO;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.RESOURCE_HISTORY_LIST + "')")
    public List<ResourceHistoryConfTO> list(final String key) {
        ExternalResource resource = resourceDAO.find(key);
        if (resource == null) {
            throw new NotFoundException("Resource '" + key + "'");
        }

        return CollectionUtils.collect(resourceHistoryConfDAO.findByEntity(resource),
                new Transformer<ExternalResourceHistoryConf, ResourceHistoryConfTO>() {

            @Override
            public ResourceHistoryConfTO transform(final ExternalResourceHistoryConf input) {
                return getResourceHistoryConfTO(input);
            }
        }, new ArrayList<ResourceHistoryConfTO>());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.RESOURCE_HISTORY_RESTORE + "')")
    public void restore(final String key) {
        ExternalResourceHistoryConf resourceHistoryConf = resourceHistoryConfDAO.find(key);
        if (resourceHistoryConf == null) {
            throw new NotFoundException("Resource History Conf '" + key + "'");
        }

        binder.update(resourceHistoryConf.getEntity(), resourceHistoryConf.getConf());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.RESOURCE_HISTORY_DELETE + "')")
    public void delete(final String key) {
        ExternalResourceHistoryConf resourceHistoryConf = resourceHistoryConfDAO.find(key);
        if (resourceHistoryConf == null) {
            throw new NotFoundException("Resource History Conf '" + key + "'");
        }

        resourceHistoryConfDAO.delete(key);
    }

    @Override
    protected ResourceHistoryConfTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        if (!"list".equals(method.getName())) {
            try {
                String key = (String) args[0];
                return getResourceHistoryConfTO(resourceHistoryConfDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }

}
