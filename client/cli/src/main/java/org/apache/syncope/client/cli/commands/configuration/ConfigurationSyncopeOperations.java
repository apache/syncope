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
package org.apache.syncope.client.cli.commands.configuration;

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.rest.api.service.ConfigurationService;

public class ConfigurationSyncopeOperations {

    private final ConfigurationService configurationService = SyncopeServices.get(ConfigurationService.class);

    public AttrTO get(final String schema) {
        return configurationService.get(schema);
    }

    public void set(final AttrTO attrTO) {
        configurationService.set(attrTO);
    }

    public List<AttrTO> list() {
        return configurationService.list();
    }

    public Response export() {
        return configurationService.export();
    }

    public void delete(final String schema) {
        configurationService.delete(schema);
    }
}
