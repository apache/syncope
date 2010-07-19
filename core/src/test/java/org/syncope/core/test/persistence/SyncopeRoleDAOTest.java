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
package org.syncope.core.test.persistence;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;

@Transactional
public class SyncopeRoleDAOTest extends AbstractTest {

    @Autowired
    private SyncopeRoleDAO syncopeRoleDAO;

    @Test
    public final void findAll() {
        List<SyncopeRole> list = syncopeRoleDAO.findAll();
        assertEquals("did not get expected number of roles ", 8, list.size());
    }

    @Test
    public final void findChildren() {
        assertEquals(2, syncopeRoleDAO.findChildren(4L).size());
    }

    @Test
    public final void find() {
        SyncopeRole role = syncopeRoleDAO.find("root", null);
        assertNotNull("did not find expected role", role);
        role = syncopeRoleDAO.find(null, null);
        assertNull("found role but did not expect it", role);
    }

    @Test
    public final void save() {
        SyncopeRole role = new SyncopeRole();
        role.setName("secondChild");

        SyncopeRole rootRole = syncopeRoleDAO.find("root", null);
        role.setParent(rootRole);

        role = syncopeRoleDAO.save(role);

        SyncopeRole actual = syncopeRoleDAO.find(role.getId());
        assertNotNull("expected save to work", actual);
    }

    @Test
    public final void delete() {
        SyncopeRole role = syncopeRoleDAO.find(4L);
        syncopeRoleDAO.delete(role.getId());

        SyncopeRole actual = syncopeRoleDAO.find(4L);
        assertNull("delete did not work", actual);

        SyncopeRole children = syncopeRoleDAO.find(7L);
        assertNull("delete of successors did not work", children);

    }
}
