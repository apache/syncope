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

import de.agilecoders.wicket.core.markup.html.bootstrap.button.BootstrapAjaxLink;
import de.agilecoders.wicket.core.markup.html.bootstrap.button.ButtonList;
import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import de.agilecoders.wicket.core.markup.html.bootstrap.button.dropdown.DropDownAlignmentBehavior;
import de.agilecoders.wicket.core.markup.html.bootstrap.button.dropdown.DropDownButton;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.RealmsUtils;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.wicket.markup.html.WebMarkupContainerNoVeil;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.DynRealmTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteRenderer;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.Response;

public class RealmChoicePanel extends Panel {

    private static final long serialVersionUID = -1100228004207271270L;

    private static final String SEARCH_REALMS = "searchRealms";

    private final PageReference pageRef;

    private final LoadableDetachableModel<List<Pair<String, RealmTO>>> realmTree;

    private final LoadableDetachableModel<List<DynRealmTO>> dynRealmTree;

    private final WebMarkupContainerNoVeil container;

    private Model<RealmTO> model;

    private final Collection<String> availableRealms;

    private final Map<String, Pair<RealmTO, List<RealmTO>>> tree;

    private final List<AbstractLink> links = new ArrayList<>();

    private String searchQuery;

    private List<RealmTO> realmsChoices;

    private final boolean isSearchEnabled;

    public RealmChoicePanel(final String id, final PageReference pageRef) {
        super(id);
        this.pageRef = pageRef;
        availableRealms = SyncopeConsoleSession.get().getSearchableRealms();
        tree = new HashMap<>();
        isSearchEnabled = RealmsUtils.isSearchEnabled(SyncopeConsoleSession.get().getSearchableRealms());

        realmTree = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = -7688359318035249200L;

            @Override
            protected List<Pair<String, RealmTO>> load() {
                Map<String, Pair<RealmTO, List<RealmTO>>> map = reloadRealmParentMap();
                List<Pair<String, RealmTO>> full;
                if (isSearchEnabled) {
                    full = map.entrySet().stream().map(el -> Pair.of(
                        el.getKey(),
                        el.getValue().getLeft())).
                        collect(Collectors.toList());
                } else {
                    full = map.entrySet().stream().
                        map(el -> Pair.of(
                            el.getValue().getLeft().getFullPath(),
                            el.getValue().getKey())).
                        sorted(Comparator.comparing(Pair::getLeft)).
                        collect(Collectors.toList());
                }
                return full.stream().filter(realm -> availableRealms.stream().anyMatch(
                    availableRealm -> realm.getValue().getFullPath().startsWith(availableRealm))).
                    collect(Collectors.toList());
            }
        };

        dynRealmTree = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<DynRealmTO> load() {
                List<DynRealmTO> dynRealms = RealmRestClient.listDynReams();
                dynRealms.sort((left, right) -> {
                    if (left == null) {
                        return -1;
                    } else if (right == null) {
                        return 1;
                    } else {
                        return left.getKey().compareTo(right.getKey());
                    }
                });
                return dynRealms.stream().filter(dynRealm -> availableRealms.stream().
                    anyMatch(availableRealm -> SyncopeConstants.ROOT_REALM.equals(availableRealm)
                        || dynRealm.getKey().equals(availableRealm))).collect(Collectors.toList());
            }
        };

        RealmTO realmTO = SyncopeConsoleSession.get().getRootRealm().map(rootRealm -> {
            String rootRealmName = StringUtils.substringAfterLast(rootRealm, "/");

            List<RealmTO> realmTOs = RealmRestClient.search(
                    RealmsUtils.buildQuery(SyncopeConstants.ROOT_REALM.equals(rootRealm)
                            ? SyncopeConstants.ROOT_REALM : rootRealmName)).getResult();

            return realmTOs.stream().filter(realm -> rootRealm.equals(realm.getFullPath())).findFirst().
                    orElseGet(() -> {
                        RealmTO placeholder = new RealmTO();
                        placeholder.setName(rootRealmName);
                        placeholder.setFullPath(rootRealm);
                        return placeholder;
                    });
        }).orElseGet(RealmTO::new);

        model = Model.of(realmTO);
        searchQuery = realmTO.getName();
        container = new WebMarkupContainerNoVeil("container", realmTree);
        container.setOutputMarkupId(true);
        add(container);
        reloadRealmTree();
    }

    public final void reloadRealmTree() {
        Label realmLabel = new Label("realmLabel", new Model<>());
        realmLabel.setOutputMarkupId(true);

        container.addOrReplace(realmLabel);

        if (StringUtils.startsWith(model.getObject().getFullPath(), SyncopeConstants.ROOT_REALM)) {
            realmLabel.setDefaultModel(new ResourceModel("realmLabel", "Realm"));
        } else {
            realmLabel.setDefaultModel(new ResourceModel("dynRealmLabel", "Dynamic Realm"));
        }

        Label label = new Label("realm", RealmsUtils.getFullPath(model.getObject().getFullPath()));
        label.setOutputMarkupId(true);
        container.addOrReplace(label);

        if (isSearchEnabled) {
            realmsChoices = buildRealmChoices();
            final AutoCompleteSettings settings = new AutoCompleteSettings();
            settings.setShowCompleteListOnFocusGain(false);
            settings.setShowListOnEmptyInput(false);

            final AutoCompleteTextField<String> searchRealms =
                new AutoCompleteTextField<>(SEARCH_REALMS, new Model<>(), settings) {

                    private static final long serialVersionUID = -6635259975264955783L;

                    @Override
                    protected Iterator<String> getChoices(final String input) {
                        searchQuery = input;
                        realmsChoices = RealmsUtils.checkInput(input)
                            ? buildRealmChoices()
                            : List.of();
                        return realmsChoices.stream().
                            map(RealmTO::getFullPath).sorted().collect(Collectors.toList()).iterator();
                    }

                    @Override
                    protected AutoCompleteBehavior<String> newAutoCompleteBehavior(
                        final IAutoCompleteRenderer<String> renderer,
                        final AutoCompleteSettings settings) {
                        return super.newAutoCompleteBehavior(new AbstractAutoCompleteRenderer<>() {

                            private static final long serialVersionUID = -4789925973199139157L;

                            @Override
                            protected void renderChoice(
                                final String object,
                                final Response response,
                                final String criteria) {
                                response.write(object);
                            }

                            @Override
                            protected String getTextValue(final String object) {
                                return object;
                            }
                        }, settings);
                    }
                };

            searchRealms.add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -6139318907146065915L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    realmsChoices.stream().filter(
                            item -> item.getFullPath().equals(searchRealms.getModelObject())).
                            findFirst().ifPresent(realm -> {
                                model.setObject(realm);
                                label.setDefaultModelObject(model.getObject().getFullPath());
                                realmLabel.setDefaultModel(new ResourceModel("realmLabel", "Realm"));
                                target.add(label);
                                send(pageRef.getPage(), Broadcast.EXACT, new ChosenRealm<>(realm, target));
                            });
                }
            });

            Fragment fragment = new Fragment("realmsFragment", "realmsSearchFragment", container);
            fragment.addOrReplace(searchRealms);
            container.addOrReplace(fragment);
        } else {
            DropDownButton realms = new DropDownButton(
                    "realms", new ResourceModel("select", ""), new Model<>(FontAwesome5IconType.folder_open_r)) {

                private static final long serialVersionUID = -5560086780455361131L;

                @Override
                protected List<AbstractLink> newSubMenuButtons(final String buttonMarkupId) {
                    buildRealmLinks(label, realmLabel);
                    return RealmChoicePanel.this.links;
                }
            };
            realms.setOutputMarkupId(true);
            realms.setAlignment(DropDownAlignmentBehavior.Alignment.RIGHT);
            realms.setType(Buttons.Type.Menu);

            MetaDataRoleAuthorizationStrategy.authorize(realms, ENABLE, IdRepoEntitlement.REALM_LIST);
            Fragment fragment = new Fragment("realmsFragment", "realmsListFragment", container);
            fragment.addOrReplace(realms);
            container.addOrReplace(fragment);
        }
    }

    private void buildRealmLinks(final Label label, final Label realmLabel) {
        RealmChoicePanel.this.links.clear();
        RealmChoicePanel.this.links.add(new BootstrapAjaxLink<>(
            ButtonList.getButtonMarkupId(),
            new Model<>(),
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
                tag.put("class", "dropdown-header disabled");
            }
        });

        realmTree.getObject().forEach(link -> {
            RealmChoicePanel.this.links.add(new BootstrapAjaxLink<>(
                ButtonList.getButtonMarkupId(),
                Model.of(link.getRight()),
                Buttons.Type.Link,
                new Model<>(link.getLeft())) {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    model.setObject(link.getRight());
                    label.setDefaultModelObject(model.getObject().getFullPath());
                    realmLabel.setDefaultModel(new ResourceModel("realmLabel", "Realm"));
                    target.add(label);
                    send(pageRef.getPage(), Broadcast.EXACT, new ChosenRealm<>(link.getRight(), target));
                }
            });
        });

        if (!dynRealmTree.getObject().isEmpty()) {
            RealmChoicePanel.this.links.add(new BootstrapAjaxLink<>(
                ButtonList.getButtonMarkupId(),
                new Model<>(),
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
                    tag.put("class", "dropdown-header disabled");
                }
            });

            dynRealmTree.getObject().forEach(dynRealmTO -> {
                final RealmTO realmTO = new RealmTO();
                realmTO.setKey(dynRealmTO.getKey());
                realmTO.setName(dynRealmTO.getKey());
                realmTO.setFullPath(dynRealmTO.getKey());

                RealmChoicePanel.this.links.add(new BootstrapAjaxLink<>(
                    ButtonList.getButtonMarkupId(),
                    new Model<>(),
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
                });
            });
        }
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
        List<RealmTO> realmsToList = isSearchEnabled
                ? RealmRestClient.search(RealmsUtils.buildQuery(searchQuery)).getResult()
                : RealmRestClient.list();

        return reloadRealmParentMap(realmsToList.stream().
                sorted(Comparator.comparing(RealmTO::getName)).
                collect(Collectors.toList()));
    }

    private Map<String, Pair<RealmTO, List<RealmTO>>> reloadRealmParentMap(final List<RealmTO> realms) {
        tree.clear();

        Map<String, List<RealmTO>> cache = new HashMap<>();

        realms.forEach(realm -> {
            List<RealmTO> children = new ArrayList<>();
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
                cache.put(realm.getParent(), Stream.of(realm).collect(Collectors.toList()));
            }
        });
        return tree;
    }

    private List<RealmTO> buildRealmChoices() {
        return Stream.of(
                realmTree.getObject().stream().map(Pair::getValue).collect(Collectors.toList()),
                dynRealmTree.getObject().stream().map(
                        item -> {
                            final RealmTO realmTO = new RealmTO();
                            realmTO.setKey(item.getKey());
                            realmTO.setName(item.getKey());
                            realmTO.setFullPath(item.getKey());
                            return realmTO;
                        }).collect(Collectors.toList())).flatMap(Collection::stream).
                collect(Collectors.toList());
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
