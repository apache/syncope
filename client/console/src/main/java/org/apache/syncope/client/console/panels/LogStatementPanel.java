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

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.ui.JQueryUIBehavior;
import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Alert;
import de.agilecoders.wicket.core.util.Attributes;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.syncope.common.lib.log.LogStatementTO;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

public class LogStatementPanel extends Panel {

    private static final long serialVersionUID = 1610867968070669922L;

    private static final FastDateFormat FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private final String labelCssClass;

    public LogStatementPanel(final String id, final LogStatementTO statement) {
        super(id);

        Alert.Type type;
        switch (statement.getLevel()) {
            case DEBUG:
                type = Alert.Type.Success;
                break;

            case INFO:
                type = Alert.Type.Info;
                break;

            case ERROR:
                type = Alert.Type.Danger;
                break;

            case WARN:
                type = Alert.Type.Warning;
                break;

            default:
                type = Alert.Type.Info;
        }
        labelCssClass = "label-" + type.name().toLowerCase();

        add(new Label("logger", Model.of(statement.getLoggerName())));
        add(new Label("instant", Model.of(FORMAT.format(statement.getTimeMillis()))));
        add(new Label("message", Model.of(statement.getMessage())));

        WebMarkupContainer collapse = new WebMarkupContainer("collapse");
        collapse.setOutputMarkupId(true);
        collapse.setOutputMarkupPlaceholderTag(true);
        collapse.setVisible(StringUtils.isNotBlank(statement.getStackTrace()));
        collapse.add(new JQueryUIBehavior(
                "#" + collapse.getMarkupId(), "accordion", new Options("active", false).set("collapsible", true)));
        add(collapse);

        Label stacktrace = new Label("stacktrace", Model.of(statement.getStackTrace()));
        stacktrace.setOutputMarkupId(true);
        collapse.add(stacktrace);
    }

    @Override
    protected void onComponentTag(final ComponentTag tag) {
        super.onComponentTag(tag);

        checkComponentTag(tag, "div");
        Attributes.addClass(tag, labelCssClass);
    }

}
