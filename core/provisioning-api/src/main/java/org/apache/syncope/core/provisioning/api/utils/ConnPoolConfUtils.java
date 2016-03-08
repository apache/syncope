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
package org.apache.syncope.core.provisioning.api.utils;

import org.apache.syncope.common.lib.to.ConnPoolConfTO;
import org.apache.syncope.core.persistence.api.entity.ConnPoolConf;
import org.identityconnectors.common.pooling.ObjectPoolConfiguration;

public final class ConnPoolConfUtils {

    public static ConnPoolConf getConnPoolConf(final ConnPoolConfTO cpcto, final ConnPoolConf cpc) {
        ObjectPoolConfiguration opc = new ObjectPoolConfiguration();

        cpc.setMaxIdle(cpcto.getMaxIdle() == null ? opc.getMaxIdle() : cpcto.getMaxIdle());
        cpc.setMaxObjects(cpcto.getMaxObjects() == null ? opc.getMaxObjects() : cpcto.getMaxObjects());
        cpc.setMaxWait(cpcto.getMaxWait() == null ? opc.getMaxWait() : cpcto.getMaxWait());
        cpc.setMinEvictableIdleTimeMillis(cpcto.getMinEvictableIdleTimeMillis() == null
                ? opc.getMinEvictableIdleTimeMillis() : cpcto.getMinEvictableIdleTimeMillis());
        cpc.setMinIdle(cpcto.getMinIdle() == null ? opc.getMinIdle() : cpcto.getMinIdle());

        return cpc;
    }

    public static ObjectPoolConfiguration getObjectPoolConfiguration(final ConnPoolConf cpc) {
        ObjectPoolConfiguration opc = new ObjectPoolConfiguration();
        updateObjectPoolConfiguration(opc, cpc);
        return opc;
    }

    public static void updateObjectPoolConfiguration(
            final ObjectPoolConfiguration opc, final ConnPoolConf cpc) {

        if (cpc.getMaxIdle() != null) {
            opc.setMaxIdle(cpc.getMaxIdle());
        }
        if (cpc.getMaxObjects() != null) {
            opc.setMaxObjects(cpc.getMaxObjects());
        }
        if (cpc.getMaxWait() != null) {
            opc.setMaxWait(cpc.getMaxWait());
        }
        if (cpc.getMinEvictableIdleTimeMillis() != null) {
            opc.setMinEvictableIdleTimeMillis(cpc.getMinEvictableIdleTimeMillis());
        }
        if (cpc.getMinIdle() != null) {
            opc.setMinIdle(cpc.getMinIdle());
        }
    }

    private ConnPoolConfUtils() {
        // empty constructor for static utility class
    }
}
