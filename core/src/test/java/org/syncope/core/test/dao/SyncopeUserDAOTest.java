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
package org.syncope.core.test.dao;

import java.util.List;
import org.junit.Test;
import org.syncope.core.beans.SyncopeUser;
import org.syncope.core.dao.SyncopeUserDAO;

public class SyncopeUserDAOTest extends AbstractDAOTest {

    public SyncopeUserDAOTest() {
        super("syncopeUserDAO", "SyncopeUserDAOImpl");
    }

    @Override
    protected SyncopeUserDAO getDAO() {
        return (SyncopeUserDAO) dao;
    }

    @Test
    public final void testFindAll() {
        List<SyncopeUser> list = getDAO().findAll();
        assertEquals("did not get expected number of users ", 3, list.size());
    }

    @Test
    public final void testFindById() {
        SyncopeUser user = getDAO().find(1L);
        assertNotNull("did not find expected user", user);
        user = getDAO().find(3L);
        assertNotNull("did not find expected user", user);
        user = getDAO().find(4L);
        assertNull("found user but did not expect it", user);
    }

    @Test
    public final void testSave() {
        SyncopeUser user = new SyncopeUser();

        user = getDAO().save(user);

        SyncopeUser actual = getDAO().find(user.getId());
        assertNotNull("expected save to work", actual);
    }

    @Test
    public final void testDelete() {
        SyncopeUser user = getDAO().find(1L);

        getDAO().delete(user.getId());

        SyncopeUser actual = getDAO().find(1L);
        assertNull("delete did not work", actual);
    }
}
