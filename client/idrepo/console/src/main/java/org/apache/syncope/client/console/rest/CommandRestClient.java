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
package org.apache.syncope.client.console.rest;

import java.util.List;
import org.apache.syncope.common.lib.command.CommandOutput;
import org.apache.syncope.common.lib.command.CommandTO;
import org.apache.syncope.common.rest.api.beans.CommandQuery;
import org.apache.syncope.common.rest.api.service.CommandService;

public class CommandRestClient extends BaseRestClient {

    private static final long serialVersionUID = -3582864276979370967L;

    public long count(final String keyword) {
        return getService(CommandService.class).
                search(new CommandQuery.Builder().page(1).size(0).keyword(keyword).build()).
                getTotalCount();
    }

    public List<CommandTO> search(final int page, final int size, final String keyword) {
        return getService(CommandService.class).
                search(new CommandQuery.Builder().page(page).size(size).keyword(keyword).build()).
                getResult();
    }

    public CommandTO read(final String key) {
        return getService(CommandService.class).read(key);
    }

    public CommandOutput run(final CommandTO command) {
        return getService(CommandService.class).run(command);
    }
}
