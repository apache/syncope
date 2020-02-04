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
package org.apache.syncope.core.provisioning.api.propagation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.provisioning.api.AbstractTest;
import org.apache.syncope.core.provisioning.api.Connector;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class PropagationTaskInfoTest extends AbstractTest {

    @Mock
    private ExternalResource externalResource;

    @Mock
    private transient Connector connector;

    @InjectMocks
    private PropagationTaskInfo propagationTaskInfo;

    @Test
    public void test(@Mock Optional<ConnectorObject> beforeObj) {
        PropagationTaskInfo propagationTaskInfo2 = new PropagationTaskInfo(externalResource);
        Object nullObj = null;

        assertTrue(propagationTaskInfo2.equals(propagationTaskInfo2));
        assertTrue(propagationTaskInfo2.equals(propagationTaskInfo));
        assertFalse(propagationTaskInfo.equals(nullObj));
        assertFalse(propagationTaskInfo.equals(String.class));
        assertEquals(propagationTaskInfo.hashCode(), propagationTaskInfo2.hashCode());
        assertEquals(connector, propagationTaskInfo.getConnector());

        propagationTaskInfo2.setConnector(connector);
        assertEquals(connector, propagationTaskInfo2.getConnector());
        assertEquals(externalResource.getClass(), propagationTaskInfo.getExternalResource().getClass());

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> propagationTaskInfo.setResource("testResource"));
        assertEquals(exception.getClass(), IllegalArgumentException.class);
        assertNull(propagationTaskInfo2.getResource());
        
        propagationTaskInfo.setBeforeObj(beforeObj);
        assertEquals(beforeObj, propagationTaskInfo.getBeforeObj());
    }
}
