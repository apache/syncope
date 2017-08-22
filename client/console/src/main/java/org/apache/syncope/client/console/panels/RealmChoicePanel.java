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

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.AlignmentBehavior;
import de.agilecoders.wicket.core.markup.html.bootstrap.button.BootstrapAjaxLink;
import de.agilecoders.wicket.core.markup.html.bootstrap.button.ButtonList;
import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import de.agilecoders.wicket.core.markup.html.bootstrap.button.dropdown.DropDownButton;
import de.agilecoders.wicket.core.markup.html.bootstrap.image.GlyphIconType;
import de.agilecoders.wicket.core.markup.html.bootstrap.image.IconType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.DynRealmTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public class RealmChoicePanel extends Panel {

    private static final long serialVersionUID = -1100228004207271270L;

    private final RealmRestClient realmRestClient = new RealmRestClient();

    private final PageReference pageRef;

    private final LoadableDetachableModel<List<Pair<String, RealmTO>>> realmTree;

    private final LoadableDetachableModel<List<DynRealmTO>> dynRealmTree;

    private final WebMarkupContainer container;

    private Model<RealmTO> model;

    private final Collection<String> availableRealms;

    private final Map<String, Pair<RealmTO, List<RealmTO>>> tree;

    private final List<AbstractLink> links = new ArrayList<>();

    public RealmChoicePanel(final String id, final PageReference pageRef) {
        super(id);
        this.pageRef = pageRef;
        tree = new HashMap<>();

        RealmTO fakeRootRealm = new RealmTO();
        fakeRootRealm.setName(SyncopeConstants.ROOT_REALM);
        fakeRootRealm.setFullPath(SyncopeConstants.ROOT_REALM);
        model = Model.of(fakeRootRealm);

        realmTree = new LoadableDetachableModel<List<Pair<String, RealmTO>>>() {

            private static final long serialVersionUID = -7688359318035249200L;

            private void getChildren(
                    final List<Pair<String, RealmTO>> full,
                    final String key,
                    final Map<String, Pair<RealmTO, List<RealmTO>>> tree,
                    final String indent) {

                if (tree.containsKey(key)) {
                    Pair<RealmTO, List<RealmTO>> subtree = tree.get(key);
                    subtree.getValue().forEach(child -> {
                        full.add(Pair.of(indent + child.getName(), child));
                        getChildren(full, child.getKey(), tree, "     " + indent + (indent.isEmpty() ? "|--- " : ""));
                    });
                }
            }

            @Override
            protected List<Pair<String, RealmTO>> load() {
                Map<String, Pair<RealmTO, List<RealmTO>>> map = reloadRealmParentMap();
                model.setObject(map.get(null).getKey());

                final List<Pair<String, RealmTO>> full = new ArrayList<>();
                getChildren(full, null, map, StringUtils.EMPTY);
                return full;
            }
        };

        dynRealmTree = new LoadableDetachableModel<List<DynRealmTO>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<DynRealmTO> load() {
                List<DynRealmTO> dynRealms = realmRestClient.listDynReams();
                dynRealms.sort((left, right) -> {
                    if (left == null) {
                        return -1;
                    } else if (right == null) {
                        return 1;
                    } else {
                        return left.getKey().compareTo(right.getKey());
                    }
                });

                return dynRealms;
            }
        };

        container = new WebMarkupContainer("container", realmTree);
        container.setOutputMarkupId(true);
        add(container);

        availableRealms = SyncopeConsoleSession.get().getAuthRealms();

        reloadRealmTree();
    }

    public final void reloadRealmTree() {
        final Label realmLabel = new Label("realmLabel", new Model<>());
        realmLabel.setOutputMarkupId(true);

        container.addOrReplace(realmLabel);

        if (model.getObject().getFullPath().startsWith(SyncopeConstants.ROOT_REALM)) {
            realmLabel.setDefaultModel(new ResourceModel("realmLabel", "Realm"));
        } else {
            realmLabel.setDefaultModel(new ResourceModel("dynRealmLabel", "Dynamic Realm"));
        }

        final Label label = new Label("realm", model.getObject().getFullPath());
        label.setOutputMarkupId(true);
        container.addOrReplace(label);

        final DropDownButton realms = new DropDownButton(
                "realms", new ResourceModel("select", ""), new Model<IconType>(GlyphIconType.folderopen)) {

            private static final long serialVersionUID = -5560086780455361131L;

            @Override
            protected List<AbstractLink> newSubMenuButtons(final String buttonMarkupId) {
                RealmChoicePanel.this.links.clear();

                RealmChoicePanel.this.links.add(new BootstrapAjaxLink<RealmTO>(
                        ButtonList.getButtonMarkupId(),
                        new Model<RealmTO>(),
                        Buttons.Type.Link,
                        new ResourceModel("realms", "Realms")) {

                    private static final long serialVersionUID = -7978723352517770744L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                    }

                    @Override
                    public boolean isEnabled() {
                        return false;
                    }

                    @Override
                    protected void onComponentTag(final ComponentTag tag) {
                        tag.put("class", "panel box box-primary box-header with-border");
                        tag.put("style", "margin: 20px 5px 0px 5px; width: 90%");
                    }
                });

                for (Pair<String, RealmTO> link : realmTree.getObject()) {
                    final RealmTO realmTO = link.getValue();
                    RealmChoicePanel.this.links.add(new BootstrapAjaxLink<RealmTO>(
                            ButtonList.getButtonMarkupId(),
                            Model.of(realmTO),
                            Buttons.Type.Link,
                            new Model<>(link.getKey())) {

                        private static final long serialVersionUID = -7978723352517770644L;

                        @Override
                        public void onClick(final AjaxRequestTarget target) {
                            model.setObject(realmTO);
                            label.setDefaultModelObject(model.getObject().getFullPath());
                            realmLabel.setDefaultModel(new ResourceModel("realmLabel", "Realm"));
                            target.add(label);
                            send(pageRef.getPage(), Broadcast.EXACT, new ChosenRealm<>(realmTO, target));
                        }

                        @Override
                        public boolean isEnabled() {
                            return availableRealms.stream().
                                    anyMatch(availableRealm -> realmTO.getFullPath().startsWith(availableRealm));
                        }
                    });
                }

                if (!dynRealmTree.getObject().isEmpty()) {
                    RealmChoicePanel.this.links.add(new BootstrapAjaxLink<RealmTO>(
                            ButtonList.getButtonMarkupId(),
                            new Model<RealmTO>(),
                            Buttons.Type.Link,
                            new ResourceModel("dynrealms", "Dynamic Realms")) {

                        private static final long serialVersionUID = -7978723352517770744L;

                        @Override
                        public void onClick(final AjaxRequestTarget target) {

                        }

                        @Override
                        public boolean isEnabled() {
                            return false;
                        }

                        @Override
                        protected void onComponentTag(final ComponentTag tag) {
                            tag.put("class", "panel box box-primary box-header with-border");
                            tag.put("style", "margin: 20px 5px 0px 5px; width: 90%");
                        }
                    });

                    for (DynRealmTO dynRealmTO : dynRealmTree.getObject()) {
                        final RealmTO realmTO = new RealmTO();
                        realmTO.setKey(dynRealmTO.getKey());
                        realmTO.setName(dynRealmTO.getKey());
                        realmTO.setFullPath(dynRealmTO.getKey());

                        RealmChoicePanel.this.links.add(new BootstrapAjaxLink<RealmTO>(
                                ButtonList.getButtonMarkupId(),
                                new Model<RealmTO>(),
                                Buttons.Type.Link,
                                new Model<>(realmTO.getKey())) {

                            private static final long serialVersionUID = -7978723352517770644L;

                            @Override
                            public void onClick(final AjaxRequestTarget target) {
                                model.setObject(realmTO);
                                label.setDefaultModelObject(realmTO.getKey());
                                realmLabel.setDefaultModel(new ResourceModel("dynRealmLabel", "Dynamic Realm"));
                                target.add(label);
                                send(pageRef.getPage(), Broadcast.EXACT, new ChosenRealm<>(realmTO, target));
                            }

                            @Override
                            public boolean isEnabled() {
                                return availableRealms.stream().anyMatch(availableRealm -> {
                                    return SyncopeConstants.ROOT_REALM.equals(availableRealm)
                                            || realmTO.getKey().equals(availableRealm);
                                });
                            }
                        });
                    }
                }

                return RealmChoicePanel.this.links;
            }
        };
        realms.setOutputMarkupId(true);
        realms.setAlignment(AlignmentBehavior.Alignment.RIGHT);
        realms.setType(Buttons.Type.Menu);

        MetaDataRoleAuthorizationStrategy.authorize(realms, ENABLE, StandardEntitlement.REALM_LIST);

        container.addOrReplace(realms);
    }

    public final RealmChoicePanel reloadRealmTree(final AjaxRequestTarget target) {
        reloadRealmTree();
        target.add(container);
        return this;
    }

    public final RealmChoicePanel reloadRealmTree(final AjaxRequestTarget target, final Model<RealmTO> newModel) {
        model = newModel;
        reloadRealmTree(target);
        return this;
    }

    private Map<String, Pair<RealmTO, List<RealmTO>>> reloadRealmParentMap() {
        final List<RealmTO> realms = realmRestClient.list();
        Collections.sort(realms, new RealmNameComparator());
        return reloadRealmParentMap(realms);
    }

    private Map<String, Pair<RealmTO, List<RealmTO>>> reloadRealmParentMap(final List<RealmTO> realms) {
        tree.clear();
        tree.put(null, Pair.<RealmTO, List<RealmTO>>of(realms.get(0), new ArrayList<>()));

        final Map<String, List<RealmTO>> cache = new HashMap<>();

        realms.forEach(realm -> {
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
        });

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

    /**
     * Gets current selected realm.
     *
     * @return selected realm.
     */
    public RealmTO getCurrentRealm() {
        return model.getObject();
    }

    public void setCurrentRealm(final RealmTO realmTO) {
        model.setObject(realmTO);
    }

    public RealmTO moveToParentRealm(final String key) {
        for (Pair<RealmTO, List<RealmTO>> subtree : tree.values()) {
            for (RealmTO child : subtree.getRight()) {
                if (child.getKey() != null && child.getKey().equals(key)) {
                    model.setObject(subtree.getLeft());
                    return subtree.getLeft();
                }
            }
        }
        return null;
    }

    public static class ChosenRealm<T> {

        private final AjaxRequestTarget target;

        private final T obj;

        public ChosenRealm(final T obj, final AjaxRequestTarget target) {
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

    public List<AbstractLink> getLinks() {
        return links;
    }
}
