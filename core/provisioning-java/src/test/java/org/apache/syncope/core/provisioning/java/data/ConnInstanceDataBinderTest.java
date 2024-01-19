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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.provisioning.api.data.ConnInstanceDataBinder;
import org.apache.syncope.core.provisioning.java.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ConnInstanceDataBinderTest extends AbstractTest {

    @Autowired
    private ConnInstanceDataBinder binder;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Test
    public void working() {
        ConnInstanceTO connInstance = binder.getConnInstanceTO(
                connInstanceDAO.findById("88a7a819-dab5-46b4-9b90-0b9769eabdb8").orElseThrow());
        assertNotNull(connInstance);
        assertFalse(connInstance.isErrored());
        assertNotNull(connInstance.getLocation());
        assertFalse(connInstance.getConf().isEmpty());
    }

    @Test
    public void errored() {
        ConnInstanceTO connInstance = binder.getConnInstanceTO(
                connInstanceDAO.findById("413bf072-678a-41d3-9d20-8c453b3a39d1").orElseThrow());
        assertNotNull(connInstance);
        assertTrue(connInstance.isErrored());
        assertNotNull(connInstance.getLocation());
        assertTrue(connInstance.getConf().isEmpty());
    }
}
