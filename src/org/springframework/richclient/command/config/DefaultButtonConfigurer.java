/*
 * Copyright 2002-2004 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.richclient.command.config;

import javax.swing.AbstractButton;

import org.springframework.util.Assert;

/**
 * @author Keith Donald
 */
public class DefaultButtonConfigurer implements CommandButtonConfigurer {
    public void configure(CommandFaceDescriptor face, AbstractButton button) {
        Assert.notNull(face, "The command face descriptor cannot be null.");
        if (button == null) { return; }
        face.getButtonLabelInfo().configure(button);
        if (face.getButtonIconInfo() != null) {
            face.getButtonIconInfo().configure(button);
        }
        button.setToolTipText(face.getCaption());
    }
}