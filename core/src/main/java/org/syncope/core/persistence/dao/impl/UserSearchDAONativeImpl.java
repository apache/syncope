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
import org.springframework.stereotype.Repository;
import org.syncope.client.search.NodeCond;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.UserSearchDAO;

@Repository
public class UserSearchDAONativeImpl extends AbstractUserSearchDAOImpl
        implements UserSearchDAO {

    @Override
    protected List<SyncopeUser> doSearch(final NodeCond nodeCond) {
        return null;
    }
}
