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
package org.syncope.core.persistence.test;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.SyncopeRole;
import org.syncope.core.persistence.beans.AttributeSchema;
import org.syncope.core.persistence.beans.Attribute;
import org.syncope.core.persistence.beans.SyncopeRolePK;
import org.syncope.core.persistence.dao.AttributeSchemaDAO;
import org.syncope.core.persistence.dao.AttributeDAO;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;

@Transactional
public class SyncopeRoleDAOTest extends AbstractDAOTest {

    @Autowired
    SyncopeRoleDAO syncopeRoleDAO;
    @Autowired
    AttributeDAO attributeDAO;
    @Autowired
    AttributeSchemaDAO attributeSchemaDAO;

    @Test
    public final void testFindAll() {
        List<SyncopeRole> list = syncopeRoleDAO.findAll();
        assertEquals("did not get expected number of roles ", 2, list.size());
    }

    @Test
    public final void testFind() {
        SyncopeRole role = syncopeRoleDAO.find("root", null);
        assertNotNull("did not find expected role", role);
        role = syncopeRoleDAO.find(new SyncopeRolePK(null, null));
        assertNull("found role but did not expect it", role);
    }

    @Test
    public final void testSave() {
        SyncopeRolePK rolePK = new SyncopeRolePK("secondChild", "root");
        SyncopeRole role = new SyncopeRole();
        role.setSyncopeRolePK(rolePK);

        role = syncopeRoleDAO.save(role);

        SyncopeRole actual = syncopeRoleDAO.find(role.getSyncopeRolePK());
        assertNotNull("expected save to work", actual);
    }

    @Test
    public final void testDelete() {
        SyncopeRole role = syncopeRoleDAO.find("child", "root");

        syncopeRoleDAO.delete(role.getSyncopeRolePK());

        SyncopeRole actual = syncopeRoleDAO.find("child", "root");
        assertNull("delete did not work", actual);
    }

    @Test
    public final void testRelationships() {
        SyncopeRole role = syncopeRoleDAO.find("root", null);
        Set<Attribute> attributes = role.getAttributes();
        int originalAttributesSize = attributes.size();
        Attribute attribute = attributes.iterator().next();

        // Remove an attribute from its table: we expect not to find it
        // associated with the user
        attributeDAO.delete(attribute.getId());
        assertNull(attributeDAO.find(attribute.getId()));
        assertEquals("unexpected number of attributes",
                originalAttributesSize - 1, role.getAttributes().size());

        // Remove an attribute association with a user: we expect not to
        // have it on the db table as well
        attribute = role.getAttributes().iterator().next();
        role.removeAttribute(attribute);
        syncopeRoleDAO.save(role);
        assertNull(attributeDAO.find(attribute.getId()));
    }
}
