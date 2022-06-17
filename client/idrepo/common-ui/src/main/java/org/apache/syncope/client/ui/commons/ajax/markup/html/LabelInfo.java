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
package org.apache.syncope.client.ui.commons.ajax.markup.html;

import java.util.Collection;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.springframework.util.CollectionUtils;

public class LabelInfo extends Label {

    private static final long serialVersionUID = 4755868673082976208L;

    private final String title;

    public LabelInfo(final String id, final String title) {
        super(id, StringUtils.EMPTY);
        this.title = Optional.ofNullable(title).map(s -> StringUtils.abbreviate(s, 30)).orElse(StringUtils.EMPTY);
    }

    public LabelInfo(final String id, final Collection<String> title) {
        super(id, StringUtils.EMPTY);
        if (CollectionUtils.isEmpty(title)) {
            this.title = StringUtils.EMPTY;
        } else {
            StringBuilder titleBuilder = new StringBuilder();
            for (String el : title) {
                if (titleBuilder.length() > 0) {
                    titleBuilder.append("; ");
                }
                if (StringUtils.isNoneEmpty(el)) {
                    titleBuilder.append(el);
                }
            }
            this.title = StringUtils.abbreviate(titleBuilder.toString(), 50);
        }
    }

    @Override
    protected void onComponentTag(final ComponentTag tag) {
        tag.put("class", "fa  fa-info-circle");
        tag.put("style", "color:red");
        tag.put("title", title);
    }
}
