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
package org.syncope.core.persistence.dao.impl;

import java.util.List;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.dao.MembershipDAO;

@Repository
public class MembershipDAOImpl extends AbstractDAOImpl
        implements MembershipDAO {

    @Override
    public Membership find(Long id) {
        return entityManager.find(Membership.class, id);
    }

    @Override
    public List<Membership> findAll() {
        Query query = entityManager.createQuery("SELECT e FROM Membership e");
        return query.getResultList();
    }

    @Override
    @Transactional
    public Membership save(Membership membership) {
        return entityManager.merge(membership);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Membership membership = find(id);
        if (id == null) {
            return;
        }

        membership.getSyncopeUser().removeMembership(membership);
        membership.setSyncopeUser(null);
        membership.getSyncopeRole().removeMembership(membership);
        membership.setSyncopeRole(null);

        entityManager.remove(membership);
    }
}
