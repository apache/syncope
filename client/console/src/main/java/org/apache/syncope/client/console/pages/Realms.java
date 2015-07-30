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
package org.apache.syncope.client.console.pages;

import static org.apache.wicket.Component.ENABLE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.Realm;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.wicket.ajax.markup.html.ClearIndicatingAjaxLink;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Realms extends BasePage {

    private static final long serialVersionUID = -1100228004207271270L;

    protected static final Logger LOG = LoggerFactory.getLogger(Realms.class);

    @SpringBean
    private RealmRestClient realmRestClient;

    private final WebMarkupContainer content;

    private final ModalWindow editModalWin;

    protected RealmTO currentRealm;

    public Realms(final PageParameters parameters) {
        super(parameters);
        
        final List<RealmTO> realms = realmRestClient.list();
        Collections.sort(realms, new RealmNameComparator());

        add(getParentMap(realms), 0L, Realms.this);

        content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        add(content);

        content.add(new Label("header", "Root realm"));
        content.add(new Label("body", "Root realm"));

        //create new realms                
        final AjaxLink<Void> createLink = new ClearIndicatingAjaxLink<Void>("createLink", getPageReference()) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onClickInternal(final AjaxRequestTarget target) {
                editModalWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        return new RealmModalPage(Realms.this.getPageReference(), editModalWin, new RealmTO(),
                                Realms.this.getCurrentRealm().getFullPath(), Entitlement.REALM_CREATE);
                    }
                });

                editModalWin.show(target);
            }
        };
        if (SyncopeConsoleSession.get().owns(Entitlement.REALM_CREATE)) {
            MetaDataRoleAuthorizationStrategy.authorize(createLink, ENABLE, Entitlement.REALM_CREATE);
        }
        content.add(createLink);

        // Modal window for editing realms
        editModalWin = new ModalWindow("editModal");
        editModalWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        editModalWin.setCookieName("edit-modal");

        content.add(editModalWin);
        setCurrentRealm(realms.get(0));
    }

    private void add(final Map<Long, List<RealmTO>> parentMap, final Long key, final MarkupContainer container) {
        final RepeatingView listItems = new RepeatingView("list");
        container.add(listItems);

        for (final RealmTO realm : parentMap.get(key)) {
            final Fragment fragment;

            final AjaxLink<Void> link = new AjaxLink<Void>("link") {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    Realms.this.setCurrentRealm(realm);
                    send(Realms.this, Broadcast.EXACT, new ControlSidebarClick<>(realm, target));
                }
            };

            link.add(new Label("name", new PropertyModel<String>(realm, "name")));

            if (parentMap.containsKey(realm.getKey()) && !parentMap.get(realm.getKey()).isEmpty()) {
                fragment = new Fragment(String.valueOf(realm.getKey()), "withChildren", Realms.this);
                add(parentMap, realm.getKey(), fragment);
            } else {
                fragment = new Fragment(String.valueOf(realm.getKey()), "withoutChildren", Realms.this);
            }

            fragment.add(link);
            listItems.add(fragment);
        }
    }

    private Map<Long, List<RealmTO>> getParentMap(final List<RealmTO> realms) {
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
            return r1.getName().compareTo(r2.getName());
        }
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof ControlSidebarClick) {
            @SuppressWarnings("unchecked")
            final ControlSidebarClick<RealmTO> controlSidebarClick = ControlSidebarClick.class.cast(event.getPayload());
            content.addOrReplace(new Label("header", controlSidebarClick.getObj().getName()));
            content.addOrReplace(new Realm("body", controlSidebarClick.getObj(), getPageReference()));
            controlSidebarClick.getTarget().add(content);
        }
    }

    public void setCurrentRealm(final RealmTO realmTO) {
        this.currentRealm = realmTO;
        setupEditModalPage(currentRealm);
    }

    public RealmTO getCurrentRealm() {
        return this.currentRealm;
    }

    public void setupEditModalPage(final RealmTO realmTO) {
        final AjaxLink<Void> edit = new ClearIndicatingAjaxLink<Void>("edit", getPageReference()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onClickInternal(final AjaxRequestTarget target) {
                editModalWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        return new EditRealmModalPage(Realms.this.getPageReference(), editModalWin, realmTO,
                                Realms.this.getCurrentRealm().getFullPath(), Entitlement.REALM_UPDATE);
                    }
                });

                editModalWin.show(target);
            }
        };
        content.addOrReplace(edit);
    }

    private static class ControlSidebarClick<T> {

        private final AjaxRequestTarget target;

        private final T obj;

        public ControlSidebarClick(
                final T obj, final AjaxRequestTarget target) {
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
}
