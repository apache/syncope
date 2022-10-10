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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.command.CommandOutput;
import org.apache.syncope.common.lib.command.CommandTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.rest.api.beans.CommandQuery;
import org.apache.syncope.common.rest.api.service.CommandService;
import org.apache.syncope.core.logic.CommandLogic;
import org.springframework.stereotype.Service;

@Service
public class CommandServiceImpl extends AbstractService implements CommandService {

    protected final CommandLogic logic;

    public CommandServiceImpl(final CommandLogic logic) {
        this.logic = logic;
    }

    @Override
    public PagedResult<CommandTO> search(final CommandQuery query) {
        String keyword = query.getKeyword() == null ? null : query.getKeyword().replace('*', '%');
        Pair<Integer, List<CommandTO>> result = logic.search(query.getPage(), query.getSize(), keyword);
        return buildPagedResult(result.getRight(), query.getPage(), query.getSize(), result.getLeft());
    }

    @Override
    public CommandTO read(final String key) {
        return logic.read(key);
    }

    @Override
    public CommandOutput run(final CommandTO command) {
        return new CommandOutput.Builder(command).output(logic.run(command)).build();
    }
}
