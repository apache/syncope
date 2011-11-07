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

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.Entitlement;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.AbstractTest;

@Transactional
public class EntitlementTest extends AbstractTest {

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Test
    public final void findAll() {
        List<Entitlement> list = entitlementDAO.findAll();
        // 53 real entitlements + 9 role entitlements
        assertEquals("did not get expected number of entitlements ",
                68, list.size());
    }

    @Test
    public final void findByName() {
        Entitlement entitlement = entitlementDAO.find("base");
        assertNotNull("did not find expected entitlement",
                entitlement);
    }

    @Test
    public final void save() {
        Entitlement entitlement = new Entitlement();
        entitlement.setName("another");

        entitlementDAO.save(entitlement);

        Entitlement actual = entitlementDAO.find("another");
        assertNotNull("expected save to work", actual);
        assertEquals(entitlement, actual);
    }

    @Test
    public final void delete() {
        Entitlement entitlement = entitlementDAO.find("base");
        assertNotNull("did not find expected entitlement",
                entitlement);

        Set<SyncopeRole> roles = entitlement.getRoles();
        assertEquals("expected two roles", 2, roles.size());

        entitlementDAO.delete("base");
        for (SyncopeRole role : roles) {
            assertFalse(role.getEntitlements().contains(entitlement));
        }
    }
}
