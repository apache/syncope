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
package org.apache.syncope.ide.netbeans.view;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.MailTemplateTO;
import org.apache.syncope.common.lib.to.ReportTemplateTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.syncope.common.lib.types.ReportTemplateFormat;
import org.apache.syncope.common.lib.types.ImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.ide.netbeans.PluginConstants;
import org.apache.syncope.ide.netbeans.ResourceConnector;
import org.apache.syncope.ide.netbeans.service.MailTemplateManagerService;
import org.apache.syncope.ide.netbeans.service.ReportTemplateManagerService;
import org.apache.syncope.ide.netbeans.service.ImplementationManagerService;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Cancellable;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.apache.syncope.ide.netbeans//ResourceExplorer//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "ResourceExplorerTopComponent",
        iconBase = "images/syncope.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "explorer", openAtStartup = false)
@ActionID(category = "Window", id = "org.apache.syncope.ide.netbeans.ResourceExplorerTopComponent")
@ActionReference(path = "Menu/Window")
@TopComponent.OpenActionRegistration(
        displayName = "Apache Syncope",
        preferredID = "ResourceExplorerTopComponent"
)
public final class ResourceExplorerTopComponent extends TopComponent {

    private static final long serialVersionUID = -1643737786852621861L;

    public static final Logger LOG = Logger.getLogger("ResourceExplorerTopComponent");

    private static final RequestProcessor REQUEST_PROCESSOR = new RequestProcessor(ResourceExplorerTopComponent.class);

    private final DefaultTreeModel treeModel;

    private final DefaultMutableTreeNode visibleRoot;

    private final DefaultMutableTreeNode root;

    private final DefaultMutableTreeNode mailTemplates;

    private final DefaultMutableTreeNode reportXslts;

    private final DefaultMutableTreeNode groovyScripts;

    private MailTemplateManagerService mailTemplateManagerService;

    private ReportTemplateManagerService reportTemplateManagerService;

    private ImplementationManagerService implementationManagerService;

    private Charset encodingPattern;

    public ResourceExplorerTopComponent() {

        initComponents();
        setName(PluginConstants.ROOT_NAME);
        setToolTipText(PluginConstants.TOOL_TIP_TEXT);

        treeModel = (DefaultTreeModel) resourceExplorerTree.getModel();
        root = (DefaultMutableTreeNode) treeModel.getRoot();
        visibleRoot = new DefaultMutableTreeNode(PluginConstants.ROOT_NAME);
        mailTemplates = new DefaultMutableTreeNode(PluginConstants.MAIL_TEMPLATES);
        reportXslts = new DefaultMutableTreeNode(PluginConstants.REPORT_XSLTS);
        groovyScripts = new DefaultMutableTreeNode(PluginConstants.GROOVY_SCRIPTS);
        root.add(visibleRoot);
        initTemplatesTree();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    //CHECKSTYLE:OFF
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        resourceExplorerTree = new javax.swing.JTree();

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        resourceExplorerTree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        resourceExplorerTree.setRootVisible(false);
        resourceExplorerTree.setScrollsOnExpand(true);
        resourceExplorerTree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                resourceExplorerTreeMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(resourceExplorerTree);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 258, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 445, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    //CHECKSTYLE:ON

    @Override
    public Image getIcon() {
        return new ImageIcon(getClass().getResource("/org/apache/syncope/ide/netbeans/view/favicon.png")).getImage();
    }

    private void resourceExplorerTreeMouseClicked(final java.awt.event.MouseEvent evt) {
        if (evt.getButton() == MouseEvent.BUTTON1 && evt.getClickCount() == 2) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) resourceExplorerTree.
                    getLastSelectedPathComponent();
            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent();
            String parentNodeName = parentNode == null ? null : String.valueOf(parentNode.getUserObject());
            if (selectedNode.isLeaf() && StringUtils.isNotBlank(parentNodeName)) {
                String leafNodeName = (String) selectedNode.getUserObject();
                DefaultMutableTreeNode grandParentNode = (DefaultMutableTreeNode) parentNode.getParent();
                String grandParentNodeName = (String) grandParentNode.getUserObject();
                try {
                    if (PluginConstants.MAIL_TEMPLATES.equals(parentNodeName)) {
                        openMailEditor(leafNodeName);
                    } else if (PluginConstants.REPORT_XSLTS.equals(parentNodeName)) {
                        openReportEditor(leafNodeName);
                    } else if (PluginConstants.GROOVY_SCRIPTS.equals(grandParentNodeName)) {
                        openScriptEditor(leafNodeName, parentNodeName);
                    }
                } catch (SyncopeClientException ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage(), "Syncope Error", JOptionPane.ERROR_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "While navigating explorer tree {0}", ex);
                    getRefreshServerDetails().setVisible(true);
                }
            }
        } else if (evt.getButton() == MouseEvent.BUTTON3 && evt.getClickCount() == 1) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) resourceExplorerTree.
                    getLastSelectedPathComponent();

            if (selectedNode != null) {
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
                String parentNodeName = (String) parent.getUserObject();
                String selectedNodeName = (String) selectedNode.getUserObject();
                if (selectedNode.isLeaf()
                        && !PluginConstants.ROOT_NAME.equals(selectedNodeName)
                        && !PluginConstants.MAIL_TEMPLATES.equals(selectedNodeName)
                        && !PluginConstants.REPORT_XSLTS.equals(selectedNodeName)
                        && !PluginConstants.GROOVY_SCRIPTS.equals(parentNodeName)) {
                    leafRightClickAction(evt, selectedNode);
                } else if (PluginConstants.MAIL_TEMPLATES.equals(selectedNodeName)) {
                    folderRightClickAction(evt, mailTemplates);
                } else if (PluginConstants.REPORT_XSLTS.equals(selectedNodeName)) {
                    folderRightClickAction(evt, reportXslts);
                } else if (PluginConstants.GROOVY_SCRIPTS.equals(parentNodeName)) {
                    folderRightClickAction(evt, selectedNode);
                } else if (PluginConstants.ROOT_NAME.equals(selectedNodeName)) {
                    rootRightClickAction(evt);
                }
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTree resourceExplorerTree;
    // End of variables declaration//GEN-END:variables

    @Override
    public void componentOpened() {
        // look for connection preferences
        Preferences prefs = NbPreferences.forModule(ResourceExplorerTopComponent.class);
        if (StringUtils.isBlank(prefs.get("scheme", null))
                || StringUtils.isBlank(prefs.get("host", null))
                || StringUtils.isBlank(prefs.get("port", null))
                || StringUtils.isBlank(prefs.get("username", null))
                || StringUtils.isBlank(prefs.get("password", null))) {
            new ServerDetailsView(null, true).setVisible(true);
        }
        try {
            mailTemplateManagerService = ResourceConnector.getMailTemplateManagerService();
            reportTemplateManagerService = ResourceConnector.getReportTemplateManagerService();
            implementationManagerService = ResourceConnector.getImplementationManagerService();
            // init tree, because on close it is reset
            initTemplatesTree();
            // Load templates
            LOG.info("Loading Apache Syncope templates...");
            Runnable tsk = new Runnable() {

                @Override
                public void run() {

                    final ProgressHandle progr = ProgressHandle.createHandle("Loading Templates", new Cancellable() {

                        @Override
                        public boolean cancel() {
                            return true;
                        }
                    });

                    progr.start();
                    progr.progress("Loading Templates.");
                    addMailTemplates();
                    addReportXslts();
                    addGroovyScripts();
                    progr.finish();
                }

            };
            REQUEST_PROCESSOR.post(tsk);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Generic Error", JOptionPane.ERROR_MESSAGE);
            ServerDetailsView serverDetails = getRefreshServerDetails();
        } catch (Exception ex) {
            getRefreshServerDetails().setVisible(true);
        }
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component
        resetTree();
    }

    void writeProperties(final java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(final java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    private void addMailTemplates() {
        List<MailTemplateTO> mailTemplateList = mailTemplateManagerService.list();
        for (MailTemplateTO mailTemplate : mailTemplateList) {
            this.mailTemplates.add(new DefaultMutableTreeNode(
                    mailTemplate.getKey()));
        }
        treeModel.reload();
    }

    private void addReportXslts() {
        List<ReportTemplateTO> reportTemplates = reportTemplateManagerService.list();
        for (ReportTemplateTO reportTemplate : reportTemplates) {
            reportXslts.add(new DefaultMutableTreeNode(
                    reportTemplate.getKey()));
        }
        treeModel.reload();
    }

    private void addGroovyScripts() {
        for (ImplementationType type : ImplementationType.values()) {
            String implType = type.toString();
            DefaultMutableTreeNode tempNode = new DefaultMutableTreeNode(implType.toString());
            if (implType.equals("JWT_SSO_PROVIDER") || implType.equals("AUDIT_APPENDER")) {
                continue;
            }
            List<ImplementationTO> scripts = implementationManagerService.list(type);
            for (ImplementationTO script : scripts) {
                if (script.getEngine() == ImplementationEngine.GROOVY) {
                    tempNode.add(new DefaultMutableTreeNode(
                            script.getKey()));
                }
            }
            groovyScripts.add(tempNode);
        }

        treeModel.reload();
    }

    private void rootRightClickAction(final MouseEvent evt) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem refreshItem = new JMenuItem("Refresh Templates");
        JMenuItem resetConnectionItem = new JMenuItem("Reset Connection");
        menu.add(refreshItem);
        menu.add(resetConnectionItem);

        refreshItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                // simulate close and open to refresh the tree
                componentClosed();
                componentOpened();
            }
        });

        resetConnectionItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent evt) {
                ServerDetailsView serverDetails = getRefreshServerDetails();
                // set previous preferences
                Preferences prefs = NbPreferences.forModule(ResourceExplorerTopComponent.class);
                serverDetails.setDetails(prefs.get("scheme", "http"),
                        prefs.get("host", "localhost"),
                        prefs.get("port", "8080"),
                        prefs.get("username", StringUtils.EMPTY),
                        prefs.get("password", StringUtils.EMPTY));
                // reset connection preferences
                prefs.remove("scheme");
                prefs.remove("host");
                prefs.remove("port");
                prefs.remove("username");
                prefs.remove("password");
                serverDetails.setVisible(true);
            }
        });

        menu.show(evt.getComponent(), evt.getX(), evt.getY());
    }

    private void folderRightClickAction(final MouseEvent evt,
            final DefaultMutableTreeNode node) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem addItem = new JMenuItem("New");
        menu.add(addItem);

        addItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent event) {
                try {
                    String name = JOptionPane.showInputDialog("Enter Name");
                    if (StringUtils.isBlank(name)) {
                        return;
                    }

                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                    boolean added = false;

                    if (!"exit".equals(event.getActionCommand())) {
                        if (PluginConstants.MAIL_TEMPLATES.equals(node.getUserObject())) {

                            MailTemplateTO mailTemplate = new MailTemplateTO();
                            mailTemplate.setKey(name);
                            added = mailTemplateManagerService.create(mailTemplate);
                            mailTemplateManagerService.setFormat(name,
                                    MailTemplateFormat.HTML,
                                    IOUtils.toInputStream("//Enter Content here", encodingPattern));
                            mailTemplateManagerService.setFormat(name,
                                    MailTemplateFormat.TEXT,
                                    IOUtils.toInputStream("//Enter Content here", encodingPattern));
                            openMailEditor(name);
                        } else if (PluginConstants.GROOVY_SCRIPTS.equals(parent.getUserObject())) {
                            ImplementationTO newNode = new ImplementationTO();
                            ImplementationType type = getType((String) node.getUserObject());
                            newNode.setKey(name);
                            newNode.setEngine(ImplementationEngine.GROOVY);
                            newNode.setType(type);
                            String templateClassName = null;
                            switch (type) {
                                case REPORTLET:
                                    templateClassName = "MyReportlet";
                                    break;

                                case ACCOUNT_RULE:
                                    templateClassName = "MyAccountRule";
                                    break;

                                case PASSWORD_RULE:
                                    templateClassName = "MyPasswordRule";
                                    break;

                                case ITEM_TRANSFORMER:
                                    templateClassName = "MyItemTransformer";
                                    break;

                                case TASKJOB_DELEGATE:
                                    templateClassName = "MySchedTaskJobDelegate";
                                    break;

                                case RECON_FILTER_BUILDER:
                                    templateClassName = "MyReconFilterBuilder";
                                    break;

                                case LOGIC_ACTIONS:
                                    templateClassName = "MyLogicActions";
                                    break;

                                case PROPAGATION_ACTIONS:
                                    templateClassName = "MyPropagationActions";
                                    break;

                                case PULL_ACTIONS:
                                    templateClassName = "MyPullActions";
                                    break;

                                case PUSH_ACTIONS:
                                    templateClassName = "MyPushActions";
                                    break;

                                case PULL_CORRELATION_RULE:
                                    templateClassName = "MyPullCorrelationRule";
                                    break;

                                case PUSH_CORRELATION_RULE:
                                    templateClassName = "MyPushCorrelationRule";
                                    break;

                                case VALIDATOR:
                                    templateClassName = "MyValidator";
                                    break;

                                case RECIPIENTS_PROVIDER:
                                    templateClassName = "MyRecipientsProvider";
                                    break;

                                default:
                            }
                            newNode.setBody(IOUtils.toString(
                                    getClass().getResourceAsStream("/org/apache/syncope/ide/netbeans/implementations/"
                                            + templateClassName + ".groovy")));
                            added = implementationManagerService.create(newNode);
                            openScriptEditor(name, (String) node.getUserObject());
                        } else {
                            ReportTemplateTO reportTemplate = new ReportTemplateTO();
                            reportTemplate.setKey(name);
                            added = reportTemplateManagerService.create(reportTemplate);
                            reportTemplateManagerService.setFormat(name,
                                    ReportTemplateFormat.FO,
                                    IOUtils.toInputStream("//Enter content here", encodingPattern));
                            reportTemplateManagerService.setFormat(name,
                                    ReportTemplateFormat.CSV,
                                    IOUtils.toInputStream("//Enter content here", encodingPattern));
                            reportTemplateManagerService.setFormat(name,
                                    ReportTemplateFormat.HTML,
                                    IOUtils.toInputStream("//Enter content here", encodingPattern));
                            openReportEditor(name);
                        }

                        if (added) {
                            node.add(new DefaultMutableTreeNode(name));
                            treeModel.reload(node);
                        } else {
                            JOptionPane.showMessageDialog(
                                    null, "Error while creating new element", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } catch (SyncopeClientException excp) {
                    JOptionPane.showMessageDialog(null, excp.getMessage(), "Syncope Error", JOptionPane.ERROR_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } catch (Exception exc) {
                    LOG.log(Level.WARNING, "Refreshing after exception");
                    getRefreshServerDetails().setVisible(true);
                }
            }
        });

        menu.show(evt.getComponent(), evt.getX(), evt.getY());
    }

    private void leafRightClickAction(final MouseEvent evt,
            final DefaultMutableTreeNode node) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete");
        menu.add(deleteItem);

        deleteItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(null, "Are you sure to delete the item?");
                if (result == JOptionPane.OK_OPTION) {
                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                    String nodeName = (String) node.getUserObject();
                    boolean deleted = false;
                    try {
                        if (PluginConstants.MAIL_TEMPLATES.equals(parent.getUserObject())) {
                            deleted = mailTemplateManagerService.delete(nodeName);
                        } else if (PluginConstants.REPORT_XSLTS.equals(parent.getUserObject())) {
                            deleted = reportTemplateManagerService.delete(nodeName);
                        } else {
                            ImplementationType type = getType((String) parent.getUserObject());
                            deleted = implementationManagerService.delete(type, nodeName);
                        }
                        if (deleted) {
                            node.removeFromParent();
                            treeModel.reload(parent);
                        } else {
                            JOptionPane.showMessageDialog(
                                    null, "Error while deleting new element", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (SyncopeClientException exc) {
                        JOptionPane.showMessageDialog(
                                null, exc.getMessage(), "Syncope Error", JOptionPane.ERROR_MESSAGE);
                    } catch (Exception ex) {
                        getRefreshServerDetails().setVisible(true);
                    }
                }
            }
        });

        menu.show(evt.getComponent(), evt.getX(), evt.getY());
    }

    private void openMailEditor(final String name) throws IOException {
        String formatStr = (String) JOptionPane.showInputDialog(null, "Select File Format",
                "File format", JOptionPane.QUESTION_MESSAGE, null,
                PluginConstants.MAIL_TEMPLATE_FORMATS, MailTemplateFormat.TEXT.name());

        if (StringUtils.isNotBlank(formatStr)) {
            MailTemplateFormat format = MailTemplateFormat.valueOf(formatStr);
            String type = null;
            InputStream is = null;

            switch (format) {
                case HTML:
                    type = "html";
                    is = (InputStream) mailTemplateManagerService.getFormat(name, MailTemplateFormat.HTML);
                    break;
                case TEXT:
                    type = "txt";
                    is = (InputStream) mailTemplateManagerService.getFormat(name, MailTemplateFormat.TEXT);
                    break;
                default:
                    LOG.log(Level.SEVERE, String.format("Format [%s] not supported", format));
                    break;
            }
            String content = is == null ? StringUtils.EMPTY : IOUtils.toString(is, encodingPattern);

            String mailTemplatesDirName = System.getProperty("java.io.tmpdir") + "/Templates/Mail/";
            File mailTemplatesDir = new File(mailTemplatesDirName);
            if (!mailTemplatesDir.exists()) {
                mailTemplatesDir.mkdirs();
            }
            File file = new File(mailTemplatesDirName + name + "." + type);
            FileWriter fw = new FileWriter(file);
            fw.write(content);
            fw.flush();
            FileObject fob = FileUtil.toFileObject(file.getAbsoluteFile());
            fob.setAttribute("description", "TEXT");
            DataObject data = DataObject.find(fob);
            data.getLookup().lookup(OpenCookie.class).open();
            data.addPropertyChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(final PropertyChangeEvent evt) {
                    if (DataObject.PROP_MODIFIED.equals(evt.getPropertyName())) {
                        //save item remotely
                        LOG.info(String.format("Saving Mail template [%s]", name));
                        saveContent();
                    }
                }
            });
        }
    }

    private void openScriptEditor(final String name, final String type) throws IOException {
        ImplementationTO node = implementationManagerService.read(getType(type), name);
        String groovyScriptsDirName = System.getProperty("java.io.tmpdir") + "/Groovy/"
                + node.getType().toString() + "/";
        File groovyScriptsDir = new File(groovyScriptsDirName);
        if (!groovyScriptsDir.exists()) {
            groovyScriptsDir.mkdirs();
        }
        File file = new File(groovyScriptsDirName + name + ".groovy");
        FileWriter fw = new FileWriter(file);
        fw.write(node.getBody());
        fw.flush();
        FileObject fob = FileUtil.toFileObject(file.getAbsoluteFile());
        DataObject data = DataObject.find(fob);
        data.getLookup().lookup(OpenCookie.class).open();
        data.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                if (DataObject.PROP_MODIFIED.equals(evt.getPropertyName())) {
                    //save item remotely
                    LOG.info(String.format("Saving Groovy template [%s]", name));
                    saveContent();
                }
            }
        });

    }

    private void openReportEditor(final String name) throws IOException {
        String formatStr = (String) JOptionPane.showInputDialog(null, "Select File Format",
                "File format", JOptionPane.QUESTION_MESSAGE, null,
                PluginConstants.REPORT_TEMPLATE_FORMATS, ReportTemplateFormat.FO.name());

        if (StringUtils.isNotBlank(formatStr)) {
            ReportTemplateFormat format = ReportTemplateFormat.valueOf(formatStr);
            InputStream is = null;
            switch (format) {
                case HTML:
                    is = (InputStream) reportTemplateManagerService.getFormat(name, ReportTemplateFormat.HTML);
                    break;
                case CSV:
                    is = (InputStream) reportTemplateManagerService.getFormat(name, ReportTemplateFormat.CSV);
                    break;
                case FO:
                    is = (InputStream) reportTemplateManagerService.getFormat(name, ReportTemplateFormat.FO);
                    break;
                default:
                    LOG.log(Level.SEVERE, String.format("Format [%s] not supported", format));
                    break;
            }
            String content = is == null ? StringUtils.EMPTY : IOUtils.toString(is, encodingPattern);

            String reportTemplatesDirName = System.getProperty("java.io.tmpdir") + "/Templates/Report/";
            File reportTemplatesDir = new File(reportTemplatesDirName);
            if (!reportTemplatesDir.exists()) {
                reportTemplatesDir.mkdirs();
            }
            File file = new File(reportTemplatesDirName + name + "." + format.
                    name().toLowerCase());
            FileWriter fw = new FileWriter(file);
            fw.write(content);
            fw.flush();
            FileObject fob = FileUtil.toFileObject(file.getAbsoluteFile());
            DataObject data = DataObject.find(fob);
            data.getLookup().lookup(OpenCookie.class).open();
            data.addPropertyChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(final PropertyChangeEvent evt) {
                    if (DataObject.PROP_MODIFIED.equals(evt.getPropertyName())) {
                        //save item remotely
                        LOG.info(String.format("Saving Report template [%s]", name));
                        saveContent();
                    }
                }
            });
        }
    }

    private void saveContent() {
        try {
            JTextComponent ed = EditorRegistry.lastFocusedComponent();
            Document document = ed.getDocument();
            String content = document.getText(0, document.getLength());
            String path = (String) document.getProperty(Document.TitleProperty);
            String[] temp = path.split(File.separator.replace("\\", "\\\\"));
            String name = temp[temp.length - 1];
            String fileName = temp[temp.length - 3];
            String templateType = temp[temp.length - 2];
            temp = name.split("\\.");
            String format = temp[1];
            String key = temp[0];

            if ("Mail".equals(templateType)) {
                if ("txt".equals(format)) {
                    mailTemplateManagerService.setFormat(key,
                            MailTemplateFormat.TEXT,
                            IOUtils.toInputStream(content, encodingPattern));
                } else {
                    mailTemplateManagerService.setFormat(key,
                            MailTemplateFormat.HTML,
                            IOUtils.toInputStream(content, encodingPattern));
                }
            } else if ("html".equals(format)) {
                reportTemplateManagerService.setFormat(key,
                        ReportTemplateFormat.HTML,
                        IOUtils.toInputStream(content, encodingPattern));
            } else if ("fo".equals(format)) {
                reportTemplateManagerService.setFormat(key,
                        ReportTemplateFormat.FO,
                        IOUtils.toInputStream(content, encodingPattern));
            } else if ("csv".equals(format)) {
                reportTemplateManagerService.setFormat(key,
                        ReportTemplateFormat.CSV,
                        IOUtils.toInputStream(content, encodingPattern));
            } else if ("Groovy".equals(fileName)) {
                ImplementationTO node = implementationManagerService.read(getType(templateType), key);
                node.setBody(content);
                implementationManagerService.update(node);
            }
        } catch (BadLocationException e) {
            Exceptions.printStackTrace(e);
        } catch (Exception e) {
            getRefreshServerDetails().setVisible(true);
        }
    }

    private ImplementationType getType(final String typeName) {
        ImplementationType type = null;
        for (ImplementationType implType : ImplementationType.values()) {
            if (implType.toString().equals(typeName)) {
                type = implType;
            }

        }
        return (type);
    }

    private void closeComponent() {
        boolean isClosed = this.close();
        if (!isClosed) {
            LOG.log(Level.SEVERE, "Unable to close {0}", getClass().getSimpleName());
        }
    }

    private void initTemplatesTree() {
        visibleRoot.add(mailTemplates);
        visibleRoot.add(reportXslts);
        visibleRoot.add(groovyScripts);
        treeModel.reload();
    }

    private void resetTree() {
        visibleRoot.removeAllChildren();
        mailTemplates.removeAllChildren();
        reportXslts.removeAllChildren();
        groovyScripts.removeAllChildren();
        treeModel.reload();
    }

    private ServerDetailsView getRefreshServerDetails() {
        return new ServerDetailsView(null, true) {

            private static final long serialVersionUID = 3926689175745815987L;

            @Override
            protected void okButtonActionPerformed(final ActionEvent evt) {
                super.okButtonActionPerformed(evt);
                // simulate close and open to refresh the tree
                componentClosed();
                componentOpened();
            }

        };
    }

}
