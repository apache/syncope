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
package org.apache.syncope.client.console.panels;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultilevelPanel extends Panel implements IHeaderContributor {

    private static final long serialVersionUID = -4064294905566247729L;

    protected static final Logger LOG = LoggerFactory.getLogger(MultilevelPanel.class);

    public static final String FIRST_LEVEL_ID = "first";

    public static final String SECOND_LEVEL_ID = "second";

    private boolean isFirstLevel = true;

    private final WebMarkupContainer firstLevelContainer;

    private final WebMarkupContainer secondLevelContainer;

    public MultilevelPanel(final String id) {
        super(id);

        firstLevelContainer = new WebMarkupContainer("firstLevelContainer");
        firstLevelContainer.setOutputMarkupPlaceholderTag(true);
        firstLevelContainer.setVisible(true);
        add(firstLevelContainer);

        secondLevelContainer = new WebMarkupContainer("secondLevelContainer");
        secondLevelContainer.setOutputMarkupPlaceholderTag(true);
        secondLevelContainer.setVisible(false);
        add(secondLevelContainer);

        secondLevelContainer.add(new AjaxLink<String>("back") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                onClickBackInternal(target);
                prev(target);
            }
        });
    }

    public final void next(final String title, final SecondLevel secondLevel, final AjaxRequestTarget target) {
        if (isFirstLevel) {
            secondLevelContainer.addOrReplace(new Label("title", new ResourceModel(title, title)));
            secondLevelContainer.addOrReplace(secondLevel);
            secondLevelContainer.setVisible(true);
            target.add(secondLevelContainer);

            firstLevelContainer.setVisible(false);
            target.add(firstLevelContainer);

            isFirstLevel = false;
        } else {
            LOG.warn("No further level available");
        }
    }

    public void prev(final AjaxRequestTarget target) {
        if (isFirstLevel) {
            LOG.warn("No further level available");
        } else {
            firstLevelContainer.setVisible(true);
            target.add(firstLevelContainer);

            secondLevelContainer.setVisible(false);
            target.add(secondLevelContainer);

            isFirstLevel = true;
        }
    }

    protected void onClickBackInternal(final AjaxRequestTarget taget) {
    }

    /**
     * Add panel with id {@code first}
     *
     * @param panel panel to be used into the first level.
     * @return the current MultilevelPanel instance.
     */
    public MultilevelPanel setFirstLevel(final Panel panel) {
        firstLevelContainer.addOrReplace(panel);
        return this;
    }

    public static class SecondLevel extends Panel {

        private static final long serialVersionUID = 5685291231060035528L;

        public SecondLevel() {
            this(SECOND_LEVEL_ID);
        }

        public SecondLevel(final String id) {
            super(id);
        }
    }
}
