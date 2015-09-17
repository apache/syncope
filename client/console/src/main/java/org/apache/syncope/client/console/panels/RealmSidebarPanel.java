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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class RealmSidebarPanel extends Panel {

    private static final long serialVersionUID = -1100228004207271270L;

    @SpringBean
    private RealmRestClient realmRestClient;

    private final WebMarkupContainer menu;

    private RealmTO currentRealm;

    private final PageReference pageRef;

    private boolean reload = false;

    public RealmSidebarPanel(final String id, final PageReference pageRef) {
        super(id);
        this.pageRef = pageRef;

        final List<RealmTO> realms = realmRestClient.list();
        Collections.sort(realms, new RealmNameComparator());

        menu = new WebMarkupContainer("menu");

        menu.setOutputMarkupId(true);
        add(menu);

        reloadRealmTree(reloadRealmParentMap(realms), 0L, menu);
        setCurrentRealm(realms.get(0));
    }

    public final RealmSidebarPanel reloadRealmTree() {
        reloadRealmTree(reloadRealmParentMap(), 0L, menu);
        return this;
    }

    private MarkupContainer reloadRealmTree(
            final Map<Long, List<RealmTO>> parentMap, final Long key, final MarkupContainer container) {
        final RepeatingView listItems = new RepeatingView("list");
        listItems.setOutputMarkupId(true);
        container.addOrReplace(listItems);

        for (final RealmTO realm : parentMap.get(key)) {
            final Fragment fragment;

            final AjaxLink<Void> link = new AjaxLink<Void>("link") {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    RealmSidebarPanel.this.setCurrentRealm(realm);
                    send(pageRef.getPage(), Broadcast.EXACT, new ControlSidebarClick<>(realm, target));
                }

                @Override
                protected void onComponentTag(final ComponentTag tag) {
                    super.onComponentTag(tag);
                    tag.put("href", "#");
                }
            };

            link.setMarkupId("item-" + realm.getKey());
            link.addOrReplace(new Label("name", new PropertyModel<String>(realm, "name")));

            if (parentMap.containsKey(realm.getKey()) && !parentMap.get(realm.getKey()).isEmpty()) {
                fragment = new Fragment(String.valueOf(realm.getKey()), "withChildren", RealmSidebarPanel.this);

                final Link<Void> angle = new Link<Void>("angle") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick() {
                    }

                    @Override
                    protected void onComponentTag(final ComponentTag tag) {
                        super.onComponentTag(tag);
                        tag.put("href", "#");
                    }
                };

                angle.setMarkupId("angle-" + realm.getKey());
                fragment.addOrReplace(angle);

                reloadRealmTree(parentMap, realm.getKey(), fragment);
            } else {
                fragment = new Fragment(String.valueOf(realm.getKey()), "withoutChildren", RealmSidebarPanel.this);
            }

            fragment.addOrReplace(link);
            fragment.setOutputMarkupId(true);
            listItems.addOrReplace(fragment);
        }

        return container;
    }

    private Map<Long, List<RealmTO>> reloadRealmParentMap() {
        final List<RealmTO> realms = realmRestClient.list();
        Collections.sort(realms, new RealmNameComparator());
        return reloadRealmParentMap(realms);
    }

    private Map<Long, List<RealmTO>> reloadRealmParentMap(final List<RealmTO> realms) {
        final Map<Long, List<RealmTO>> res = new HashMap<>();
        res.put(0L, new ArrayList<RealmTO>());

        final Map<Long, List<RealmTO>> cache = new HashMap<>();

        for (RealmTO realm : realms) {
            if (res.containsKey(realm.getParent())) {
                res.get(realm.getParent()).add(realm);

                final List<RealmTO> children = new ArrayList<>();
                res.put(realm.getKey(), children);

                if (cache.containsKey(realm.getKey())) {
                    children.addAll(cache.get(realm.getKey()));
                    cache.remove(realm.getKey());
                }
            } else if (cache.containsKey(realm.getParent())) {
                cache.get(realm.getParent()).add(realm);
            } else {
                final List<RealmTO> children = new ArrayList<>();
                children.add(realm);
                cache.put(realm.getParent(), children);
            }
        }

        return res;
    }

    private static class RealmNameComparator implements Comparator<RealmTO>, Serializable {

        private static final long serialVersionUID = 7085057398406518811L;

        @Override
        public int compare(final RealmTO r1, final RealmTO r2) {
            if (r1 == null && r2 == null) {
                return 0;
            } else if (r1 != null && r2 != null) {
                return r1.getName().compareTo(r2.getName());
            } else if (r1 == null && r2 != null) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    private void setCurrentRealm(final RealmTO realmTO) {
        this.currentRealm = realmTO;
    }

    public RealmTO getCurrentRealm() {
        return this.currentRealm;
    }

    public static class ControlSidebarClick<T> {

        private final AjaxRequestTarget target;

        private final T obj;

        public ControlSidebarClick(final T obj, final AjaxRequestTarget target) {
            this.obj = obj;
            this.target = target;
        }

        public T getObj() {
            return obj;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        if (reload) {
            response.render(OnLoadHeaderItem.forScript("$.AdminLTE.tree('.syncopeSidebar');"));
        } else {
            reload = true;
        }
    }
}
