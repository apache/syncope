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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
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

    private final LoadableDetachableModel<List<Pair<String, RealmTO>>> ldm;

    private final WebMarkupContainer container;

    private final Model<RealmTO> model;

    private final Map<String, Pair<RealmTO, List<RealmTO>>> tree;

    public RealmChoicePanel(final String id, final PageReference pageRef) {
        super(id);
        this.pageRef = pageRef;
        tree = new HashMap<>();

        RealmTO fakeRootRealm = new RealmTO();
        fakeRootRealm.setName("/");
        fakeRootRealm.setFullPath("/");
        model = Model.of(fakeRootRealm);

        ldm = new LoadableDetachableModel<List<Pair<String, RealmTO>>>() {

            private static final long serialVersionUID = -7688359318035249200L;

            private void getChildren(
                    final List<Pair<String, RealmTO>> full,
                    final String key,
                    final Map<String, Pair<RealmTO, List<RealmTO>>> tree,
                    final String indent) {

                if (tree.containsKey(key)) {
                    Pair<RealmTO, List<RealmTO>> subtree = tree.get(key);
                    for (RealmTO child : subtree.getValue()) {
                        full.add(Pair.of(indent + child.getName(), child));
                        getChildren(full, child.getKey(), tree, "     " + indent + (indent.isEmpty() ? "|--- " : ""));
                    }
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

        container = new WebMarkupContainer("container", ldm);
        container.setOutputMarkupId(true);
        add(container);

        reloadRealmTree();
    }

    public final void reloadRealmTree() {
        final Label label = new Label("realm", model.getObject().getFullPath());
        label.setOutputMarkupId(true);
        container.addOrReplace(label);

        final DropDownButton realms = new DropDownButton(
                "realms", new ResourceModel("select", ""), new Model<IconType>(GlyphIconType.folderopen)) {

            private static final long serialVersionUID = -5560086780455361131L;

            @Override
            protected List<AbstractLink> newSubMenuButtons(final String buttonMarkupId) {
                List<AbstractLink> links = new ArrayList<>();

                for (Pair<String, RealmTO> link : ldm.getObject()) {
                    final RealmTO realmTO = link.getValue();
                    links.add(new BootstrapAjaxLink<String>(
                            ButtonList.getButtonMarkupId(),
                            new Model<String>(),
                            Buttons.Type.Link,
                            new Model<>(link.getKey())) {

                        private static final long serialVersionUID = -7978723352517770644L;

                        @Override
                        public void onClick(final AjaxRequestTarget target) {
                            model.setObject(realmTO);
                            label.setDefaultModelObject(model.getObject().getFullPath());
                            target.add(label);
                            send(pageRef.getPage(), Broadcast.EXACT, new ChosenRealm<>(realmTO, target));
                        }
                    });
                }
                return links;
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

    private Map<String, Pair<RealmTO, List<RealmTO>>> reloadRealmParentMap() {
        final List<RealmTO> realms = realmRestClient.list();
        Collections.sort(realms, new RealmNameComparator());
        return reloadRealmParentMap(realms);
    }

    private Map<String, Pair<RealmTO, List<RealmTO>>> reloadRealmParentMap(final List<RealmTO> realms) {
        tree.clear();
        tree.put(null, Pair.<RealmTO, List<RealmTO>>of(realms.get(0), new ArrayList<RealmTO>()));

        final Map<String, List<RealmTO>> cache = new HashMap<>();

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

    /**
     * Gets current selected realm.
     *
     * @return selected realm.
     */
    public RealmTO getCurrentRealm() {
        return model.getObject();
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
}
