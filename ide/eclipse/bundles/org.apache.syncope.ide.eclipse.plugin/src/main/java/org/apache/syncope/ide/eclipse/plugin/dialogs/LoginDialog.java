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
package org.apache.syncope.ide.eclipse.plugin.dialogs;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class LoginDialog extends TitleAreaDialog {
    private Text deploymentUrlInp;
    private Text usernameInp;
    private Text passwordInp;
    private String deploymentUrl;
    private String username;
    private String password;
    private Button resetbutton;
    private Preferences instancePreferences = InstanceScope.INSTANCE
            .getNode("org.apache.syncope.ide.eclipse.plugin.dialogs.prefs");
    private Preferences configPreferences = ConfigurationScope.INSTANCE
            .getNode("org.apache.syncope.ide.eclipse.plugin.dialogs.prefs");
    private Preferences sub1 = configPreferences.node("depPref");
    private Preferences sub2 = instancePreferences.node("userPref");

    public LoginDialog(final Shell parentShell) {
        super(parentShell);
    }

    @Override
    public void create() {
        super.create();
        setTitle("Login to Apache Syncope");
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Login", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        resetbutton = createButton(parent, IDialogConstants.NO_ID, "Reset Fields", false);
        resetbutton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                deploymentUrlInp.setText("");
                usernameInp.setText("");
                passwordInp.setText("");

                // add to preferences
                saveInput();
            }
        });
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(2, false);
        container.setLayout(layout);

        createDeploymentUrl(container);
        createUsername(container);
        createPassword(container);

        return area;
    }

    private void createDeploymentUrl(final Composite container) {
        Label lbtDeploymentUrl = new Label(container, SWT.NONE);
        lbtDeploymentUrl.setText("Deployment Url");

        GridData dataDeploymentUrl = new GridData();
        dataDeploymentUrl.grabExcessHorizontalSpace = true;
        dataDeploymentUrl.horizontalAlignment = GridData.FILL;
        deploymentUrlInp = new Text(container, SWT.BORDER);
        deploymentUrlInp.setLayoutData(dataDeploymentUrl);
        String depurl = sub1.get("deploymentUrl", "http://");
        if (depurl != null) {
            deploymentUrlInp.setText(depurl);
        }
    }

    private void createUsername(final Composite container) {
        Label lbtUsername = new Label(container, SWT.NONE);
        lbtUsername.setText("Username");

        GridData dataUsername = new GridData();
        dataUsername.grabExcessHorizontalSpace = true;
        dataUsername.horizontalAlignment = GridData.FILL;
        usernameInp = new Text(container, SWT.BORDER);
        usernameInp.setLayoutData(dataUsername);
        String usernameString = sub2.get("username", "");
        if (usernameString != null) {
            usernameInp.setText(usernameString);
        }
    }

    private void createPassword(final Composite container) {
        Label lbtPassword = new Label(container, SWT.NONE);
        lbtPassword.setText("Password");

        GridData dataPassword = new GridData();
        dataPassword.grabExcessHorizontalSpace = true;
        dataPassword.horizontalAlignment = GridData.FILL;
        passwordInp = new Text(container, SWT.BORDER | SWT.PASSWORD);
        passwordInp.setLayoutData(dataPassword);
        String passwordString = sub2.get("password", "");
        if (passwordString != null) {
            passwordInp.setText(passwordString);
        }
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    // save content of the Text fields because they get disposed
    // as soon as the Dialog closes
    private void saveInput() {
        deploymentUrl = deploymentUrlInp.getText();
        username = usernameInp.getText();
        password = passwordInp.getText();

        // add data to preferences for repopulation later
        sub1.put("deploymentUrl", deploymentUrl);
        sub2.put("username", username);
        sub2.put("password", password);

        try {
            instancePreferences.flush();
            configPreferences.flush();
        } catch (final BackingStoreException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void okPressed() {
        saveInput();
        setReturnCode(200);
        super.okPressed();
    }

    public String getDeploymentUrl() {
        return deploymentUrl;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

}
