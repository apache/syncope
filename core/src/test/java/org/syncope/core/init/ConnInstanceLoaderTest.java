/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.init;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.dao.ConnInstanceDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.propagation.ConnectorFacadeProxy;
import org.syncope.core.util.ApplicationContextManager;

public class ConnInstanceLoaderTest {

    private ConnInstanceLoader cil;

    private ConnInstanceDAO connInstanceDAOMock;

    private ResourceDAO resourceDAOMock;

    @Before
    public void before() {
        ApplicationContextManager.setApplicationContext(new StaticApplicationContext());
        cil = new ConnInstanceLoader();
        connInstanceDAOMock = mock(ConnInstanceDAO.class);
        resourceDAOMock = mock(ResourceDAO.class);
        ReflectionTestUtils.setField(cil, "connInstanceDAO",
                connInstanceDAOMock);
        ReflectionTestUtils.setField(cil, "resourceDAO", resourceDAOMock);
    }

    @Test
    public void loadEmpty() {
        when(resourceDAOMock.findAll()).thenReturn(
                Collections.<TargetResource>emptyList());
        cil.load();
        assertEquals(0, ApplicationContextManager.getApplicationContext().
                getBeanNamesForType(ConnectorFacadeProxy.class).length);
    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void getConnectorWhenEmpty() {
        ConnInstance instance = new ConnInstance();
        TargetResource resource = new TargetResource();
        resource.setConnector(instance);
        cil.getConnector(resource);
    }
}
