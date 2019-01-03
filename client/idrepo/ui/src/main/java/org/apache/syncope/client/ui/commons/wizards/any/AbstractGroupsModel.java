/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.client.ui.commons.wizards.any;

import java.util.List;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.wicket.model.util.ListModel;

public abstract class AbstractGroupsModel extends ListModel<GroupTO> {

    private static final long serialVersionUID = -4541954630939063927L;

    protected List<GroupTO> groups;

    protected List<MembershipTO> memberships;

    protected String realm;

    @Override
    public List<GroupTO> getObject() {
        reload();
        return groups;
    }

    /**
     * Retrieve the first MAX_GROUP_LIST_CARDINALITY assignable.
     */
    protected abstract void reloadObject();

    public List<MembershipTO> getMemberships() {
        reload();
        return memberships;
    }

    /**
     * Retrieve group memberships.
     */
    protected abstract void reloadMemberships();

    public abstract List<String> getDynMemberships();

    /**
     * Retrieve dyn group memberships.
     */
    protected abstract void reloadDynMemberships();

    /**
     * Reload data if the realm changes (see SYNCOPE-1135).
     */
    protected abstract void reload();
}
