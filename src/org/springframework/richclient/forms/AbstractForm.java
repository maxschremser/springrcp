/*
 * Copyright 2002-2004 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.richclient.forms;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.springframework.binding.form.FormModel;
import org.springframework.binding.form.NestingFormModel;
import org.springframework.binding.form.ValidationListener;
import org.springframework.binding.form.support.CommitListenerAdapter;
import org.springframework.binding.value.ValueChangeListener;
import org.springframework.binding.value.ValueModel;
import org.springframework.richclient.command.ActionCommand;
import org.springframework.richclient.core.Guarded;
import org.springframework.richclient.dialog.MessageReceiver;
import org.springframework.richclient.factory.AbstractControlFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author Keith Donald
 */
public abstract class AbstractForm extends AbstractControlFactory implements
        Form {
    private String formId;

    private SwingFormModel formModel;

    private NestingFormModel parentFormModel;

    private Guarded formEnabledGuarded;

    private JButton lastDefaultButton;

    private PropertyChangeListener formEnabledChangeHandler;

    private ActionCommand newFormObjectCommand;

    private ActionCommand commitCommand;

    private ActionCommand revertCommand;

    private boolean editingNewFormObject;

    private List editableFormObjects;

    private ValueModel editingFormObjectIndexHolder;

    protected AbstractForm() {

    }

    protected AbstractForm(String formId) {
        this.formId = formId;
    }

    protected AbstractForm(SwingFormModel pageFormModel) {
        this(pageFormModel, null);
    }

    protected AbstractForm(NestingFormModel parentFormModel, String formId) {
        this(SwingFormModel.createChildPageFormModel(parentFormModel, formId));
        this.formId = formId;
    }

    protected AbstractForm(NestingFormModel parentFormModel, String formId,
            String childFormObjectPropertyPath) {
        this.parentFormModel = parentFormModel;
        setFormModel(SwingFormModel.createChildPageFormModel(parentFormModel,
                formId, childFormObjectPropertyPath));
        this.formId = formId;
    }

    protected AbstractForm(NestingFormModel parentFormModel, String formId,
            ValueModel childFormObjectHolder) {
        this.parentFormModel = parentFormModel;
        setFormModel(SwingFormModel.createChildPageFormModel(parentFormModel,
                formId, childFormObjectHolder));
        this.formId = formId;
    }

    protected AbstractForm(FormModel formModel, String formId) {
        this.formId = formId;
        if (formModel instanceof NestingFormModel) {
            this.parentFormModel = (NestingFormModel)formModel;
            setFormModel(SwingFormModel.createChildPageFormModel(
                    this.parentFormModel, formId));
        }
        else if (formModel instanceof SwingFormModel) {
            setFormModel((SwingFormModel)formModel);
        }
        else {
            throw new IllegalArgumentException(
                    "Unsupported form model implementation " + formModel);
        }
    }

    public String getId() {
        return formId;
    }

    public SwingFormModel getFormModel() {
        return formModel;
    }

    protected void setFormModel(SwingFormModel formModel) {
        Assert.notNull(formModel);
        this.formModel = formModel;
    }

    protected NestingFormModel getParent() {
        return this.parentFormModel;
    }

    public Object getFormObject() {
        return formModel.getFormObject();
    }

    public void setFormObject(Object formObject) {
        formModel.setFormObject(formObject);
    }

    public Object getValue(String formProperty) {
        return formModel.getValue(formProperty);
    }

    public ValueModel getValueModel(String formProperty) {
        ValueModel valueModel = formModel.getValueModel(formProperty);
        if (valueModel == null) {
            logger.warn("A value model for property '" + formProperty
                    + "' could not be found.  Typo?");
        }
        return valueModel;
    }

    public boolean isEnabled() {
        return this.formModel.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        this.formModel.setEnabled(enabled);
    }

    public void addValidationListener(ValidationListener listener) {
        formModel.addValidationListener(listener);
    }

    public void removeValidationListener(ValidationListener listener) {
        formModel.removeValidationListener(listener);
    }

    public ValidationListener newSingleLineResultsReporter(Guarded guarded,
            MessageReceiver messageAreaPane) {
        return getFormModel().createSingleLineResultsReporter(guarded,
                messageAreaPane);
    }

    public void addFormObjectChangeListener(ValueChangeListener listener) {
        formModel.addFormObjectChangeListener(listener);
    }

    public void removeFormObjectChangeListener(ValueChangeListener listener) {
        formModel.addFormObjectChangeListener(listener);
    }

    public void addFormValueChangeListener(String formPropertyPath,
            ValueChangeListener listener) {
        getFormModel().addFormValueChangeListener(formPropertyPath, listener);
    }

    public void removeFormValueChangeListener(String formPropertyPath,
            ValueChangeListener listener) {
        getFormModel()
                .removeFormValueChangeListener(formPropertyPath, listener);
    }

    public boolean hasErrors() {
        return formModel.getHasErrors();
    }

    public void commit() {
        formModel.commit();
    }

    public void revert() {
        formModel.revert();
    }

    public void reset() {
        formModel.reset();
    }

    protected void setEditableFormObjects(List editableFormObjects) {
        this.editableFormObjects = editableFormObjects;
    }

    protected void setEditingFormObjectIndexHolder(ValueModel valueModel) {
        this.editingFormObjectIndexHolder = valueModel;
        this.editingFormObjectIndexHolder
                .addValueChangeListener(new EditingFormObjectSetter());
    }

    private class EditingFormObjectSetter implements ValueChangeListener {
        public void valueChanged() {
            int selectionIndex = getEditingFormObjectIndex();
            if (selectionIndex == -1) {
                reset();
                setEnabled(false);
            }
            else {
                setFormObject(getEditableFormObject(selectionIndex));
                setEnabled(true);
            }
        }
    }

    protected int getEditingFormObjectIndex() {
        return ((Integer)editingFormObjectIndexHolder.getValue()).intValue();
    }

    protected Object getEditableFormObject(int selectionIndex) {
        return editableFormObjects.get(selectionIndex);
    }

    protected void formObjectEditCommitted(Object formObject,
            boolean newFormObject) {
        if (newFormObject) {
            editableFormObjects.add(formObject);
        }
        else {
            editableFormObjects.set(getEditingFormObjectIndex(), formObject);
        }
    }

    protected void setFormEnabledGuarded(Guarded formEnabledGuarded) {
        this.formEnabledGuarded = formEnabledGuarded;
        updateFormEnabledGuarded();
    }

    protected JButton getDefaultButton() {
        if (isControlCreated()) {
            return SwingUtilities.getRootPane(getControl()).getDefaultButton();
        }
        else {
            return null;
        }
    }

    protected void setDefaultButton(JButton button) {
        SwingUtilities.getRootPane(getControl()).setDefaultButton(button);
    }

    protected final JComponent createControl() {
        initStandardLocalFormCommands();
        JComponent formControl = createFormControl();
        this.formEnabledChangeHandler = new FormEnabledPropertyChangeHandler();
        getFormModel().addPropertyChangeListener(FormModel.ENABLED_PROPERTY,
                formEnabledChangeHandler);
        addFormObjectChangeListener(new FormEnabledStateController());
        ActionCommand commitCommand = getCommitCommand();
        if (getCommitCommand() != null) {
            attachFormErrorGuard(getCommitCommand());
            getFormModel().addCommitListener(new PostCommitHandler());
        }
        return formControl;
    }

    private void initStandardLocalFormCommands() {
        initNewFormObjectCommand();
        initCommitCommand();
        initRevertCommand();
    }

    protected abstract JComponent createFormControl();

    private class FormEnabledStateController implements ValueChangeListener {
        public FormEnabledStateController() {
            valueChanged();
        }

        public void valueChanged() {
            setFormModelDefaultEnabledState();
        }
    }

    private class FormEnabledPropertyChangeHandler implements
            PropertyChangeListener {
        public FormEnabledPropertyChangeHandler() {
            handleEnabledChange(getFormModel().isEnabled());
        }

        public void propertyChange(PropertyChangeEvent evt) {
            handleEnabledChange(getFormModel().isEnabled());
        }

        private void handleEnabledChange(boolean enabled) {
            updateFormEnabledGuarded();

            if (enabled) {
                if (getCommitCommand() != null) {
                    if (lastDefaultButton == null) {
                        lastDefaultButton = getDefaultButton();
                    }
                    getCommitCommand().setDefaultButton();
                }
            }
            else {
                if (getCommitCommand() != null) {
                    getCommitCommand().setEnabled(false);
                }
                // set previous default button
                if (lastDefaultButton != null) {
                    setDefaultButton(lastDefaultButton);
                }
            }
        }
    };

    private void updateFormEnabledGuarded() {
        if (this.formEnabledGuarded != null) {
            this.formEnabledGuarded.setEnabled(formModel.isEnabled());
        }
    }

    protected void attachFormErrorGuard(Guarded guarded) {
        FormGuard guard = new FormGuard(getFormModel(), guarded);
        addValidationListener(guard);
    }

    protected ActionCommand getNewFormObjectCommand() {
        return newFormObjectCommand;
    }

    protected ActionCommand getCommitCommand() {
        return commitCommand;
    }

    protected ActionCommand getRevertCommand() {
        return revertCommand;
    }

    /**
     * Set the form's enabled state based on a default policy--specifically,
     * disable if the form object is null or the form object is guarded and is
     * marked as disabled.
     */
    protected void setFormModelDefaultEnabledState() {
        if (getFormObject() == null) {
            getFormModel().setEnabled(false);
        }
        else {
            if (getFormObject() instanceof Guarded) {
                setEnabled(((Guarded)getFormObject()).isEnabled());
            }
        }
    }

    private ActionCommand initNewFormObjectCommand() {
        String commandId = getNewFormObjectFaceConfigurationKey();
        if (!StringUtils.hasText(commandId)) { return null; }

        this.newFormObjectCommand = new ActionCommand(commandId) {
            protected void doExecuteCommand() {
                getFormModel().reset();
                getFormModel().setEnabled(true);
                editingNewFormObject = true;
            }
        };
        return (ActionCommand)getCommandConfigurer().configure(
                newFormObjectCommand);
    }

    protected final JButton createNewFormObjectButton() {
        Assert.notNull(newFormObjectCommand,
                "New form object command has not been created!");
        return (JButton)newFormObjectCommand.createButton();
    }

    protected final JButton createCommitButton() {
        Assert.notNull(commitCommand, "Commit command has not been created!");
        return (JButton)commitCommand.createButton();
    }

    protected String getNewFormObjectFaceConfigurationKey() {
        return "new"
                + StringUtils.capitalize(ClassUtils.getShortName(getFormModel()
                        .getFormObject().getClass()
                        + "Command"));
    }

    protected String getCommitFaceConfigurationKey() {
        return null;
    }

    protected String getRevertFaceConfigurationKey() {
        return null;
    }

    private final ActionCommand initCommitCommand() {
        String commandId = getCommitFaceConfigurationKey();
        if (!StringUtils.hasText(commandId)) { return null; }
        this.commitCommand = new ActionCommand(commandId) {
            protected void doExecuteCommand() {
                getFormModel().commit();
            }
        };
        return (ActionCommand)getCommandConfigurer().configure(commitCommand);
    }

    private final class PostCommitHandler extends CommitListenerAdapter {
        public void postEditCommitted(Object formObject) {
            System.out.println("post edit committed");
            formObjectEditCommitted(formObject, editingNewFormObject);
            setFormObject(null);
            editingNewFormObject = false;
        }
    }

    private final ActionCommand initRevertCommand() {
        String commandId = getRevertFaceConfigurationKey();
        if (!StringUtils.hasText(commandId)) { return null; }
        this.commitCommand = new ActionCommand(commandId) {
            protected void doExecuteCommand() {
                getFormModel().revert();
            }
        };
        return (ActionCommand)getCommandConfigurer().configure(commitCommand);
    }

}