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
package org.apache.syncope.core.starter.actuate;

import java.util.Map;
import org.apache.syncope.core.persistence.api.dao.EntityCacheDAO;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Endpoint(id = "entityCache")
public class EntityCacheEndpoint {

    protected final EntityCacheDAO entityCacheDAO;

    public EntityCacheEndpoint(final EntityCacheDAO entityCacheDAO) {
        this.entityCacheDAO = entityCacheDAO;
    }

    @ReadOperation
    public Map<String, Object> statistics() {
        return entityCacheDAO.getStatistics();
    }

    @WriteOperation
    public void statistics(final @Selector String operation) {
        switch (operation) {
            case "enable":
            case "ENABLE":
                entityCacheDAO.enableStatistics();
                break;

            case "disable":
            case "DISABLE":
                entityCacheDAO.disableStatistics();
                break;

            case "reset":
            case "RESET":
                entityCacheDAO.resetStatistics();
                break;

            default:
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unsupported Operation: " + operation);
        }
    }

    @DeleteOperation
    public void clearCache() {
        entityCacheDAO.clearCache();
    }
}
