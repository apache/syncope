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
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.syncope.common.lib.types.ReportTemplateFormat;
import org.apache.syncope.ide.netbeans.PluginConstants;
import org.apache.syncope.ide.netbeans.ResourceConnector;
import org.apache.syncope.ide.netbeans.service.MailTemplateManagerService;
import org.apache.syncope.ide.netbeans.service.ReportTemplateManagerService;
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

    private MailTemplateManagerService mailTemplateManagerService;

    private ReportTemplateManagerService reportTemplateManagerService;

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
                try {
                    if (PluginConstants.MAIL_TEMPLATES.equals(parentNodeName)) {
                        openMailEditor(leafNodeName);
                    } else if (PluginConstants.REPORT_XSLTS.equals(parentNodeName)) {
                        openReportEditor(leafNodeName);
                    }
                } catch (IOException e) {
                    Exceptions.printStackTrace(e);
                }
            }
        } else if (evt.getButton() == MouseEvent.BUTTON3 && evt.getClickCount() == 1) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) resourceExplorerTree.
                    getLastSelectedPathComponent();
            String selectedNodeName = (String) selectedNode.getUserObject();
            if (selectedNode.isLeaf()
                    && !PluginConstants.ROOT_NAME.equals(selectedNodeName)
                    && !PluginConstants.MAIL_TEMPLATES.equals(selectedNodeName)
                    && !PluginConstants.REPORT_XSLTS.equals(selectedNodeName)) {
                leafRightClickAction(evt, selectedNode);
            } else if (PluginConstants.MAIL_TEMPLATES.equals(selectedNodeName)) {
                folderRightClickAction(evt, mailTemplates);
            } else if (PluginConstants.REPORT_XSLTS.equals(selectedNodeName)) {
                folderRightClickAction(evt, reportXslts);
            } else if (PluginConstants.ROOT_NAME.equals(selectedNodeName)) {
                rootRightClickAction(evt);
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
                    progr.finish();
                }

            };
            REQUEST_PROCESSOR.post(tsk);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Generic Error", JOptionPane.ERROR_MESSAGE);
            ServerDetailsView serverDetails = getRefreshServerDetails();
        }

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
                progr.finish();
            }

        };
        RequestProcessor.getDefault().post(tsk);
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
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
            public void actionPerformed(final ActionEvent e) {
                String name = JOptionPane.showInputDialog("Enter Name");
                boolean added = false;
                if (!"exit".equals(e.getActionCommand())) {

                    if (node.getUserObject().equals(PluginConstants.MAIL_TEMPLATES)) {
                        MailTemplateTO mailTemplate = new MailTemplateTO();
                        mailTemplate.setKey(name);
                        added = mailTemplateManagerService.create(mailTemplate);
                        mailTemplateManagerService.setFormat(name,
                                MailTemplateFormat.HTML,
                                IOUtils.toInputStream("//Enter Content here", encodingPattern));
                        mailTemplateManagerService.setFormat(name,
                                MailTemplateFormat.TEXT,
                                IOUtils.toInputStream("//Enter Content here", encodingPattern));
                        try {
                            openMailEditor(name);
                        } catch (IOException ex) {
                            Exceptions.printStackTrace(ex);
                        }
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
                        try {
                            openReportEditor(name);
                        } catch (IOException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }

                    if (added) {
                        node.add(new DefaultMutableTreeNode(name));
                        treeModel.reload(node);
                    } else {
                        JOptionPane.showMessageDialog(
                                null, "Error while creating new element", "Error", JOptionPane.ERROR_MESSAGE);
                    }
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
                int result = JOptionPane.showConfirmDialog(null, "Do you want to delete ?");
                if (result == JOptionPane.OK_OPTION) {
                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                    boolean deleted;
                    if (parent.getUserObject().equals(PluginConstants.MAIL_TEMPLATES)) {
                        deleted = mailTemplateManagerService.delete((String) node.getUserObject());
                    } else {
                        deleted = reportTemplateManagerService.delete((String) node.getUserObject());
                    }
                    if (deleted) {
                        node.removeFromParent();
                        treeModel.reload(parent);
                    } else {
                        JOptionPane.showMessageDialog(
                                null, "Error while deleting new element", "Error", JOptionPane.ERROR_MESSAGE);
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

            try {
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
            } catch (SyncopeClientException e) {
                LOG.log(Level.SEVERE,
                        String.format("Unable to get [%s] mail template in [%s] format", name, format), e);
                if (ClientExceptionType.NotFound.equals(e.getType())) {
                    LOG.log(Level.SEVERE, String.format(
                            "Report template in [%s] format not found, create an empty one", format));
                } else {
                    JOptionPane.showMessageDialog(
                            null, String.format("Unable to get [%s] report template in [%s] format", name, format),
                            "Connection Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE,
                        String.format("Unable to get [%s] mail template in [%s] format", name, format), e);
                JOptionPane.showMessageDialog(
                        null, String.format("Unable to get [%s] mail template in [%s] format", name, format), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
            String content = is == null ? StringUtils.EMPTY : IOUtils.toString(is, encodingPattern);

            File directory = new File("Template/Mail");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File file = new File("Template/Mail/" + name + "." + type);
            FileWriter fw = new FileWriter(file);
            fw.write(content);
            fw.flush();
            FileObject fob = FileUtil.toFileObject(file.getAbsoluteFile());
            fob.setAttribute("description", "TEXT");
            DataObject data = DataObject.find(fob);
            data.getLookup().lookup(OpenCookie.class).open();
        }
    }

    private void openReportEditor(final String name) throws IOException {
        String formatStr = (String) JOptionPane.showInputDialog(null, "Select File Format",
                "File format", JOptionPane.QUESTION_MESSAGE, null,
                PluginConstants.REPORT_TEMPLATE_FORMATS, ReportTemplateFormat.FO.name());
        if (StringUtils.isNotBlank(formatStr)) {
            ReportTemplateFormat format = ReportTemplateFormat.valueOf(formatStr);

            InputStream is = null;
            try {
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
            } catch (SyncopeClientException e) {
                LOG.log(Level.SEVERE, String.format("Unable to get [%s] report template in [%s] format", name, format),
                        e);
                if (ClientExceptionType.NotFound.equals(e.getType())) {
                    LOG.log(Level.SEVERE, String.format(
                            "Report template [%s] not found, create an empty one", name));
                } else {
                    JOptionPane.showMessageDialog(
                            null, String.format("Unable to get [%s] report template in [%s] format", name, format),
                            "Connection Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, String.format("Unable to get [%s] report template in [%s] format", name, format),
                        e);
                JOptionPane.showMessageDialog(
                        null, String.format("Unable to get [%s] report template in [%s] format", name, format),
                        "Generic Error", JOptionPane.ERROR_MESSAGE);
            }
            String content = is == null ? StringUtils.EMPTY : IOUtils.toString(is, encodingPattern);

            File directory = new File("Template/Report");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File file = new File("Template/Report/" + name + "." + format.name().toLowerCase());
            FileWriter fw = new FileWriter(file);
            fw.write(content);
            fw.flush();
            FileObject fob = FileUtil.toFileObject(file.getAbsoluteFile());
            DataObject data = DataObject.find(fob);
            data.getLookup().lookup(OpenCookie.class).open();
        }
    }

    private void saveContent() {
        try {
            JTextComponent ed = EditorRegistry.lastFocusedComponent();
            Document document = ed.getDocument();
            String content = document.getText(0, document.getLength());
            String path = (String) document.getProperty(Document.TitleProperty);
            String[] temp = path.split(File.separator);
            String name = temp[temp.length - 1];
            String templateType = temp[temp.length - 2];
            temp = name.split("\\.");
            String format = temp[1];
            String key = temp[0];

            if (templateType.equals("Mail")) {
                if (format.equals("txt")) {
                    mailTemplateManagerService.setFormat(key,
                            MailTemplateFormat.TEXT,
                            IOUtils.toInputStream(content, encodingPattern));
                } else {
                    mailTemplateManagerService.setFormat(key,
                            MailTemplateFormat.HTML,
                            IOUtils.toInputStream(content, encodingPattern));
                }
            } else if (format.equals("html")) {
                reportTemplateManagerService.setFormat(key,
                        ReportTemplateFormat.HTML,
                        IOUtils.toInputStream(content, encodingPattern));
            } else if (format.equals("fo")) {
                reportTemplateManagerService.setFormat(key,
                        ReportTemplateFormat.FO,
                        IOUtils.toInputStream(content, encodingPattern));
            } else {
                reportTemplateManagerService.setFormat(key,
                        ReportTemplateFormat.CSV,
                        IOUtils.toInputStream(content, encodingPattern));
            }
        } catch (BadLocationException e) {
            Exceptions.printStackTrace(e);
        }
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
        treeModel.reload();
    }

    private void resetTree() {
        visibleRoot.removeAllChildren();
        mailTemplates.removeAllChildren();
        reportXslts.removeAllChildren();
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
