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
package org.apache.syncope.ide.eclipse.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class SyncopeViewTest {

    private static SWTWorkbenchBot BOT;

    private SWTBotView syncopeView;

    private SWTBotTreeItem mTemplateItem;

    private SWTBotTreeItem rTemplateItem;

    private SWTBotTreeItem[] mtemplateList;

    private SWTBotTreeItem[] rtemplateList;

    protected static final String ADMIN_UNAME = "admin";

    protected static final String ADMIN_PWD = "password";

    protected static final String ADDRESS = "http://localhost:9080/syncope/rest";

    @BeforeClass
    public static void beforeClass() throws Exception {
        BOT = new SWTWorkbenchBot();
    }

    @Test
    public void canOpenView() throws Exception {
        BOT.menu("Window").menu("Show View").menu("Other...").click();
        SWTBotShell shell = BOT.shell("Show View");
        shell.activate();
        BOT.tree().expandNode("Apache Syncope").select("Apache Syncope Templates").click();
        BOT.button("OK").click();
    }

    @Test
    public void canOpenLoginDialog() throws Exception {
        SWTBotView view = BOT.viewByTitle("Apache Syncope Templates");
        view.getToolbarButtons().get(0).click();
    }

    @Test
    public void canLoginAdmin() throws Exception {
        SWTBotShell loginShell = BOT.activeShell();
        loginShell.activate();
        BOT.textWithLabel("Deployment Url").setText(ADDRESS);
        BOT.textWithLabel("Username").setText(ADMIN_UNAME);
        BOT.textWithLabel("Password").setText(ADMIN_PWD);
        BOT.button("Login").click();
        BOT.waitWhile(new ICondition() {

            @Override
            public boolean test() throws Exception {
                String title = BOT.activeShell().getText();
                return title.equals("Loading Templates");
            }

            @Override
            public void init(final SWTBot bot) {
            }

            @Override
            public String getFailureMessage() {
                return "Unable to Login";
            }
        });
    }

    @Test
    public void hasCorrectTemplates() throws Exception {
        syncopeView = BOT.viewByTitle("Apache Syncope Templates");
        mTemplateItem = syncopeView.bot().tree().expandNode("Mail Templates");
        rTemplateItem = syncopeView.bot().tree().expandNode("Report Templates");

        mtemplateList = mTemplateItem.getItems();
        rtemplateList = rTemplateItem.getItems();

        assertNotNull(mtemplateList);
        assertNotNull(rtemplateList);

        assertTrue(checkMailTemplateKeys());
        assertTrue(checkReportTemplateKeys());
        assertTrue(canAddMailTemplate());
        assertTrue(canDeleteMailTemplate());
        assertTrue(canAddReportTemplate());
        assertTrue(canDeleteReportTemplate());

        assertTrue(canOpenMailTemplateEditor());
        assertTrue(canOpenReportTemplateEditor());
    }

    public boolean checkMailTemplateKeys() {
        return (mtemplateList[0].getText().equals("confirmPasswordReset")
                && mtemplateList[1].getText().equals("optin")
                && mtemplateList[2].getText().equals("requestPasswordReset")
                && mtemplateList[3].getText().equals("test"));
    }

    public boolean checkReportTemplateKeys() {
        return (rtemplateList[0].getText().equals("empty") && rtemplateList[1].getText().equals("sample"));
    }

    private boolean canAddMailTemplate() {
        SWTBotTreeItem mTemplateHead = syncopeView.bot().tree().getTreeItem("Mail Templates");
        mTemplateHead.setFocus();
        mTemplateHead.select().contextMenu("Add Template").click();
        BOT.sleep(1000);

        // Filling Dialog details
        SWTBotShell addTemplateShell = BOT.activeShell();
        addTemplateShell.activate();
        BOT.textWithLabel("Key").setText("newMailTemplate");
        BOT.button("Add").click();

        // wait for template keys reload
        BOT.waitWhile(new ICondition() {

            @Override
            public boolean test() throws Exception {
                String title = BOT.activeShell().getText();
                return title.equals("Loading Templates");
            }

            @Override
            public void init(final SWTBot bot) {
            }

            @Override
            public String getFailureMessage() {
                return "Unable to Login";
            }
        });

        // recheck available templates
        syncopeView = BOT.viewByTitle("Apache Syncope Templates");
        mTemplateItem = syncopeView.bot().tree().expandNode("Mail Templates");
        mtemplateList = mTemplateItem.getItems();

        return (mtemplateList.length == 5 && mtemplateList[1].getText().equals("newMailTemplate"));
    }

    private boolean canAddReportTemplate() {
        SWTBotTreeItem rTemplateHead = syncopeView.bot().tree().getTreeItem("Report Templates");
        rTemplateHead.setFocus();
        rTemplateHead.select().contextMenu("Add Template").click();
        BOT.sleep(1000);

        // Filling Dialog details
        SWTBotShell addTemplateShell = BOT.activeShell();
        addTemplateShell.activate();
        BOT.textWithLabel("Key").setText("newReportTemplate");
        BOT.button("Add").click();

        // wait for template keys reload
        BOT.waitWhile(new ICondition() {

            @Override
            public boolean test() throws Exception {
                String title = BOT.activeShell().getText();
                return title.equals("Loading Templates");
            }

            @Override
            public void init(final SWTBot bot) {
            }

            @Override
            public String getFailureMessage() {
                return "Unable to Login";
            }
        });

        // recheck available templates
        syncopeView = BOT.viewByTitle("Apache Syncope Templates");
        rTemplateItem = syncopeView.bot().tree().expandNode("Report Templates");
        rtemplateList = rTemplateItem.getItems();

        return (rtemplateList.length == 3 && rtemplateList[1].getText().equals("newReportTemplate"));
    }

    private boolean canDeleteMailTemplate() {
        SWTBotTreeItem mTemplateHead = syncopeView.bot().tree().getTreeItem("Mail Templates");

        if (!mTemplateHead.isExpanded()) {
            mTemplateHead.expand();
        }

        SWTBotTreeItem mNewTemplate = mTemplateHead.getNode("newMailTemplate");

        mNewTemplate.setFocus();
        mNewTemplate.select().contextMenu("Remove template").click();
        BOT.sleep(1000);
        // wait for template keys reload
        BOT.waitWhile(new ICondition() {

            @Override
            public boolean test() throws Exception {
                String title = BOT.activeShell().getText();
                return title.equals("Loading Templates");
            }

            @Override
            public void init(final SWTBot bot) {
            }

            @Override
            public String getFailureMessage() {
                return "Unable to Login";
            }
        });

        // recheck available templates
        mTemplateItem = syncopeView.bot().tree().expandNode("Mail Templates");
        mtemplateList = mTemplateItem.getItems();

        return (mtemplateList.length == 4);
    }

    private boolean canDeleteReportTemplate() {
        SWTBotTreeItem rTemplateHead = syncopeView.bot().tree().getTreeItem("Report Templates");

        if (!rTemplateHead.isExpanded()) {
            rTemplateHead.expand();
        }

        SWTBotTreeItem rNewTemplate = rTemplateHead.getNode("newReportTemplate");

        rNewTemplate.setFocus();
        rNewTemplate.select().contextMenu("Remove template").click();
        BOT.sleep(1000);
        // wait for template keys reload
        BOT.waitWhile(new ICondition() {

            @Override
            public boolean test() throws Exception {
                String title = BOT.activeShell().getText();
                return title.equals("Loading Templates");
            }

            @Override
            public void init(final SWTBot bot) {
            }

            @Override
            public String getFailureMessage() {
                return "Unable to Login";
            }
        });

        // recheck available templates
        rTemplateItem = syncopeView.bot().tree().expandNode("Report Templates");
        rtemplateList = rTemplateItem.getItems();

        return (rtemplateList.length == 2);
    }

    public boolean canOpenMailTemplateEditor() {
        final int initBotEditors = BOT.editors().size();
        SWTBotTreeItem optin = syncopeView.bot().tree().expandNode("Mail Templates").getNode("optin");
        optin.setFocus();
        optin.select().contextMenu("View Template").click();

        // wait for template keys reload
        BOT.waitWhile(new ICondition() {

            @Override
            public boolean test() throws Exception {
                return BOT.editors().size() == initBotEditors;
            }

            @Override
            public void init(final SWTBot bot) {
            }

            @Override
            public String getFailureMessage() {
                return "Unable to Login";
            }
        });
        return !BOT.editors().isEmpty();
    }

    public boolean canOpenReportTemplateEditor() {
        final int initBotEditors = BOT.editors().size();
        SWTBotTreeItem sample = syncopeView.bot().tree().expandNode("Report Templates").getNode("sample");
        sample.setFocus();
        sample.select().contextMenu("View Template").click();

        // wait for template keys reload
        BOT.waitWhile(new ICondition() {

            @Override
            public boolean test() throws Exception {
                return BOT.editors().size() == initBotEditors;
            }

            @Override
            public void init(final SWTBot bot) {
            }

            @Override
            public String getFailureMessage() {
                return "Unable to Login";
            }
        });
        return !BOT.editors().isEmpty();
    }

}
