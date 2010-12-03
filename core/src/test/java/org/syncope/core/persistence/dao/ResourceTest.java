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
package org.syncope.core.persistence.dao;

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.AbstractTest;
import org.syncope.identityconnectors.bundles.staticwebservice.WebServiceConnector;
import org.syncope.types.SourceMappingType;

@Transactional
public class ResourceTest extends AbstractTest {

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private TaskDAO taskDAO;

    @Test
    public final void findById() {
        TargetResource resource =
                resourceDAO.find("ws-target-resource-1");

        assertNotNull("findById did not work", resource);

        ConnectorInstance connector = resource.getConnector();

        assertNotNull("connector not found", connector);

        assertEquals("invalid connector name",
                WebServiceConnector.class.getName(),
                connector.getConnectorName());

        assertEquals("invalid bundle name",
                "org.syncope.identityconnectors.bundles.staticws",
                connector.getBundleName());

        assertEquals("invalid bundle version",
                bundlesVersion, connector.getVersion());

        List<SchemaMapping> mappings = resource.getMappings();

        assertNotNull("mappings not found", mappings);

        assertFalse("no mapping specified", mappings.isEmpty());

        List<Long> mappingIds = new ArrayList<Long>();

        for (SchemaMapping mapping : mappings) {
            mappingIds.add(mapping.getId());
        }

        assertTrue(mappingIds.contains(100L));
    }

    @Test
    public final void getAccountId() {
        SchemaMapping mapping = resourceDAO.getMappingForAccountId(
                "ws-target-resource-2");
        assertEquals("username", mapping.getSourceAttrName());
    }

    @Test
    public final void save() {
        TargetResource resource = new TargetResource();
        resource.setName("ws-target-resource-basic-save");

        SchemaMapping accountId = new SchemaMapping();
        accountId.setResource(resource);
        accountId.setAccountid(true);
        accountId.setDestAttrName("username");
        accountId.setSourceAttrName("username");
        accountId.setSourceMappingType(SourceMappingType.SyncopeUserId);

        resource.addMapping(accountId);

        // save the resource
        TargetResource actual = resourceDAO.save(resource);

        assertNotNull(actual);
    }

    @Test
    public final void delete() {
        TargetResource resource = resourceDAO.find("ws-target-resource-2");
        assertNotNull(resource);

        resourceDAO.delete(resource.getName());

        TargetResource actual = resourceDAO.find("ws-target-resource-2");
        assertNull(actual);
    }
}
