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
package org.apache.syncope.core.rest.cxf.service;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.info.NumbersInfo;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.syncope.core.logic.SyncopeLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SyncopeServiceImpl extends AbstractServiceImpl implements SyncopeService {

    @Autowired
    private SyncopeLogic logic;

    @Override
    public PlatformInfo platform() {
        return logic.platform();
    }

    @Override
    public SystemInfo system() {
        return logic.system();
    }

    @Override
    public NumbersInfo numbers() {
        return logic.numbers();
    }

    @Override
    public PagedResult<GroupTO> searchAssignableGroups(
            final String realm, final int page, final int size) {

        Pair<Integer, List<GroupTO>> result = logic.searchAssignableGroups(
                StringUtils.prependIfMissing(realm, SyncopeConstants.ROOT_REALM), page, size);
        return buildPagedResult(result.getRight(), page, size, result.getLeft());
    }

    @Override
    public TypeExtensionTO readUserTypeExtension(final String groupName) {
        return logic.readTypeExtension(groupName);
    }

}
