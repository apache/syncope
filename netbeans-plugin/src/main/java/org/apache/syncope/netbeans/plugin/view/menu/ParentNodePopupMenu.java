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
import org.apache.syncope.common.lib.to.MailTemplateTO;
import org.apache.syncope.common.lib.to.ReportTemplateTO;
import org.apache.syncope.netbeans.plugin.connector.ResourceConnector;
import org.apache.syncope.netbeans.plugin.constants.PluginConstants;
import org.apache.syncope.netbeans.plugin.service.MailTemplateManagerService;
import org.apache.syncope.netbeans.plugin.service.ReportTemplateManagerService;

/**
 *
 * @author nuwan
 */
public class ParentNodePopupMenu extends JPopupMenu{

    private JMenuItem newItem;
    private MailTemplateManagerService mailTemplateManagerService;
    private ReportTemplateManagerService reportTemplateManagerService;
    private DefaultMutableTreeNode parent;
    private JTree tree;
    private DefaultTreeModel treeModel;
    
    public ParentNodePopupMenu(DefaultMutableTreeNode parent,JTree tree) {
        this.parent = parent;
        this.tree = tree;
        treeModel = (DefaultTreeModel) this.tree.getModel();
        newItem = new JMenuItem("New");
        add(newItem);
        mailTemplateManagerService = ResourceConnector.getMailTemplateManagerService();
        reportTemplateManagerService = ResourceConnector.getReportTemplateManagerService();
        
        newItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String name = JOptionPane.showInputDialog("Enter Name");
                boolean added;
                if(ParentNodePopupMenu.this.parent.getUserObject().equals(PluginConstants.MAIL_TEMPLTAE_CONSTANT)){
                    MailTemplateTO mailTemplate = new MailTemplateTO();
                    mailTemplate.setKey(name);
                    added = mailTemplateManagerService.create(mailTemplate);
                }else{
                    ReportTemplateTO reportTemplate = new ReportTemplateTO();
                    reportTemplate.setKey(name);
                    added = reportTemplateManagerService.create(reportTemplate);
                }
                
                if(added){
                    ParentNodePopupMenu.this.parent.add(new DefaultMutableTreeNode(name));
                    ParentNodePopupMenu.this.treeModel.reload(ParentNodePopupMenu.this.parent);
                }else{
                    JOptionPane.showMessageDialog(null, "Error while creating "
                            + "new element", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
    
}
