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
package org.springframework.richclient.dialog;

import org.springframework.rules.reporting.Severity;

/**
 * Interface to be implemented by object capable of receiving messages to the
 * user.
 * 
 * @author Keith Donald
 */
public interface MessageAreaModel {

    /**
     * Set the message text. If the message line currently displays an error,
     * the message is stored and will be shown after a call to
     * setErrorMessage(null)
     */
    public void setMessage(String newMessage);

    /**
     * Display the given error message. The currently displayed message is saved
     * and will be redisplayed when the error message is set to
     * <code>null</code>.
     * 
     * @param errorMessage
     *            the errorMessage to display or <code>null</code> to clear
     */
    public void setErrorMessage(String errorMessage);

    /**
     * Display the given error message of the provided severity.
     * 
     * @param newMessage
     * @param severity
     */
    public void setMessage(String newMessage, Severity severity);

    /**
     * Adds the specified message listener to receive events from this
     * MessageReceiver whenever messages are updated.
     * 
     * @param listener
     *            the MessageListener to be added
     */
    public void addMessageAreaChangeListener(MessageAreaChangeListener messageListener);

    /**
     * Removes the specified message listener
     * 
     * @param listener
     *            the MessageListener to be removed
     */
    public void removeMessageAreaChangeListener(MessageAreaChangeListener messageListener);

}