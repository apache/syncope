/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.syncope.core.util;

import java.util.Date;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import org.apache.syncope.core.persistence.beans.AbstractSysInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SysInfoListener {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(SysInfoListener.class);

    @PrePersist
    @PreUpdate
    public void setSysInfo(final AbstractSysInfo entity) {
        try {
            final String username = EntitlementUtil.getAuthenticatedUsername();
            LOG.debug("Set system properties for '{}'", entity);

            final Date now = new Date();

            if (entity.getCreationDate() == null) {
                LOG.debug("Set creation date '{}' and creator '{}' for '{}'", new Object[] {now, username, entity});
                entity.setCreationDate(now);
                entity.setCreator(username);
            }

            LOG.debug("Set last change date '{}' and modifier '{}' for '{}'", new Object[] {now, username, entity});
            entity.setLastModifier(username);
            entity.setLastChangeDate(now);
        } catch (Exception e) {
            // In case of exception, do not trace create/update event info: maybe it's an internal management action.
            // See SyncopeAuthenticationProvider for instance.
            LOG.info("Unauthenticated user action: no system property to be update");
        }
    }
}
