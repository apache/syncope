/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.syncope.netbeans.plugin.view.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import org.apache.syncope.netbeans.plugin.connector.ResourceConnector;
import org.apache.syncope.netbeans.plugin.constants.PluginConstants;
import org.apache.syncope.netbeans.plugin.service.MailTemplateManagerService;
import org.apache.syncope.netbeans.plugin.service.ReportTemplateManagerService;

/**
 *
 * @author nuwan
 */
public class LeafNodePopupMenu extends JPopupMenu {

    private JMenuItem deleteItem;
    private MailTemplateManagerService mailTemplateManagerService;
    private ReportTemplateManagerService reportTemplateManagerService;
    private DefaultMutableTreeNode leaf;
    private JTree tree;
    private DefaultTreeModel treeModel;

    public LeafNodePopupMenu(DefaultMutableTreeNode leaf, JTree tree) {
        deleteItem = new JMenuItem("Delete");
        add(deleteItem);
        this.tree = tree;
        treeModel = (DefaultTreeModel) tree.getModel();

        mailTemplateManagerService = ResourceConnector.getMailTemplateManagerService();
        reportTemplateManagerService = ResourceConnector.getReportTemplateManagerService();
        this.leaf = leaf;

        deleteItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(null, "Do you want to delete ?");
                if (result == JOptionPane.OK_OPTION) {
                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) LeafNodePopupMenu.this.leaf.getParent();
                    boolean deleted;
                    if (parent.getUserObject().equals(PluginConstants.MAIL_TEMPLTAE_CONSTANT)) {
                        deleted = mailTemplateManagerService.delete((String) LeafNodePopupMenu.this.leaf.getUserObject());
                    } else {
                        deleted = reportTemplateManagerService.delete((String) LeafNodePopupMenu.this.leaf.getUserObject());
                    }
                    if (deleted) {
                        LeafNodePopupMenu.this.leaf.removeFromParent();
                        treeModel.reload(parent);
                    } else {
                        JOptionPane.showMessageDialog(null, "Error while deleting "
                                + "new element", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
    }

}
