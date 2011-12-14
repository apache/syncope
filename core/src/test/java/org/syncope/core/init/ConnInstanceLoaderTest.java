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

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.AbstractTest;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.propagation.ConnectorFacadeProxy;
import org.syncope.core.util.ApplicationContextManager;
import org.syncope.core.util.ConnBundleManager;

@Transactional
public class ConnInstanceLoaderTest extends AbstractTest {

    private ConnInstanceLoader cil;

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private ConnBundleManager connBundleManager;

    @Before
    public void before() {
        cil = new ConnInstanceLoader();
        ReflectionTestUtils.setField(cil, "resourceDAO", resourceDAO);
        ReflectionTestUtils.setField(cil, "connBundleManager", connBundleManager);

        // Remove any other connector instance bean set up by
        // standard ConnInstanceLoader.load()
        for (String bean : ApplicationContextManager.getApplicationContext().
                getBeanNamesForType(ConnectorFacadeProxy.class)) {

            cil.unregisterConnector(bean);
        }
    }

    @Test
    public void load() {
        cil.load();

        assertEquals(resourceDAO.findAll().size(),
                ApplicationContextManager.getApplicationContext().
                getBeanNamesForType(ConnectorFacadeProxy.class).length);
    }
}
