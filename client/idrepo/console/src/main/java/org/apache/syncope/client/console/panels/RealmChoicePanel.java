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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.commons.RealmsUtils;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.wicket.markup.html.WebMarkupContainerNoVeil;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.DynRealmTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.rest.api.beans.RealmQuery;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteRenderer;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.IAutoCompleteRenderer;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.Response;

public class RealmChoicePanel extends Panel {

    private static final long serialVersionUID = -1100228004207271270L;

    protected static final String SEARCH_REALMS = "searchRealms";

    protected final RealmRestClient realmRestClient;

    protected final PageReference pageRef;

    protected final LoadableDetachableModel<List<Pair<String, RealmTO>>> realmTree;

    protected final LoadableDetachableModel<List<DynRealmTO>> dynRealmTree;

    protected final WebMarkupContainerNoVeil container;

    protected Model<RealmTO> model;

    protected final Map<String, Pair<RealmTO, List<RealmTO>>> tree;

    protected final List<AbstractLink> links = new ArrayList<>();

    protected String searchQuery;

    protected List<RealmTO> realmsChoices;

    protected final boolean fullRealmsTree;

    protected final ListView<String> breadcrumb;

    public RealmChoicePanel(
            final String id,
            final String base,
            final RealmRestClient realmRestClient,
            final PageReference pageRef) {

        super(id);
        this.realmRestClient = realmRestClient;
        this.pageRef = pageRef;

        tree = new HashMap<>();
        fullRealmsTree = SyncopeWebApplication.get().fullRealmsTree(realmRestClient);

        realmTree = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = -7688359318035249200L;

            @Override
            protected List<Pair<String, RealmTO>> load() {
                Map<String, Pair<RealmTO, List<RealmTO>>> map = reloadRealmParentMap();
                Stream<Pair<String, RealmTO>> full;
                if (fullRealmsTree) {
                    full = map.values().stream().
                            map(realmTOListPair ->
                                Pair.of(realmTOListPair.getLeft().getFullPath(), realmTOListPair.getKey())).
                            sorted(Comparator.comparing(Pair::getLeft));
                } else {
                    full = map.entrySet().stream().
                            map(el -> Pair.of(el.getKey(), el.getValue().getLeft()));
                }
                return full.filter(realm -> SyncopeConsoleSession.get().getSearchableRealms().stream().anyMatch(
                        r -> realm.getValue().getFullPath().startsWith(r))).
                        collect(Collectors.toList());
            }
        };

        dynRealmTree = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<DynRealmTO> load() {
                List<DynRealmTO> dynRealms = realmRestClient.listDynRealms();
                dynRealms.sort((left, right) -> {
                    if (left == null) {
                        return -1;
                    }
                    if (right == null) {
                        return 1;
                    }
                    return left.getKey().compareTo(right.getKey());
                });
                return dynRealms.stream().filter(dynRealm -> SyncopeConsoleSession.get().getSearchableRealms().stream().
                        anyMatch(availableRealm -> SyncopeConstants.ROOT_REALM.equals(availableRealm)
                        || dynRealm.getKey().equals(availableRealm))).collect(Collectors.toList());
            }
        };

        RealmTO realm = SyncopeConsoleSession.get().getRootRealm(base).map(rootRealm -> {
            String rootRealmName = StringUtils.substringAfterLast(rootRealm, "/");

            List<RealmTO> realmTOs = realmRestClient.search(
                    RealmsUtils.buildKeywordQuery(SyncopeConstants.ROOT_REALM.equals(rootRealm)
                            ? SyncopeConstants.ROOT_REALM : rootRealmName)).getResult();

            return realmTOs.stream().
                    filter(r -> rootRealm.equals(r.getFullPath())).findFirst().
                    orElseGet(() -> {
                        RealmTO placeholder = new RealmTO();
                        placeholder.setName(rootRealmName);
                        placeholder.setFullPath(rootRealm);
                        return placeholder;
                    });
        }).orElseGet(() -> {
            RealmTO root = new RealmTO();
            root.setName(SyncopeConstants.ROOT_REALM);
            root.setFullPath(SyncopeConstants.ROOT_REALM);
            return root;
        });

        model = Model.of(realm);
        searchQuery = realm.getName();

        container = new WebMarkupContainerNoVeil("container", realmTree);
        add(container.setOutputMarkupId(true));

        breadcrumb = new ListView<String>("breadcrumb") {

            private static final long serialVersionUID = -8746795666847966508L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                AjaxLink<Void> bcitem = new AjaxLink<>("bcitem") {

                    private static final long serialVersionUID = -817438685948164787L;

                    @Override
                    protected void onInitialize() {
                        super.onInitialize();
                        String fullPath = RealmsUtils.getFullPath(item.getModelObject());
                        if (!SyncopeConstants.ROOT_REALM.equals(fullPath) && fullPath.lastIndexOf("/") == 0) {
                            item.add(new AttributeModifier("class", "breadcrumb-item no-separator"));
                        }
                    }

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        realmRestClient.search(
                                new RealmQuery.Builder().base(RealmsUtils.getFullPath(item.getModelObject())).build()).
                                getResult().stream().findFirst().ifPresent(t -> chooseRealm(t, target));
                    }
                };
                bcitem.setBody(Model.of(SyncopeConstants.ROOT_REALM.equals(item.getModelObject())
                        ? SyncopeConstants.ROOT_REALM
                        : StringUtils.substringAfterLast(RealmsUtils.getFullPath(item.getModelObject()), "/")));
                bcitem.setEnabled(!model.getObject().getFullPath().equals(item.getModelObject()));
                item.add(bcitem);
            }
        };
        container.addOrReplace(breadcrumb.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true));
        setBreadcrumb(model.getObject());

        reloadRealmsTree();
    }

    protected void setBreadcrumb(final RealmTO realm) {
        if (SyncopeConstants.ROOT_REALM.equals(realm.getFullPath())) {
            breadcrumb.setList(List.of(realm.getFullPath()));
        } else {
            Set<String> bcitems = new HashSet<>();
            bcitems.add(SyncopeConstants.ROOT_REALM);

            String[] split = realm.getFullPath().split("/");
            for (int i = 1; i < split.length; i++) {
                StringBuilder bcitem = new StringBuilder();
                for (int j = 1; j <= i; j++) {
                    bcitem.append('/').append(split[j]);
                }
                bcitems.add(bcitem.toString());
            }

            breadcrumb.setList(bcitems.stream().sorted().collect(Collectors.toList()));
        }
    }

    protected void chooseRealm(final RealmTO realm, final AjaxRequestTarget target) {
        model.setObject(realm);
        setBreadcrumb(realm);
        target.add(container);
        send(pageRef.getPage(), Broadcast.EXACT, new ChosenRealm<>(realm, target));
    }

    public void reloadRealmsTree() {
        if (fullRealmsTree) {
            DropDownButton realms = new DropDownButton(
                    "realms", new ResourceModel("select", ""), new Model<>(FontAwesome5IconType.folder_open_r)) {

                private static final long serialVersionUID = -5560086780455361131L;

                @Override
                protected List<AbstractLink> newSubMenuButtons(final String buttonMarkupId) {
                    buildRealmLinks();
                    return RealmChoicePanel.this.links;
                }
            };
            realms.setOutputMarkupId(true);
            realms.setAlignment(DropDownAlignmentBehavior.Alignment.RIGHT);
            realms.setType(Buttons.Type.Menu);

            MetaDataRoleAuthorizationStrategy.authorize(realms, ENABLE, IdRepoEntitlement.REALM_SEARCH);
            Fragment fragment = new Fragment("realmsFragment", "realmsListFragment", container);
            fragment.addOrReplace(realms);
            container.addOrReplace(fragment);
        } else {
            realmsChoices = buildRealmChoices();
            AutoCompleteSettings settings = new AutoCompleteSettings();
            settings.setShowCompleteListOnFocusGain(false);
            settings.setShowListOnEmptyInput(false);

            AutoCompleteTextField<String> searchRealms =
                    new AutoCompleteTextField<>(SEARCH_REALMS, new Model<>(), settings) {

                private static final long serialVersionUID = -6635259975264955783L;

                @Override
                protected Iterator<String> getChoices(final String input) {
                    searchQuery = input;
                    realmsChoices = RealmsUtils.checkInput(input)
                            ? buildRealmChoices()
                            : List.of();
                    return realmsChoices.stream().map(RealmTO::getFullPath).sorted().iterator();
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
                    realmsChoices.stream().
                            filter(item -> item.getFullPath().equals(searchRealms.getModelObject())).
                            findFirst().ifPresent(realm -> chooseRealm(realm, target));
                }
            });

            Fragment fragment = new Fragment("realmsFragment", "realmsSearchFragment", container);
            fragment.addOrReplace(searchRealms);
            container.addOrReplace(fragment);
        }
    }

    protected void buildRealmLinks() {
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
                    chooseRealm(link.getRight(), target);
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
                RealmTO realm = new RealmTO();
                realm.setKey(dynRealmTO.getKey());
                realm.setName(dynRealmTO.getKey());
                realm.setFullPath(dynRealmTO.getKey());

                RealmChoicePanel.this.links.add(new BootstrapAjaxLink<>(
                        ButtonList.getButtonMarkupId(),
                        new Model<>(),
                        Buttons.Type.Link,
                        new Model<>(realm.getKey())) {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        chooseRealm(realm, target);
                    }
                });
            });
        }
    }

    protected List<RealmTO> buildRealmChoices() {
        return Stream.of(
                realmTree.getObject().stream().map(Pair::getValue).collect(Collectors.toList()),
                dynRealmTree.getObject().stream().map(item -> {
                    RealmTO realm = new RealmTO();
                    realm.setKey(item.getKey());
                    realm.setName(item.getKey());
                    realm.setFullPath(item.getKey());
                    return realm;
                }).collect(Collectors.toList())).flatMap(Collection::stream).
                collect(Collectors.toList());
    }

    public final RealmChoicePanel reloadRealmTree(final AjaxRequestTarget target) {
        reloadRealmsTree();
        chooseRealm(model.getObject(), target);
        target.add(container);
        return this;
    }

    public final RealmChoicePanel reloadRealmTree(final AjaxRequestTarget target, final Model<RealmTO> newModel) {
        model = newModel;
        reloadRealmTree(target);
        return this;
    }

    protected Map<String, Pair<RealmTO, List<RealmTO>>> reloadRealmParentMap() {
        List<RealmTO> realmsToList = realmRestClient.search(fullRealmsTree
                ? RealmsUtils.buildBaseQuery()
                : RealmsUtils.buildKeywordQuery(searchQuery)).getResult();

        return reloadRealmParentMap(realmsToList.stream().
                sorted(Comparator.comparing(RealmTO::getName)).
                collect(Collectors.toList()));
    }

    protected Map<String, Pair<RealmTO, List<RealmTO>>> reloadRealmParentMap(final List<RealmTO> realms) {
        tree.clear();

        Map<String, List<RealmTO>> cache = new HashMap<>();

        realms.forEach(realm -> {
            List<RealmTO> children = new ArrayList<>();
            tree.put(realm.getKey(), Pair.of(realm, children));

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

        protected final AjaxRequestTarget target;

        protected final T obj;

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
