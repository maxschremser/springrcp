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
package org.springframework.richclient.table.renderer;

import java.awt.Component;
import java.text.Format;

import javax.swing.JTable;

/**
 * @author Oliver Hutchison
 */
public class FormatTableCellRenderer extends OptimizedTableCellRenderer {
    
    private Format format;
    
    public FormatTableCellRenderer(Format format) {
        setFormat(format);
    }
    
    protected Format getFormat() {
        return format;
    }
    
    protected void setFormat(Format format) {
        this.format = format;        
    }
    
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        doPrepareRenderer(table, isSelected, hasFocus, row, column);
        if (value != null) {
            setValue(format.format(value));
        }
        else {
            setValue(null);
        }
        return this;
    }
}