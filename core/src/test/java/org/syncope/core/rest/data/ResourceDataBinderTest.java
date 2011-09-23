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
package org.syncope.core.rest.data;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.syncope.client.to.ResourceTO;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.types.ConnConfProperty;

public class ResourceDataBinderTest {

    private ResourceDataBinder rdb;

    private ResourceTO resourceTO;

    private TargetResource resource;

    @Before
    public void before() {
        rdb = new ResourceDataBinder();
        resourceTO = new ResourceTO();
        resource = new TargetResource();
    }

    @Test
    public void getResource() {
        Set<ConnConfProperty> props = new HashSet<ConnConfProperty>();
        resourceTO.setConnectorConfigurationProperties(props);

        TargetResource res = rdb.getResource(resourceTO);

        assertEquals("configuration properties should be filled",
                props, res.getConfiguration());
    }

    @Test
    public void getResourceTO() {
        Set<ConnConfProperty> props = new HashSet<ConnConfProperty>();
        resource.setConnectorConfigurationProperties(props);

        ResourceTO resTO = rdb.getResourceTO(resource);

        assertEquals("configuration properties should be filled",
                props, resTO.getConnectorConfigurationProperties());
    }
}
