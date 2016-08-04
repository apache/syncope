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
package org.apache.syncope.ide.eclipse.plugin.editors;

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.syncope.common.lib.types.ReportTemplateFormat;
import org.apache.syncope.ide.eclipse.plugin.Activator;
import org.apache.syncope.ide.eclipse.plugin.views.SyncopeView;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.wst.sse.ui.StructuredTextEditor;

public class TemplateEditor extends MultiPageEditorPart implements IResourceChangeListener {

    public static final String ID = "org.apache.syncope.ide.eclipse.plugin.editors.TemplateEditor";
    private static final String SAVE_TEMPLATE_LABEL = "Saving Template";
    private static final String ERROR_NESTED_EDITOR = "Error creating nested text editor";
    private static final String ERROR_INCORRECT_INPUT = "Wrong Input";
    private TextEditor editor;
    private TemplateEditorInput input;
    private String[] inputStringList;
    private String[] titleList;
    private String[] tooltipList;

    void createPage(final String inputString, final String title, final String tooltip) {
        try {
            if (title.equals(SyncopeView.TEMPLATE_FORMAT_HTML)) {
                editor = new HTMLEditor();
            } else {
                editor = new StructuredTextEditor(); 
            }
            int index = addPage(editor, (IEditorInput) new TemplateEditorInput(inputString, title, tooltip));
            setPageText(index, editor.getTitle());
        } catch (final PartInitException e) {
            ErrorDialog.openError(
                getSite().getShell(), ERROR_NESTED_EDITOR, null, e.getStatus());
        }
    }

    protected void createPages() {
        for (int i = 0; i < inputStringList.length; i++) {
            createPage(inputStringList[i], titleList[i], tooltipList[i]);
        }
    }

    public void dispose() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        super.dispose();
    }

    public void doSave(final IProgressMonitor monitor) {
        final ITextEditor ite = (ITextEditor) getActiveEditor();
        final String content = ite.getDocumentProvider().getDocument(ite.getEditorInput()).get();
        Job saveJob = new Job(SAVE_TEMPLATE_LABEL) {
            @Override
            protected IStatus run(final IProgressMonitor arg0) {
                try {
                    if (ite.getTitle().equals(SyncopeView.TEMPLATE_FORMAT_HTML)) {
                        SyncopeView.setMailTemplateContent(ite.getTitleToolTip(),
                                MailTemplateFormat.HTML, content);
                    } else if (ite.getTitle().equals(SyncopeView.TEMPLATE_FORMAT_TEXT)) {
                        SyncopeView.setMailTemplateContent(ite.getTitleToolTip(),
                                MailTemplateFormat.TEXT, content);
                    } else if (ite.getTitle().equals(SyncopeView.TEMPLATE_FORMAT_CSV)) {
                        SyncopeView.setReportTemplateContent(ite.getTitleToolTip(),
                                ReportTemplateFormat.CSV, content);
                    } else if (ite.getTitle().equals(SyncopeView.TEMPLATE_FORMAT_XSL_FO)) {
                        SyncopeView.setReportTemplateContent(ite.getTitleToolTip(),
                                ReportTemplateFormat.FO, content);
                    } else if (ite.getTitle().equals(SyncopeView.TEMPLATE_FORMAT_XSL_HTML)) {
                        SyncopeView.setReportTemplateContent(ite.getTitleToolTip(),
                                ReportTemplateFormat.HTML, content);
                    } else {
                        throw new Exception("Not a valid editor title");
                    }
                } catch (final SyncopeClientException e) {
                    e.printStackTrace();
                    if (ClientExceptionType.NotFound.equals(e.getType())) {
                        /*
                         * If a deleted template is being edited
                         * close editor and display error
                         */
                        Display.getDefault().syncExec(new Runnable() {
                            @Override
                            public void run() {
                                TemplateEditor.this.getSite().getPage().closeEditor(
                                        TemplateEditor.this, false);
                            }
                        });
                        return new org.eclipse.core.runtime.Status(
                                org.eclipse.core.runtime.Status.ERROR, Activator.PLUGIN_ID,
                                "Template No longer exists");
                    }
                }  catch (final Exception e) {
                    e.printStackTrace();
                } finally {
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            ite.doSave(monitor);
                        }
                    });
                }
                if (monitor.isCanceled()) {
                    return org.eclipse.core.runtime.Status.CANCEL_STATUS;
                }
                return org.eclipse.core.runtime.Status.OK_STATUS;
            }
        };
        saveJob.setUser(true);
        saveJob.schedule();
    }

    public void doSaveAs() {
        getActiveEditor().doSaveAs();
    }
    public void gotoMarker(final IMarker marker) {
        setActivePage(0);
        IDE.gotoMarker(getEditor(0), marker);
    }
    public void init(final IEditorSite site, final IEditorInput editorInput)
        throws PartInitException {
        if (!(editorInput instanceof TemplateEditorInput)) {
            throw new RuntimeException(ERROR_INCORRECT_INPUT);
        }
        this.input = (TemplateEditorInput) editorInput;

        this.inputStringList = this.input.getInputStringList();
        this.titleList = this.input.getTitleList();
        this.tooltipList = this.input.getTooltipList();

        setSite(site);
        setInput(input);
        setPartName(this.tooltipList[0]);
    }

    public boolean isSaveAsAllowed() {
        return true;
    }

    protected void pageChange(final int newPageIndex) {
        super.pageChange(newPageIndex);
    }
    
    public ITextEditor getActiveHTMLEditor() {
        final ITextEditor ite = (ITextEditor) getActiveEditor();
        if (ite.getTitle().equals(SyncopeView.TEMPLATE_FORMAT_HTML)) {
            return (ITextEditor) getActiveEditor();
        } else {
            return null;
        }
    }

    public void resourceChanged(final IResourceChangeEvent event) {
        if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    IWorkbenchPage[] pages = getSite().getWorkbenchWindow().getPages();
                    for (int i = 0; i < pages.length; i++) {
                        if (((FileEditorInput) editor.getEditorInput()).getFile().getProject()
                                .equals(event.getResource())) {
                            IEditorPart editorPart = pages[i].findEditor(editor.getEditorInput());
                            pages[i].closeEditor(editorPart, true);
                        }
                    }
                }
            });
        }
    }
}
