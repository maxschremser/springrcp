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
package org.springframework.richclient.wizard;

import org.springframework.richclient.core.Guarded;
import org.springframework.richclient.dialog.DialogPage;

public interface WizardPage extends DialogPage, Guarded {

    /**
     * Returns this page's name.
     * 
     * @return the name of this page
     */
    public String getId();

    /**
     * Returns the wizard page that would to be shown if the user was to press
     * the Next button.
     * 
     * @return the next wizard page, or <code>null</code> if none
     */
    public WizardPage getNextPage();

    /**
     * Returns the wizard page that would to be shown if the user was to press
     * the Back button.
     * 
     * @return the previous wizard page, or <code>null</code> if none
     */
    public WizardPage getPreviousPage();

    /**
     * Sets the wizard page that would typically be shown if the user was to
     * press the Back button.
     * <p>
     * This method is called by the container.
     * </p>
     * 
     * @param page
     *            the previous wizard page
     */
    public void setPreviousPage(WizardPage page);

    /**
     * Returns the wizard that hosts this wizard page.
     * 
     * @return the wizard, or <code>null</code> if this page has not been
     *         added to any wizard
     * @see #setWizard
     */
    public Wizard getWizard();

    /**
     * Returns whether this page is complete or not.
     * <p>
     * This information is typically used by the wizard to decide when it is
     * okay to finish.
     * </p>
     * 
     * @return <code>true</code> if this page is complete, and
     *         <code>false</code> otherwise
     */
    public boolean isPageComplete();

    /**
     * Returns whether the next page could be displayed.
     * 
     * @return <code>true</code> if the next page could be displayed, and
     *         <code>false</code> otherwise
     */
    public boolean canFlipToNextPage();

    /**
     * Sets the wizard that hosts this wizard page. Once established, a page's
     * wizard cannot be changed to a different wizard.
     * 
     * @param newWizard
     *            the wizard
     * @see #getWizard
     */
    public void setWizard(Wizard newWizard);

    public void onAboutToShow();
}