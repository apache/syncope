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
package org.apache.syncope.common.lib.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConnPoolConfTest {

    @Test
    void pool() {
        ConnPoolConf pool = new ConnPoolConf();

        pool.setMaxObjects(10);
        pool.setMinIdle(2);
        pool.setMaxIdle(5);
        pool.setMaxWait(3L);
        pool.setMinEvictableIdleTimeMillis(60L);

        assertEquals(10, pool.getMaxObjects().intValue());
        assertEquals(2, pool.getMinIdle().intValue());
        assertEquals(5, pool.getMaxIdle().intValue());
        assertEquals(3L, pool.getMaxWait());
        assertEquals(60L, pool.getMinEvictableIdleTimeMillis());

        ConnPoolConf same = new ConnPoolConf();
        same.setMaxObjects(10);
        same.setMinIdle(2);
        same.setMaxIdle(5);
        same.setMaxWait(3L);
        same.setMinEvictableIdleTimeMillis(60L);

        ConnPoolConf other = new ConnPoolConf();
        other.setMaxObjects(20);

        assertEquals(pool, pool);
        assertTrue(!pool.equals(null));
        assertEquals(pool, same);
        assertEquals(pool.hashCode(), same.hashCode());
        assertTrue(!pool.equals(other));
    }

    @Test
    void poolWithNulls() {
        ConnPoolConf pool = new ConnPoolConf();
        assertTrue(pool.getMaxObjects() == null);
        assertTrue(pool.getMinIdle() == null);
        assertTrue(pool.getMaxIdle() == null);
        assertTrue(pool.getMaxWait() == null);
        assertTrue(pool.getMinEvictableIdleTimeMillis() == null);
    }
}
