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
package org.apache.syncope.core.persistence.jpa.dao;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.util.ImplHelper;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.conf.Conf;

public class JPAJSONConfDAO extends JPAConfDAO {

    /**
     * Marks the {@code plainAttrs} field as dirty, to force OpenJPA generating an update statement on the
     * SyncopeConf table - otherwise no update on the table itself would be generated when adding an attribute,
     * as the {@code plainAttrs} JSON field gets updated by the entity listener.
     */
    private void dirten() {
        PersistenceCapable pc = ImplHelper.toPersistenceCapable(get(), null);
        if (pc != null) {
            pc.pcGetStateManager().dirty("plainAttrs");
        }
    }

    @Override
    public Conf save(final CPlainAttr attr) {
        dirten();
        return super.save(attr);
    }

    @Override
    public Conf delete(final String key) {
        dirten();
        return super.delete(key);
    }
}
