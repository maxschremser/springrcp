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
package org.springframework.richclient.text;

import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.undo.UndoManager;

import org.springframework.binding.value.ValueChangeListener;
import org.springframework.binding.value.ValueModel;
import org.springframework.richclient.application.Application;
import org.springframework.richclient.application.ApplicationWindow;
import org.springframework.richclient.command.ActionCommand;
import org.springframework.richclient.command.CommandGroup;
import org.springframework.richclient.command.CommandManager;
import org.springframework.richclient.command.TargetableActionCommand;
import org.springframework.richclient.command.support.AbstractActionCommandExecutor;
import org.springframework.richclient.command.support.DefaultCommandManager;
import org.springframework.richclient.command.support.GlobalCommandIds;

/**
 * Helper class that decorates a <code>JTextComponent</code> with a standard
 * popup menu. Support for undo/redo is also provided.
 * 
 * @author oliverh
 */
public class TextComponentPopup extends MouseAdapter implements FocusListener,
        CaretListener, UndoableEditListener {

    private static final String[] COMMANDS = new String[] {
            GlobalCommandIds.UNDO, GlobalCommandIds.REDO,
            GlobalCommandIds.COPY, GlobalCommandIds.CUT,
            GlobalCommandIds.PASTE, GlobalCommandIds.SELECT_ALL };

    public static void attachPopup(JTextComponent textComponent,
            ValueModel resetUndoHistoryTrigger) {
        new TextComponentPopup(textComponent, resetUndoHistoryTrigger);
    }

    public static void attachPopup(JTextComponent textComponent) {
        new TextComponentPopup(textComponent, null);
    }

    private JTextComponent textComponent;

    private UndoManager undoManager = new UndoManager();

    private ValueModel resetUndoHistoryTrigger;

    private static CommandManager localCommandManager;

    private final UndoCommandExecutor undo = new UndoCommandExecutor();

    private final RedoCommandExecutor redo = new RedoCommandExecutor();

    private final CutCommandExecutor cut = new CutCommandExecutor();

    private final CopyCommandExecutor copy = new CopyCommandExecutor();

    private final PasteCommandExecutor paste = new PasteCommandExecutor();

    private final SelectAllCommandExecutor selectAll = new SelectAllCommandExecutor();

    protected TextComponentPopup(JTextComponent textComponent,
            ValueModel resetUndoHistoryTrigger) {
        this.textComponent = textComponent;
        this.resetUndoHistoryTrigger = resetUndoHistoryTrigger;
        registerListeners();
        registerAccelerators();
    }

    private void registerListeners() {
        textComponent.addMouseListener(this);
        textComponent.addFocusListener(this);
        textComponent.addCaretListener(this);
        textComponent.getDocument().addUndoableEditListener(this);
        if (resetUndoHistoryTrigger != null) {
            resetUndoHistoryTrigger
                    .addValueChangeListener(new ValueChangeListener() {
                        public void valueChanged() {
                            if (Boolean.TRUE.equals(resetUndoHistoryTrigger
                                    .getValue())) {
                                undoManager.discardAllEdits();
                                updateUndoRedoState();
                            }
                        }
                    });
        }
    }

    protected CommandManager getCommandManager() {
        CommandManager commandManager;
        ApplicationWindow appWindow = Application.instance().getActiveWindow();
        if (appWindow == null || appWindow.getCommandManager() == null) {
            if (localCommandManager == null) {
                localCommandManager = new DefaultCommandManager();
            }
            commandManager = localCommandManager;
        }
        else {
            commandManager = appWindow.getCommandManager();
        }
        for (int i = 0; i < COMMANDS.length; i++) {
            if (!commandManager.containsActionCommand(COMMANDS[i])) {
                commandManager.addNewCommand(new TargetableActionCommand(
                        COMMANDS[i], null), COMMANDS[i]);
            }
        }
        return commandManager;
    }

    public void registerAccelerators() {
        CommandManager commandManager = getCommandManager();
        Keymap keymap = textComponent.getKeymap();
        String[] commandIds = new String[] { GlobalCommandIds.UNDO,
                GlobalCommandIds.REDO, };
        for (int i = 0; i < COMMANDS.length; i++) {
            ActionCommand command = commandManager
                    .getActionCommand(COMMANDS[i]);
            keymap.addActionForKeyStroke(command.getAccelerator(), command
                    .getActionAdapter());
        }
    }

    /**
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent evt) {
        maybeShowPopup(evt);
    }

    /**
     * @see java.awt.event.MouseAdapter#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent evt) {
        maybeShowPopup(evt);
    }

    private void maybeShowPopup(MouseEvent evt) {
        if (evt.isPopupTrigger()) {
            createPopup().show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }

    public void caretUpdate(CaretEvent e) {
        updateState();
    }

    public void focusGained(FocusEvent e) {
        updateState();
        registerCommandExecutors();
    }

    public void focusLost(FocusEvent e) {
        if (!e.isTemporary()) {
            unregisterCommandExecutors();
        }
    }

    public void undoableEditHappened(UndoableEditEvent e) {
        undoManager.addEdit(e.getEdit());
        updateUndoRedoState();
    }

    private JPopupMenu createPopup() {
        if (textComponent instanceof JPasswordField) {
            return getPasswordCommandGroup().createPopupMenu();
        }
        else {
            if (isEditable()) {
                return getEditableCommandGroup().createPopupMenu();
            }
            else {
                return getReadOnlyCommandGroup().createPopupMenu();
            }
        }
    }

    private void updateState() {
        boolean hasSelection = textComponent.getSelectionStart() != textComponent
                .getSelectionEnd();
        copy.setEnabled(hasSelection);
        selectAll.setEnabled(textComponent.getDocument().getLength() > 0);
        boolean isEditable = isEditable();
        cut.setEnabled(hasSelection && isEditable);
        boolean canPaste = isEditable
                && textComponent.getTransferHandler().canImport(
                        textComponent,
                        Toolkit.getDefaultToolkit().getSystemClipboard()
                                .getContents(textComponent)
                                .getTransferDataFlavors());
        paste.setEnabled(canPaste);
        updateUndoRedoState();
    }

    private void updateUndoRedoState() {
        undo.setEnabled(undoManager.canUndo());
        redo.setEnabled(undoManager.canRedo());
    }

    private boolean isEditable() {
        return !(textComponent instanceof JPasswordField)
                && textComponent.isEnabled() && textComponent.isEditable();
    }

    protected CommandGroup getEditableCommandGroup() {
        CommandGroup editGroup = getCommandManager().getCommandGroup(
                "textEditMenu");
        if (editGroup == null) {
            editGroup = getCommandManager().createCommandGroup(
                    "textEditMenu",
                    new Object[] { GlobalCommandIds.UNDO,
                            GlobalCommandIds.REDO, "separator",
                            GlobalCommandIds.CUT, GlobalCommandIds.COPY,
                            GlobalCommandIds.PASTE, "separator",
                            GlobalCommandIds.SELECT_ALL });
        }
        return editGroup;
    }

    protected CommandGroup getPasswordCommandGroup() {
        CommandGroup passwordGroup = getCommandManager().getCommandGroup(
                "passwordTextEditMenu");
        if (passwordGroup == null) {
            passwordGroup = getCommandManager()
                    .createCommandGroup(
                            "passwordTextEditMenu",
                            new Object[] { GlobalCommandIds.UNDO,
                                    GlobalCommandIds.REDO });
        }
        return passwordGroup;
    }

    protected CommandGroup getReadOnlyCommandGroup() {
        CommandGroup readOnlyGroup = getCommandManager().getCommandGroup(
                "readOnlyTextEditMenu");
        if (readOnlyGroup == null) {
            readOnlyGroup = getCommandManager().createCommandGroup(
                    "readOnlyTextEditMenu",
                    new Object[] { GlobalCommandIds.COPY, "separator",
                            GlobalCommandIds.SELECT_ALL });
        }
        return readOnlyGroup;
    }

    private void registerCommandExecutors() {
        CommandManager commandManager = getCommandManager();
        commandManager.setTargetableActionCommandExecutor(
                GlobalCommandIds.UNDO, undo);
        commandManager.setTargetableActionCommandExecutor(
                GlobalCommandIds.REDO, redo);
        commandManager.setTargetableActionCommandExecutor(GlobalCommandIds.CUT,
                cut);
        commandManager.setTargetableActionCommandExecutor(
                GlobalCommandIds.COPY, copy);
        commandManager.setTargetableActionCommandExecutor(
                GlobalCommandIds.PASTE, paste);
        commandManager.setTargetableActionCommandExecutor(
                GlobalCommandIds.SELECT_ALL, selectAll);
    }

    private void unregisterCommandExecutors() {
        CommandManager commandManager = getCommandManager();
        commandManager.setTargetableActionCommandExecutor(
                GlobalCommandIds.UNDO, null);
        commandManager.setTargetableActionCommandExecutor(
                GlobalCommandIds.REDO, null);
        commandManager.setTargetableActionCommandExecutor(GlobalCommandIds.CUT,
                null);
        commandManager.setTargetableActionCommandExecutor(
                GlobalCommandIds.COPY, null);
        commandManager.setTargetableActionCommandExecutor(
                GlobalCommandIds.PASTE, null);
        commandManager.setTargetableActionCommandExecutor(
                GlobalCommandIds.SELECT_ALL, null);
    }

    private class UndoCommandExecutor extends AbstractActionCommandExecutor {
        public void execute() {
            undoManager.undo();
        }
    }

    private class RedoCommandExecutor extends AbstractActionCommandExecutor {
        public void execute() {
            undoManager.redo();
        }
    }

    private class CutCommandExecutor extends AbstractActionCommandExecutor {
        public void execute() {
            textComponent.cut();
        }
    }

    private class CopyCommandExecutor extends AbstractActionCommandExecutor {
        public void execute() {
            textComponent.copy();
        }
    }

    private class PasteCommandExecutor extends AbstractActionCommandExecutor {
        public void execute() {
            textComponent.paste();
        }
    }

    private class SelectAllCommandExecutor extends
            AbstractActionCommandExecutor {
        public void execute() {
            textComponent.selectAll();
        }
    }
}