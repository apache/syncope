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

import java.util.Collections;
import java.util.List;
import org.syncope.client.search.NodeCond;
import org.syncope.client.search.PaginatedResult;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.UserSearchDAO;

public abstract class AbstractUserSearchDAOImpl extends AbstractDAOImpl
        implements UserSearchDAO {

    @Override
    public List<SyncopeUser> search(final NodeCond searchCondition) {
        return search(searchCondition, -1, -1, null);
    }

    @Override
    public List<SyncopeUser> search(final NodeCond searchCondition,
            final int page,
            final int itemsPerPage,
            final PaginatedResult paginatedResult) {

        List<SyncopeUser> result;

        LOG.debug("Search condition:\n{}", searchCondition);
        if (!searchCondition.checkValidity()) {
            LOG.error("Invalid search condition:\n{}", searchCondition);

            result = Collections.EMPTY_LIST;
        }

        try {
            result = doSearch(searchCondition,
                    page, itemsPerPage, paginatedResult);
        } catch (Throwable t) {
            LOG.error("While searching users", t);

            result = Collections.EMPTY_LIST;
        }

        return result;
    }

    protected abstract List<SyncopeUser> doSearch(final NodeCond nodeCond,
            final int page,
            final int itemsPerPage,
            final PaginatedResult paginatedResult);
}
