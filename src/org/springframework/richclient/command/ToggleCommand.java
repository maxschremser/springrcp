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
package org.springframework.richclient.command;

import java.util.Iterator;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JRadioButton;
import javax.swing.RootPaneContainer;

import org.springframework.richclient.command.config.CommandButtonConfigurer;
import org.springframework.richclient.command.config.CommandFaceDescriptor;
import org.springframework.richclient.factory.ButtonFactory;
import org.springframework.richclient.factory.MenuFactory;
import org.springframework.util.Assert;

public abstract class ToggleCommand extends ActionCommand {

    private static final String SELECTED_PROPERTY = "selected";

    private boolean selected;

    private ExclusiveCommandGroupSelectionController exclusiveController;

    public ToggleCommand() {
    }

    public ToggleCommand(String commandId) {
        super(commandId);
    }

    public ToggleCommand(String id, CommandFaceDescriptor face) {
        super(id, face);
    }

    public ToggleCommand(String id, String encodedLabel) {
        super(id, encodedLabel);
    }

    public ToggleCommand(String id, String encodedLabel, Icon icon, String caption) {
        super(id, encodedLabel, icon, caption);
    }

    public void setExclusiveController(ExclusiveCommandGroupSelectionController exclusiveController) {
        this.exclusiveController = exclusiveController;
    }

    public boolean isExclusiveGroupMember() {
        return exclusiveController != null;
    }

    public JMenuItem createMenuItem(String faceDescriptorId, MenuFactory factory,
            CommandButtonConfigurer buttonConfigurer) {
        JMenuItem menuItem;
        if (isExclusiveGroupMember()) {
            menuItem = factory.createRadioButtonMenuItem();
        }
        else {
            menuItem = factory.createCheckBoxMenuItem();
        }
        attach(menuItem, faceDescriptorId, buttonConfigurer);
        return menuItem;
    }

    public AbstractButton createButton(String faceDescriptorId, ButtonFactory buttonFactory,
            CommandButtonConfigurer configurer) {
        AbstractButton button = buttonFactory.createToggleButton();
        attach(button, faceDescriptorId, configurer);
        return button;
    }

    public final JCheckBox createCheckBox() {
        return createCheckBox(getDefaultFaceDescriptorId(), getButtonFactory(), getDefaultButtonConfigurer());
    }

    public final JCheckBox createCheckBox(ButtonFactory buttonFactory) {
        return createCheckBox(getDefaultFaceDescriptorId(), buttonFactory, getDefaultButtonConfigurer());
    }

    public final JCheckBox createCheckBox(String faceDescriptorId, ButtonFactory buttonFactory) {
        return createCheckBox(faceDescriptorId, buttonFactory, getDefaultButtonConfigurer());
    }

    public JCheckBox createCheckBox(String faceDescriptorId, ButtonFactory buttonFactory,
            CommandButtonConfigurer configurer) {
        JCheckBox checkBox = buttonFactory.createCheckBox();
        attach(checkBox, configurer);
        return checkBox;
    }

    public final JRadioButton createRadioButton() {
        return createRadioButton(getDefaultFaceDescriptorId(), getButtonFactory(), getDefaultButtonConfigurer());
    }

    public final JRadioButton createRadioButton(ButtonFactory buttonFactory) {
        return createRadioButton(getDefaultFaceDescriptorId(), buttonFactory, getDefaultButtonConfigurer());
    }

    public final JRadioButton createRadioButton(String faceDescriptorId, ButtonFactory buttonFactory) {
        return createRadioButton(faceDescriptorId, buttonFactory, getDefaultButtonConfigurer());
    }

    public JRadioButton createRadioButton(String faceDescriptorId, ButtonFactory buttonFactory,
            CommandButtonConfigurer configurer) {
        Assert.state(isExclusiveGroupMember(),
                "Can't create radio buttons for toggle commands that aren't members of an exclusive group");
        JRadioButton radioButton = buttonFactory.createRadioButton();
        attach(radioButton, faceDescriptorId, configurer);
        return radioButton;
    }

    protected void onButtonAttached(AbstractButton button) {
        super.onButtonAttached(button);
        button.setSelected(selected);
    }

    public boolean isSelected() {
        return this.selected;
    }

    public final void setSelected(boolean selected) {
        if (isExclusiveGroupMember()) {
            boolean oldState = isSelected();

            exclusiveController.handleSelectionRequest(this, selected);
            // set back button state if controller didn't change this command;
            // needed b/c of natural button check box toggling in swing
            if (oldState == isSelected()) {
                Iterator iter = buttonIterator();
                while (iter.hasNext()) {
                    AbstractButton button = (AbstractButton)iter.next();
                    button.setSelected(isSelected());
                }
            }
        }
        else {
            requestSetSelection(selected);
        }
    }

    boolean requestSetSelection(boolean selected) {
        boolean previousState = isSelected();

        if (previousState != selected) {
            this.selected = onSelection(selected);
            if (logger.isDebugEnabled()) {
                logger.debug("Toggle command selection returned '" + this.selected + "'");
            }
        }

        // we must always update toggle buttons
        Iterator it = buttonIterator();
        if (logger.isDebugEnabled()) {
            logger.debug("Updating all attached toggle buttons to '" + isSelected() + "'");
        }
        while (it.hasNext()) {
            AbstractButton button = (AbstractButton)it.next();
            button.setSelected(isSelected());
        }

        if (previousState != isSelected()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Selection changed; firing property change event");
            }
            firePropertyChange(SELECTED_PROPERTY, previousState, isSelected());
        }

        return isSelected();
    }

    protected void doExecuteCommand() {
        setSelected(!isSelected());
    }

    protected abstract boolean onSelection(boolean selected);

    public void requestDefaultIn(RootPaneContainer container) {
        throw new UnsupportedOperationException();
    }

}