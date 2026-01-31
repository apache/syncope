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

import jakarta.persistence.EntityManagerFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.syncope.core.persistence.api.dao.PersistenceInfoDAO;
import org.hibernate.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JPAPersistenceInfoDAO implements PersistenceInfoDAO {

    protected static final Logger LOG = LoggerFactory.getLogger(PersistenceInfoDAO.class);

    protected final EntityManagerFactory entityManagerFactory;

    public JPAPersistenceInfoDAO(final EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public Map<String, Object> info() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("vendor", Version.class.getPackage().getImplementationVendor());
        result.put("version", Version.class.getPackage().getImplementationVersion());
        result.put("title", Version.class.getPackage().getImplementationTitle());

        return result;
    }
}
