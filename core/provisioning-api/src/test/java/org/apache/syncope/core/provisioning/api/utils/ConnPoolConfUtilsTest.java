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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;

import org.apache.syncope.common.lib.to.ConnPoolConfTO;
import org.apache.syncope.core.persistence.api.entity.ConnPoolConf;
import org.apache.syncope.core.provisioning.api.AbstractTest;
import org.identityconnectors.common.pooling.ObjectPoolConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class ConnPoolConfUtilsTest extends AbstractTest {

    @Mock
    private ConnPoolConf cpc;

    @Test
    public void getConnPoolConf() {
        ConnPoolConfTO cpcto = new ConnPoolConfTO();
        ConnPoolConfUtils.getConnPoolConf(cpcto, cpc);
        verify(cpc).setMaxIdle(anyInt());
        verify(cpc).setMaxObjects(anyInt());
        verify(cpc).setMaxWait(anyLong());
        verify(cpc).setMinEvictableIdleTimeMillis(anyLong());
        verify(cpc).setMinIdle(anyInt());
    }

    @Test
    public void updateObjectPoolConfiguration(@Mock ObjectPoolConfiguration opc) {
        ConnPoolConfUtils.updateObjectPoolConfiguration(opc, cpc);
        verify(opc).setMaxIdle(anyInt());
        verify(opc).setMaxObjects(anyInt());
        verify(opc).setMaxWait(anyLong());
        verify(opc).setMinEvictableIdleTimeMillis(anyLong());
        verify(opc).setMinIdle(anyInt());
    }
}
