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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.common.lib.scim.SCIMComplexConf;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMUserAddressConf;
import org.apache.syncope.common.lib.scim.SCIMUserConf;
import org.apache.syncope.common.lib.scim.SCIMUserNameConf;
import org.apache.syncope.common.lib.scim.types.AddressCanonicalType;
import org.apache.syncope.common.lib.scim.types.EmailCanonicalType;
import org.apache.syncope.common.lib.scim.types.IMCanonicalType;
import org.apache.syncope.common.lib.scim.types.PhoneNumberCanonicalType;
import org.apache.syncope.common.lib.scim.types.PhotoCanonicalType;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SCIMConfUserPanel extends SCIMConfTabPanel {

    protected static final Logger LOG = LoggerFactory.getLogger(SCIMConfUserPanel.class);

    private static final long serialVersionUID = 8747864142447220523L;

    private final SCIMUserConf scimUserConf;

    public SCIMConfUserPanel(final String id, final SCIMConf scimConf) {
        super(id);

        if (scimConf.getUserConf() == null) {
            scimConf.setUserConf(new SCIMUserConf());
        }
        if (scimConf.getUserConf().getName() == null) {
            scimConf.getUserConf().setName(new SCIMUserNameConf());
        }
        scimUserConf = scimConf.getUserConf();

        final AutoCompleteSettings settings = new AutoCompleteSettings();
        settings.setShowCompleteListOnFocusGain(true);
        settings.setShowListOnEmptyInput(true);
        settings.setCssClassName("custom-autocomplete-box");

        AjaxTextFieldPanel displayNamePanel = new AjaxTextFieldPanel("displayName", "displayName",
            new PropertyModel<>("displayName", "displayName") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return scimUserConf.getDisplayName();
                }

                @Override
                public void setObject(final String object) {
                    scimUserConf.setDisplayName(object);
                }

            });
        displayNamePanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel localePanel = new AjaxTextFieldPanel("locale", "locale",
            new PropertyModel<>("locale", "locale") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return scimUserConf.getLocale();
                }

                @Override
                public void setObject(final String object) {
                    scimUserConf.setLocale(object);
                }

            });
        localePanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel nickNamePanel = new AjaxTextFieldPanel("nickName", "nickName",
            new PropertyModel<>("nickName", "nickName") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return scimUserConf.getNickName();
                }

                @Override
                public void setObject(final String object) {
                    scimUserConf.setNickName(object);
                }

            });
        nickNamePanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel preferredLanguagePanel = new AjaxTextFieldPanel("preferredLanguage", "preferredLanguage",
            new PropertyModel<>("preferredLanguage", "preferredLanguage") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return scimUserConf.getPreferredLanguage();
                }

                @Override
                public void setObject(final String object) {
                    scimUserConf.setPreferredLanguage(object);
                }

            });
        preferredLanguagePanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel profileUrlPanel = new AjaxTextFieldPanel("profileUrl", "profileUrl",
            new PropertyModel<>("profileUrl", "profileUrl") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return scimUserConf.getProfileUrl();
                }

                @Override
                public void setObject(final String object) {
                    scimUserConf.setProfileUrl(object);
                }

            });
        profileUrlPanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel timezonePanel = new AjaxTextFieldPanel("timezone", "timezone",
            new PropertyModel<>("timezone", "timezone") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return scimUserConf.getTimezone();
                }

                @Override
                public void setObject(final String object) {
                    scimUserConf.setTimezone(object);
                }

            });
        timezonePanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel titlePanel = new AjaxTextFieldPanel("title", "title",
            new PropertyModel<>("title", "title") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return scimUserConf.getTitle();
                }

                @Override
                public void setObject(final String object) {
                    scimUserConf.setTitle(object);
                }

            });
        titlePanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel userTypePanel = new AjaxTextFieldPanel("userType", "userType",
            new PropertyModel<>("userType", "userType") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return scimUserConf.getUserType();
                }

                @Override
                public void setObject(final String object) {
                    scimUserConf.setUserType(object);
                }

            });
        userTypePanel.setChoices(plainSchemaNames);

        // name
        buildNameAccordion();

        // x509certificates
        final AjaxTextFieldPanel x509CertificatesPanel = new AjaxTextFieldPanel("panel", "x509CertificatesPanel",
                new Model<>(null));
        x509CertificatesPanel.setChoices(plainSchemaNames);
        MultiFieldPanel<String> x509CertificatesMultiPanel = new MultiFieldPanel.Builder<>(
                new ListModel<>(scimUserConf.getX509Certificates())).build(
                "x509Certificates",
                "x509Certificates",
                x509CertificatesPanel);

        // addresses
        List<SCIMUserAddressConf> addresses = new ArrayList<>();
        for (AddressCanonicalType canonicalType : AddressCanonicalType.values()) {
            SCIMUserAddressConf address = scimUserConf.getAddresses().stream().
                    filter(addressConf -> addressConf.getType().equals(canonicalType)).
                    findFirst().orElseGet(() -> {
                        SCIMUserAddressConf empty = new SCIMUserAddressConf();
                        empty.setType(canonicalType);
                        return empty;
                    });
            buildAddressAccordion(address, canonicalType);
            addresses.add(address);
        }
        scimUserConf.getAddresses().clear();
        scimUserConf.getAddresses().addAll(addresses);

        // complex objects
        buildComplexPanels(scimUserConf.getEmails(), "emailsAccordion", "emails", EmailCanonicalType.values());
        buildComplexPanels(scimUserConf.getPhoneNumbers(), "phoneNumbersAccordion", "phoneNumbers",
                PhoneNumberCanonicalType.values());
        buildComplexPanels(scimUserConf.getIms(), "imsAccordion", "ims", IMCanonicalType.values());
        buildComplexPanels(scimUserConf.getPhotos(), "photosAccordion", "photos", PhotoCanonicalType.values());

        add(displayNamePanel);
        add(localePanel);
        add(nickNamePanel);
        add(preferredLanguagePanel);
        add(profileUrlPanel);
        add(timezonePanel);
        add(titlePanel);
        add(userTypePanel);

        add(x509CertificatesMultiPanel);

        add(new Label("nameLabel", Model.of("name")));
        add(new Label("addressesLabel", Model.of("addresses")));
        add(new Label("emailsLabel", Model.of("emails")));
        add(new Label("phoneNumbersLabel", Model.of("phoneNumbers")));
        add(new Label("imsLabel", Model.of("ims")));
        add(new Label("photosLabel", Model.of("photos")));

    }

    private <T extends Enum<?>> void buildComplexPanels(
            final List<SCIMComplexConf<T>> complexes,
            final String basePanelId,
            final String baseTabId,
            final T[] canonicalTypes) {

        List<SCIMComplexConf<T>> newElems = new ArrayList<>();
        for (T canonicalType : canonicalTypes) {
            SCIMComplexConf<T> complex = complexes.stream().
                    filter(complexConf -> complexConf.getType().equals(canonicalType)).
                    findFirst().orElseGet(() -> {
                        SCIMComplexConf<T> empty = new SCIMComplexConf<>();
                        empty.setType(canonicalType);
                        return empty;
                    });
            buildComplexAccordion(complex, basePanelId, baseTabId, canonicalType);
            newElems.add(complex);
        }
        complexes.clear();
        complexes.addAll(newElems);
    }

    private void buildNameAccordion() {
        final Accordion accordion = new Accordion("nameAccordion",
                Collections.<ITab>singletonList(new AbstractTab(Model.of("name")) {

                    private static final long serialVersionUID = -5861786415855103549L;

                    @Override
                    public WebMarkupContainer getPanel(final String panelId) {
                        return buildNameAccordionContent(panelId);
                    }

                }), Model.of(-1)); // accordion closed at beginning
        add(accordion.setOutputMarkupId(true));
    }

    private void buildAddressAccordion(final SCIMUserAddressConf address, final AddressCanonicalType canonicalType) {
        Accordion accordion = new Accordion("addressesAccordion_" + address.getType().name(),
                Collections.<ITab>singletonList(new AbstractTab(Model.of("address." + address.getType().name())) {

                    private static final long serialVersionUID = -5861786415855103549L;

                    @Override
                    public WebMarkupContainer getPanel(final String panelId) {
                        return buildAddressAccordionContent(address, canonicalType, panelId);
                    }

                }), Model.of(-1)); // accordion closed at beginning
        add(accordion.setOutputMarkupId(true));
    }

    private <T extends Enum<?>> void buildComplexAccordion(
            final SCIMComplexConf<T> complex,
            final String basePanelId,
            final String baseTabId,
            final T canonicalType) {

        Accordion accordion = new Accordion(basePanelId + '_' + complex.getType().name(),
                Collections.<ITab>singletonList(new AbstractTab(Model.of(baseTabId + '.' + complex.getType().name())) {

                    private static final long serialVersionUID = -5861786415855103549L;

                    @Override
                    public WebMarkupContainer getPanel(final String panelId) {
                        return buildComplexAccordionContent(complex, canonicalType, panelId);
                    }

                }), Model.of(-1)); // accordion closed at beginning
        add(accordion.setOutputMarkupId(true));
    }

    private SCIMConfAccordionContainer buildNameAccordionContent(final String panelId) {
        final List<AjaxTextFieldPanel> panelList = new ArrayList<>();

        AjaxTextFieldPanel nameFamilyNamePanel = new AjaxTextFieldPanel("accordionContent", "name.familyName",
            new PropertyModel<>(scimUserConf.getName(), "accordionContent") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return scimUserConf.getName().getFamilyName();
                }

                @Override
                public void setObject(final String object) {
                    scimUserConf.getName().setFamilyName(object);
                }

            });
        nameFamilyNamePanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel nameFormattedPanel = new AjaxTextFieldPanel("accordionContent", "name.formatted",
            new PropertyModel<>(scimUserConf.getName(), "accordionContent") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return scimUserConf.getName().getFormatted();
                }

                @Override
                public void setObject(final String object) {
                    scimUserConf.getName().setFormatted(object);
                }

            });
        nameFormattedPanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel nameGivenNamePanel = new AjaxTextFieldPanel("accordionContent", "name.givenName",
            new PropertyModel<>(scimUserConf.getName(), "accordionContent") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return scimUserConf.getName().getGivenName();
                }

                @Override
                public void setObject(final String object) {
                    scimUserConf.getName().setGivenName(object);
                }

            });
        nameGivenNamePanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel nameHonorificPrefixPanel = new AjaxTextFieldPanel("accordionContent", "name.honorificPrefix",
            new PropertyModel<>(scimUserConf.getName(), "accordionContent") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return scimUserConf.getName().getHonorificPrefix();
                }

                @Override
                public void setObject(final String object) {
                    scimUserConf.getName().setHonorificPrefix(object);
                }

            });
        nameHonorificPrefixPanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel nameHonorificSuffixPanel = new AjaxTextFieldPanel("accordionContent", "name.honorificSuffix",
            new PropertyModel<>(scimUserConf.getName(), "accordionContent") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return scimUserConf.getName().getHonorificSuffix();
                }

                @Override
                public void setObject(final String object) {
                    scimUserConf.getName().setHonorificSuffix(object);
                }

            });
        nameHonorificSuffixPanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel nameMiddleNamePanel = new AjaxTextFieldPanel("accordionContent", "name.middleName",
            new PropertyModel<>(scimUserConf.getName(), "accordionContent") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return scimUserConf.getName().getMiddleName();
                }

                @Override
                public void setObject(final String object) {
                    scimUserConf.getName().setMiddleName(object);
                }

            });
        nameMiddleNamePanel.setChoices(plainSchemaNames);

        panelList.add(nameFamilyNamePanel);
        panelList.add(nameFormattedPanel);
        panelList.add(nameGivenNamePanel);
        panelList.add(nameHonorificPrefixPanel);
        panelList.add(nameHonorificSuffixPanel);
        panelList.add(nameMiddleNamePanel);

        return new SCIMConfAccordionContainer(panelId, panelList);
    }

    private <T extends Enum<?>> SCIMConfAccordionContainer buildComplexAccordionContent(
            final SCIMComplexConf<T> complex,
            final T canonicalType,
            final String panelId) {

        final List<AjaxTextFieldPanel> panelList = new ArrayList<>();
        final String fieldName = panelId + '.' + canonicalType.name();

        AjaxTextFieldPanel displayPanel = new AjaxTextFieldPanel("accordionContent", fieldName + ".display",
            new PropertyModel<>(complex, "accordionContent") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return complex.getDisplay();
                }

                @Override
                public void setObject(final String object) {
                    complex.setDisplay(object);
                }

            });
        displayPanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel valuePanel = new AjaxTextFieldPanel("accordionContent", fieldName + ".value",
            new PropertyModel<>(complex, "accordionContent") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return complex.getValue();
                }

                @Override
                public void setObject(final String object) {
                    complex.setValue(object);
                }

            });
        valuePanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel primaryPanel = new AjaxTextFieldPanel("accordionContent", fieldName + ".primary",
            new PropertyModel<>(complex, "accordionContent") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return String.valueOf(complex.isPrimary());
                }

                @Override
                public void setObject(final String object) {
                    complex.setPrimary(Boolean.valueOf(object));
                }

            });
        primaryPanel.setChoices(List.of("true", "false"));

        panelList.add(displayPanel);
        panelList.add(valuePanel);
        panelList.add(primaryPanel);

        return new SCIMConfAccordionContainer(panelId, panelList);
    }

    private SCIMConfAccordionContainer buildAddressAccordionContent(
            final SCIMUserAddressConf address,
            final AddressCanonicalType canonicalType,
            final String panelId) {

        final List<AjaxTextFieldPanel> panelList = new ArrayList<>();
        final String fieldName = "addresses." + canonicalType.name();

        AjaxTextFieldPanel addressCountryPanel = new AjaxTextFieldPanel("accordionContent", fieldName + ".country",
            new PropertyModel<>(address, "accordionContent") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return address.getCountry();
                }

                @Override
                public void setObject(final String object) {
                    address.setCountry(object);
                }

            });
        addressCountryPanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel addressFormattedPanel = new AjaxTextFieldPanel("accordionContent", fieldName + ".formatted",
            new PropertyModel<>(address, "accordionContent") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return address.getFormatted();
                }

                @Override
                public void setObject(final String object) {
                    address.setFormatted(object);
                }

            });
        addressFormattedPanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel addressLocalityPanel = new AjaxTextFieldPanel("accordionContent", fieldName + ".locality",
            new PropertyModel<>(address, "accordionContent") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return address.getLocality();
                }

                @Override
                public void setObject(final String object) {
                    address.setLocality(object);
                }

            });
        addressLocalityPanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel addressRegionPanel = new AjaxTextFieldPanel("accordionContent", fieldName + ".region",
            new PropertyModel<>(address, "accordionContent") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return address.getRegion();
                }

                @Override
                public void setObject(final String object) {
                    address.setRegion(object);
                }

            });
        addressRegionPanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel addressPostalCodePanel =
                new AjaxTextFieldPanel("accordionContent", fieldName + ".postalCode",
                    new PropertyModel<>(address, "accordionContent") {

                        private static final long serialVersionUID = -6427731218492117883L;

                        @Override
                        public String getObject() {
                            return address.getPostalCode();
                        }

                        @Override
                        public void setObject(final String object) {
                            address.setPostalCode(object);
                        }

                    });
        addressPostalCodePanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel addressPrimaryPanel = new AjaxTextFieldPanel("accordionContent", fieldName + ".primary",
            new PropertyModel<>(address, "accordionContent") {

                private static final long serialVersionUID = -6427731218492117883L;

                @Override
                public String getObject() {
                    return String.valueOf(address.isPrimary());
                }

                @Override
                public void setObject(final String object) {
                    address.setPrimary(Boolean.valueOf(object));
                }

            });
        addressPrimaryPanel.setChoices(List.of("true", "false"));

        panelList.add(addressCountryPanel);
        panelList.add(addressFormattedPanel);
        panelList.add(addressLocalityPanel);
        panelList.add(addressRegionPanel);
        panelList.add(addressPostalCodePanel);
        panelList.add(addressPrimaryPanel);

        return new SCIMConfAccordionContainer(panelId, panelList);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);
    }
}
