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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.core.persistence.api.dao.WAConfigDAO;
import org.apache.syncope.core.persistence.api.entity.am.WAConfigEntry;
import org.apache.syncope.core.provisioning.api.data.WAConfigDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WAConfigDataBinderTest extends AbstractDataBinderTest {

    private static final String KEY = "test.wa.config";

    @Autowired
    private WAConfigDataBinder binder;

    @Autowired
    private WAConfigDAO waConfigDAO;

    @Test
    void setGet() {
        WAConfigEntry created = binder.set(
                new Attr.Builder(KEY).value("first").value("second").build());
        waConfigDAO.save(created);
        entityManager.flush();

        Attr actual = binder.get(waConfigDAO.findById(KEY).orElseThrow());
        assertEquals(KEY, actual.getSchema());
        assertEquals(List.of("first", "second"), actual.getValues());

        WAConfigEntry updated = binder.set(new Attr.Builder(KEY).value("third").build());
        waConfigDAO.save(updated);
        entityManager.flush();

        assertEquals(List.of("third"), binder.get(waConfigDAO.findById(KEY).orElseThrow()).getValues());
    }
}
