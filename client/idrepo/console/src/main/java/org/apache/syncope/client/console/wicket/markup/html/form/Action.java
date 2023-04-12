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
package org.apache.syncope.client.console.wicket.markup.html.form;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.Model;

/**
 * Action link basic details.
 *
 * @param <T> model object type.
 */
public final class Action<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = -7989237020377623993L;

    private final List<String> realms = new ArrayList<>();

    private final ActionLink<T> link;

    private final ActionLink.ActionType type;

    private String entitlements;

    private boolean onConfirm;

    private boolean visibleLabel;

    private Model<String> label;

    private Model<String> title;

    private Model<String> alt;

    private Model<String> icon;

    private boolean indicator;

    public Action(final ActionLink<T> link, final ActionLink.ActionType type) {
        this.link = link;
        this.type = type;
        this.entitlements = StringUtils.EMPTY;
        this.onConfirm = false;
        this.visibleLabel = true;
        this.label = null;
        this.title = null;
        this.alt = null;
        this.icon = null;
        this.indicator = true;
    }

    public String[] getRealms() {
        return realms.toArray(String[]::new);
    }

    public void setRealm(final String realm) {
        this.realms.clear();

        if (realm != null) {
            this.realms.add(realm);
        }
    }

    public void setRealms(final String realm, final List<String> dynRealms) {
        setRealm(realm);

        if (dynRealms != null) {
            this.realms.addAll(dynRealms);
        }
    }

    public ActionLink<T> getLink() {
        return link;
    }

    public ActionLink.ActionType getType() {
        return type;
    }

    public String getEntitlements() {
        return entitlements;
    }

    public boolean isOnConfirm() {
        return onConfirm;
    }

    public Action<T> setEntitlements(final String entitlements) {
        this.entitlements = entitlements;
        return this;
    }

    public Action<T> setOnConfirm(final boolean onConfirm) {
        this.onConfirm = onConfirm;
        return this;
    }

    public Action<T> hideLabel() {
        this.visibleLabel = false;
        return this;
    }

    public Action<T> showLabel() {
        this.visibleLabel = true;
        return this;
    }

    public boolean isVisibleLabel() {
        return visibleLabel;
    }

    /**
     * Override default action label.
     *
     * @param label new action label;
     * @return updated action.
     */
    public Action<T> setLabel(final Model<String> label) {
        this.label = label;
        return this;
    }

    public Model<String> getLabel() {
        return label;
    }

    /**
     * Override default action title.
     *
     * @param title new action title;
     * @return updated action.
     */
    public Action<T> setTitleI(final Model<String> title) {
        this.title = title;
        return this;
    }

    public Model<String> getTitle() {
        return title;
    }

    /**
     * Override default action icon text name.
     *
     * @param alt action icon text name;
     * @return updated action.
     */
    public Action<T> setAlt(final Model<String> alt) {
        this.alt = alt;
        return this;
    }

    public Model<String> getAlt() {
        return alt;
    }

    /**
     * Override default action css class.
     *
     * @param icon new action class;
     * @return updated action.
     */
    public Action<T> setIcon(final Model<String> icon) {
        this.icon = icon;
        return this;
    }

    public Model<String> getIcon() {
        return icon;
    }

    /**
     * Override disable AJAX indicator.
     *
     * @return updated action.
     */
    public Action<T> disableIndicator() {
        this.indicator = false;
        return this;
    }

    public boolean hasIndicator() {
        return indicator;
    }
}
