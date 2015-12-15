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
package org.apache.syncope.client.console.panels.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.panels.NotificationPanel;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.common.lib.search.SearchableFields;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSearchPanel extends Panel {

    private static final long serialVersionUID = 5922413053568696414L;

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractSearchPanel.class);

    protected SchemaRestClient schemaRestClient = new SchemaRestClient();

    protected ResourceRestClient resourceRestClient = new ResourceRestClient();

//    protected AuthRestClient authRestClient;
    protected IModel<List<String>> dnames;

    protected IModel<List<String>> anames;

    protected IModel<List<String>> resourceNames;

//    protected IModel<List<String>> entitlements;
    protected IModel<List<SearchClause.Type>> types;

    protected IModel<List<Pair<Long, String>>> groupNames;

    protected NotificationPanel searchFeedback;

    protected PropertyModel<List<SearchClause>> model;

    protected WebMarkupContainer searchFormContainer;

    protected AnyTypeKind typeKind;

    protected boolean required;

    public abstract static class Builder<T extends AbstractSearchPanel> implements Serializable {

        private static final long serialVersionUID = 6308997285778809578L;

        protected final PropertyModel<List<SearchClause>> model;

        protected boolean required = true;

        public Builder(final PropertyModel<List<SearchClause>> model) {
            this.model = model;
        }

        public Builder<T> required(final boolean required) {
            this.required = required;
            return this;
        }

        public abstract T build(final String id);
    }

    protected AbstractSearchPanel(
            final String id,
            final PropertyModel<List<SearchClause>> model,
            final AnyTypeKind typeKind) {
        this(id, model, typeKind, true);
    }

    protected AbstractSearchPanel(
            final String id,
            final PropertyModel<List<SearchClause>> model,
            final AnyTypeKind typeKind,
            final boolean required) {

        super(id);
        populate();

        this.typeKind = typeKind;
        this.required = required;

        setOutputMarkupId(true);

        searchFormContainer = new WebMarkupContainer("searchFormContainer");
        searchFormContainer.setOutputMarkupId(true);
        add(searchFormContainer);

        searchFeedback = new NotificationPanel("searchFeedback", new IFeedbackMessageFilter() {

            private static final long serialVersionUID = 6895024863321391672L;

            @Override
            public boolean accept(final FeedbackMessage message) {
                boolean result;

                // messages reported on the session have a null reporter
                if (message.getReporter() == null) {
                    result = false;
                } else {
                    // only accept messages coming from the children of the search form container
                    result = searchFormContainer.contains(message.getReporter(), true);
                }

                return result;
            }
        });
        searchFeedback.setOutputMarkupId(true);
        add(searchFeedback);

        final SearchClausePanel searchClausePanel = new SearchClausePanel("panel", "panel",
                Model.of(new SearchClause()),
                required,
                types, anames, dnames, groupNames, resourceNames);

        final MultiFieldPanel.Builder<SearchClause> searchView = new MultiFieldPanel.Builder<SearchClause>(model) {

            private static final long serialVersionUID = 1L;

            @Override
            protected SearchClause newModelObject() {
                return new SearchClause();
            }
        };

        searchFormContainer.add(searchView.build("search", "search", searchClausePanel).hideLabel());
    }

    protected void populate() {
        dnames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return SearchableFields.get(typeKind);
            }
        };

        anames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return schemaRestClient.getPlainSchemaNames();
            }
        };

        resourceNames = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return CollectionUtils.collect(resourceRestClient.getAll(), new Transformer<ResourceTO, String>() {

                    @Override
                    public String transform(final ResourceTO input) {
                        return input.getKey();
                    }
                }, new ArrayList<String>());
            }
        };
    }

    public NotificationPanel getSearchFeedback() {
        return searchFeedback;
    }
}
