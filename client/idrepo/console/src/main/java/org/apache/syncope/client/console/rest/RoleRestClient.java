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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.RoleService;

/**
 * Console client for invoking Rest Role's services.
 */
public class RoleRestClient extends BaseRestClient {

    private static final long serialVersionUID = -3161863874876938094L;

    public void delete(final String key) {
        getService(RoleService.class).delete(key);
    }

    public RoleTO read(final String key) {
        return getService(RoleService.class).read(key);
    }

    public void update(final RoleTO roleTO) {
        getService(RoleService.class).update(roleTO);
    }

    public void create(final RoleTO roleTO) {
        getService(RoleService.class).create(roleTO);
    }

    public List<RoleTO> list() {
        return getService(RoleService.class).list();
    }

    public String readAnyLayout(final String role) {
        try {
            return IOUtils.toString(InputStream.class.cast(
                    getService(RoleService.class).getAnyLayout(role).getEntity()),
                    StandardCharsets.UTF_8);
        } catch (SyncopeClientException e) {
            if (e.getType() == ClientExceptionType.NotFound) {
                LOG.warn("Could not locate console layout info for role {}", role);
            } else {
                LOG.error("While retrieving console layout info for role {}", role, e);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return StringUtils.EMPTY;
    }

    public void setAnyLayout(final String roleKey, final String content) {
        getService(RoleService.class).setAnyLayout(
                roleKey, IOUtils.toInputStream(content, StandardCharsets.UTF_8));
    }

    public void removeAnyLayout(final String roleKey) {
        getService(RoleService.class).removeAnyLayout(roleKey);
    }

    public List<String> getAllAvailableEntitlements() {
        return SyncopeConsoleSession.get().getAnonymousClient().platform().getEntitlements().
                stream().sorted().collect(Collectors.toList());
    }
}
