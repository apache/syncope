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
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.Entitlement;
import org.syncope.core.persistence.dao.EntitlementDAO;

@Transactional
public class EntitlementDAOTest extends AbstractDAOTest {

    @Autowired
    EntitlementDAO entitlementDAO;

    @Test
    public final void testFindAll() {
        List<Entitlement> list = entitlementDAO.findAll();
        assertEquals("did not get expected number of entitlements ",
                2, list.size());
    }

    @Test
    public final void testFindByName() {
        Entitlement entitlement = entitlementDAO.find("base");
        assertNotNull("did not find expected entitlement",
                entitlement);
        assertFalse("expected some role", entitlement.getRoles().isEmpty());
    }

    @Test
    public final void testSave() {
        Entitlement entitlement = new Entitlement();
        entitlement.setName("another");

        entitlementDAO.save(entitlement);

        Entitlement actual = entitlementDAO.find("another");
        assertNotNull("expected save to work", actual);
        assertEquals(entitlement, actual);
    }

    @Test
    public final void testDelete() {
        entitlementDAO.delete("advanced");

        Entitlement actual = entitlementDAO.find("advanced");
        assertNull("delete did not work", actual);
    }
}
