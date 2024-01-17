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
package org.apache.syncope.client.console.wicket.markup.html.bootstrap.buttons;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.ModalCloseButton;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.ResourceModel;

public class DefaultModalCloseButton extends ModalCloseButton implements IAjaxIndicatorAware {

    private static final long serialVersionUID = -1097993976905448580L;

    public DefaultModalCloseButton() {
        super(new ResourceModel("cancel", "Cancel"));
        add(new AttributeAppender("class", " float-start"));
        setOutputMarkupId(true);
    }

    @Override
    public String getAjaxIndicatorMarkupId() {
        return StringUtils.EMPTY;
    }
}
