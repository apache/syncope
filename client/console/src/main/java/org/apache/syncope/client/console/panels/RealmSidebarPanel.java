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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.panels.RealmSidebarPanel.ControlSidebarClick;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
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

public class RealmSidebarPanel extends Panel {

    private static final long serialVersionUID = -1100228004207271270L;

    private final RealmRestClient realmRestClient = new RealmRestClient();

    private final WebMarkupContainer menu;

    private List<RealmTO> currentPath;

    private final PageReference pageRef;

    private Map<Long, Pair<RealmTO, List<RealmTO>>> tree;

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
    }

    public final RealmSidebarPanel reloadRealmTree() {
        reloadRealmTree(reloadRealmParentMap(), 0L, menu);
        return this;
    }

    private MarkupContainer reloadRealmTree(
            final Map<Long, Pair<RealmTO, List<RealmTO>>> parentMap, final Long key, final MarkupContainer container) {

        // set the current active path base on the current parent map
        setCurrentRealm(getCurrentRealm());

        final RepeatingView listItems = new RepeatingView("list");
        listItems.setOutputMarkupId(true);
        container.addOrReplace(listItems);

        if (!parentMap.containsKey(key)) {
            return container;
        }

        for (final RealmTO realm : parentMap.get(key).getRight()) {
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

            if (parentMap.containsKey(realm.getKey()) && !parentMap.get(realm.getKey()).getRight().isEmpty()) {
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

                final WebMarkupContainer subtree = new WebMarkupContainer("subtree");
                subtree.setOutputMarkupId(true);
                subtree.setMarkupId("subtree");
                fragment.add(subtree);

                reloadRealmTree(parentMap, realm.getKey(), subtree);
            } else {
                fragment = new Fragment(String.valueOf(realm.getKey()), "withoutChildren", RealmSidebarPanel.this);
            }

            fragment.addOrReplace(link);
            fragment.setOutputMarkupId(true);
            listItems.addOrReplace(fragment);

            MetaDataRoleAuthorizationStrategy.authorize(link, ENABLE, StandardEntitlement.REALM_LIST);
        }

        return container;
    }

    private Map<Long, Pair<RealmTO, List<RealmTO>>> reloadRealmParentMap() {
        final List<RealmTO> realms = realmRestClient.list();
        Collections.sort(realms, new RealmNameComparator());
        return reloadRealmParentMap(realms);
    }

    private Map<Long, Pair<RealmTO, List<RealmTO>>> reloadRealmParentMap(final List<RealmTO> realms) {
        tree = new HashMap<>();
        tree.put(0L, Pair.<RealmTO, List<RealmTO>>of(realms.get(0), new ArrayList<RealmTO>()));

        final Map<Long, List<RealmTO>> cache = new HashMap<>();

        for (RealmTO realm : realms) {
            final List<RealmTO> children = new ArrayList<>();
            tree.put(realm.getKey(), Pair.<RealmTO, List<RealmTO>>of(realm, children));

            if (cache.containsKey(realm.getKey())) {
                children.addAll(cache.get(realm.getKey()));
                cache.remove(realm.getKey());
            }

            if (tree.containsKey(realm.getParent())) {
                tree.get(realm.getParent()).getRight().add(realm);
            } else if (cache.containsKey(realm.getParent())) {
                cache.get(realm.getParent()).add(realm);
            } else {
                cache.put(realm.getParent(), new ArrayList<>(Collections.singleton(realm)));
            }
        }

        return tree;
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

    public final void setCurrentRealm(final RealmTO realmTO) {
        RealmTO realm;

        if (tree.containsKey(realmTO.getKey())) {
            realm = tree.get(realmTO.getKey()).getLeft();
        } else if (tree.containsKey(realmTO.getParent())) {
            realm = tree.get(realmTO.getParent()).getLeft();
        } else {
            realm = tree.get(0L).getLeft();
        }

        this.currentPath = new ArrayList<>();
        this.currentPath.add(realm);

        while (realm.getParent() != 0L) {
            realm = tree.get(realm.getParent()).getLeft();
            this.currentPath.add(realm);
        }
    }

    public RealmTO getCurrentRealm() {
        return this.currentPath == null || this.currentPath.isEmpty()
                ? tree.get(0L).getLeft() : this.currentPath.get(0);
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

            for (RealmTO realm : this.currentPath.subList(1, this.currentPath.size())) {
                response.render(OnLoadHeaderItem.forScript(String.format("$('#angle-%d').click();", realm.getKey())));
            }
        } else {
            reload = true;
        }
    }
}
