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
package org.apache.syncope.core.provisioning.java.propagation;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class LDAPNestedRealmPropagationActions extends DefaultPropagationActions {

    private static final Logger LOG = LoggerFactory.getLogger(LDAPNestedRealmPropagationActions.class);

    @Autowired
    private RealmDAO realmDAO;

    @Override
    public void before(final PropagationTask task, final ConnectorObject beforeObj) {
        super.before(task, beforeObj);
        if (task.getAnyType() == null && task.getAnyTypeKind() == null && task.getResource().getOrgUnit() != null) {
            // search realm involved
            Realm realm = realmDAO.find(task.getEntityKey());
            Name objectLinkAttr = AttributeUtil.getNameFromAttributes(task.getAttributes());
            String oldObjectLink = objectLinkAttr.getNameValue();
            String extAttrName = task.getResource().getOrgUnit().getExtAttrName();

            String[] fullPathSplitted = realm == null ? null : StringUtils.split(realm.getFullPath(), "/");
            if (fullPathSplitted != null
                    && fullPathSplitted.length > 1
                    && StringUtils.isNotBlank(oldObjectLink)
                    && StringUtils.isNotBlank(extAttrName)) {
                // if realm depth is greater than 1 adapt Object Link accordingly
                LOG.debug("{} has depth greater than 1, adapting Object Link.", task.getConnObjectKey(),
                        fullPathSplitted);
                StringBuilder newOlPrefix = new StringBuilder();

                for (int i = fullPathSplitted.length - 1; i >= 0; i--) {
                    newOlPrefix.append(extAttrName).append("=").append(fullPathSplitted[i]).append(",");
                }

                String[] olSplitted = oldObjectLink.split(extAttrName + "=" + "[a-zA-Z0-9]*,");

                if (olSplitted.length < 2) {
                    LOG.error("Unable to generate new object link starting from {}", oldObjectLink);
                } else {
                    // change Object Link name attribute
                    Set<Attribute> attributes = new HashSet<>(task.getAttributes());
                    attributes.remove(objectLinkAttr);
                    attributes.add(new Name(newOlPrefix.append(olSplitted[1]).toString()));
                    task.setAttributes(attributes);
                }
            }

        } else {
            LOG.debug("Object to propagte is not a Realm, nothing to do.");
        }
    }

}
