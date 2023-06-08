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
package org.apache.syncope.client.enduser.rest;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.rest.api.service.AnyTypeService;

public class AnyTypeRestClient extends BaseRestClient {

    private static final long serialVersionUID = -2211371717449597247L;

    private static final Comparator<AnyTypeTO> COMPARATOR = new AnyTypeComparator();

    public static final Comparator<String> KEY_COMPARATOR = new AnyTypeKeyComparator();

    private static class AnyTypeComparator implements Comparator<AnyTypeTO>, Serializable {

        private static final long serialVersionUID = -8227715253094467138L;

        @Override
        public int compare(final AnyTypeTO o1, final AnyTypeTO o2) {
            if (o1.getKind() == AnyTypeKind.USER) {
                return -1;
            }
            if (o2.getKind() == AnyTypeKind.USER) {
                return 1;
            }
            if (o1.getKind() == AnyTypeKind.GROUP) {
                return -1;
            }
            if (o2.getKind() == AnyTypeKind.GROUP) {
                return 1;
            }
            return ObjectUtils.compare(o1.getKey(), o2.getKey());
        }
    }

    private static class AnyTypeKeyComparator implements Comparator<String>, Serializable {

        private static final long serialVersionUID = -7778622183107320760L;

        @Override
        public int compare(final String o1, final String o2) {
            if (SyncopeConstants.REALM_ANYTYPE.equals(o1)) {
                return -1;
            }
            if (SyncopeConstants.REALM_ANYTYPE.equals(o2)) {
                return 1;
            }
            if (AnyTypeKind.USER.name().equals(o1)) {
                return -1;
            }
            if (AnyTypeKind.USER.name().equals(o2)) {
                return 1;
            }
            if (AnyTypeKind.GROUP.name().equals(o1)) {
                return -1;
            }
            if (AnyTypeKind.GROUP.name().equals(2)) {
                return 1;
            }
            return ObjectUtils.compare(o1, o2);
        }
    }

    public AnyTypeTO read(final String key) {
        AnyTypeTO type = null;

        try {
            type = getService(AnyTypeService.class).read(key);
        } catch (SyncopeClientException e) {
            LOG.error("While reading all any types", e);
        }

        return type;
    }

    public List<AnyTypeTO> listAnyTypes() {
        List<AnyTypeTO> types = List.of();

        try {
            types = getService(AnyTypeService.class).list();
            types.sort(COMPARATOR);
        } catch (SyncopeClientException e) {
            LOG.error("While reading all any types", e);
        }

        return types;
    }

    public List<String> list() {
        List<String> types = SyncopeEnduserSession.get().getAnonymousClient().platform().getAnyTypes();
        types.sort(KEY_COMPARATOR);
        return types;
    }

    public void create(final AnyTypeTO anyTypeTO) {
        getService(AnyTypeService.class).create(anyTypeTO);
    }

    public void update(final AnyTypeTO anyTypeTO) {
        getService(AnyTypeService.class).update(anyTypeTO);
    }

    public void delete(final String key) {
        getService(AnyTypeService.class).delete(key);
    }
}
