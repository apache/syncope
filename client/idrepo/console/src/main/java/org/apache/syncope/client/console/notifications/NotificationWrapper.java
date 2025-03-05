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
package org.apache.syncope.client.console.notifications;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.wizards.any.EntityWrapper;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.NotificationTO;

public class NotificationWrapper extends EntityWrapper<NotificationTO> {

    private static final long serialVersionUID = 8058288034211558376L;

    private List<Pair<String, List<SearchClause>>> aboutClauses;

    private List<SearchClause> recipientClauses;

    public NotificationWrapper(final NotificationTO notificationTO) {
        super(notificationTO);
    }

    public final String getKey() {
        return getInnerObject().getKey();
    }

    public List<Pair<String, List<SearchClause>>> getAboutClauses() {
        if (this.aboutClauses == null) {
            this.aboutClauses = SearchUtils.getSearchClauses(getInnerObject().getAbouts()).entrySet().stream().
                    map(entry -> Pair.of(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        }

        return this.aboutClauses;
    }

    public void setAboutClauses(final List<Pair<String, List<SearchClause>>> dynClauses) {
        this.aboutClauses = dynClauses;
    }

    public List<SearchClause> getRecipientClauses() {
        if (this.recipientClauses == null) {
            this.recipientClauses = SearchUtils.getSearchClauses(getInnerObject().getRecipientsFIQL());
        }
        return this.recipientClauses;
    }

    public void setRecipientClauses(final List<SearchClause> dynClauses) {
        this.recipientClauses = dynClauses;
    }

    public Map<String, String> getAboutFIQLs() {
        if (CollectionUtils.isEmpty(this.aboutClauses) || this.aboutClauses.getFirst().getValue().isEmpty()) {
            return getInnerObject().getAbouts();
        } else {
            Map<String, String> res = new HashMap<>();
            for (Pair<String, List<SearchClause>> pair : this.aboutClauses) {
                AbstractFiqlSearchConditionBuilder<?, ?, ?> builder;
                switch (pair.getLeft()) {
                    case "USER":
                        builder = SyncopeClient.getUserSearchConditionBuilder();
                        break;

                    case "GROUP":
                        builder = SyncopeClient.getGroupSearchConditionBuilder();
                        break;

                    default:
                        builder = SyncopeClient.getAnyObjectSearchConditionBuilder(pair.getLeft());
                }
                res.put(pair.getLeft(), SearchUtils.buildFIQL(pair.getRight(), builder));
            }
            return res;
        }
    }

    private String getRecipientsFIQL() {
        if (CollectionUtils.isEmpty(this.recipientClauses)) {
            return null;
        } else {
            return SearchUtils.buildFIQL(this.recipientClauses, SyncopeClient.getUserSearchConditionBuilder());
        }
    }

    public NotificationTO fillAboutConditions() {
        getInnerObject().getAbouts().clear();
        getInnerObject().getAbouts().putAll(this.getAboutFIQLs());
        return getInnerObject();
    }

    public NotificationTO fillRecipientConditions() {
        getInnerObject().setRecipientsFIQL(this.getRecipientsFIQL());
        return getInnerObject();
    }
}
