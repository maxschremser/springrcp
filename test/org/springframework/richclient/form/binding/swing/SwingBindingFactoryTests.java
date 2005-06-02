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
package org.springframework.richclient.form.binding.swing;

import java.util.Collections;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import junit.framework.TestCase;

import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.PropertyComparator;
import org.springframework.binding.support.TestBean;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.richclient.application.Application;
import org.springframework.richclient.application.config.DefaultApplicationLifecycleAdvisor;
import org.springframework.richclient.application.support.DefaultPropertyEditorRegistry;
import org.springframework.richclient.forms.FormModelHelper;
import org.springframework.richclient.list.BeanPropertyValueListRenderer;

/**
 * @author Oliver Hutchison
 */
public class SwingBindingFactoryTests extends TestCase {

    static {
        Application application = new Application(new DefaultApplicationLifecycleAdvisor());
        Application.services().setPropertyEditorRegistry(new DefaultPropertyEditorRegistry());
        Application.services().setApplicationContext(new StaticApplicationContext());
    }

    private SwingBindingFactory sbf;

    public void setUp() {
        sbf = new SwingBindingFactory(FormModelHelper.createFormModel(new TestBean()));
        sbf.setBinderSelectionStrategy(new TestingBinderSelectionStrategy());
    }

    public void testSwingBindingFactory() {
        try {
            new SwingBindingFactory(null);
            fail("allowed null form model");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testCreateBoundTextField() {
        TestableBinding b = (TestableBinding)sbf.createBoundTextField("name");
        assertBindingProperties(b, JTextField.class, null, "name");
        assertEquals(b.getContext(), Collections.EMPTY_MAP);
    }

    public void testCreateBoundCheckBox() {
        TestableBinding b = (TestableBinding)sbf.createBoundCheckBox("name");
        assertBindingProperties(b, JCheckBox.class, null, "name");
        assertEquals(b.getContext(), Collections.EMPTY_MAP);
    }

    public void testCreateBoundComboBoxString() {
        TestableBinding b = (TestableBinding)sbf.createBoundComboBox("name");
        assertBindingProperties(b, JComboBox.class, null, "name");
        assertEquals(b.getContext(), Collections.EMPTY_MAP);
    }

    public void testCreateBoundComboBoxStringObjectArray() {
        Object[] items = new Object[0];
        TestableBinding b = (TestableBinding)sbf.createBoundComboBox("name", items);
        assertBindingProperties(b, JComboBox.class, null, "name");
        assertEquals(b.getContext().size(), 1);
        assertEquals(b.getContext().get(ComboBoxBinder.SELECTABLE_ITEMS_KEY), items);
    }

    public void testCreateBoundComboBoxStringStringString() {
        TestableBinding b = (TestableBinding)sbf.createBoundComboBox("name", "listProperty", "displayPropety");
        assertBindingProperties(b, JComboBox.class, null, "name");
        assertEquals(b.getContext().size(), 3);        
        assertEquals(b.getContext().get(ComboBoxBinder.SELECTABLE_ITEMS_HOLDER_KEY), sbf.getFormModel()
                .getValueModel("listProperty"));
        assertEquals(
                ((BeanPropertyValueListRenderer)b.getContext().get(ComboBoxBinder.RENDERER_KEY)).getPropertyName(),
                "displayPropety");
        assertEquals(
                ((PropertyComparator)b.getContext().get(ComboBoxBinder.COMPARATOR_KEY)).getProperty(),
                "displayPropety");
        
        try {
            b = (TestableBinding)sbf.createBoundComboBox("name", "someUnknownProperty", "displayPropety");
            fail("cant use an unknown property to provide the selectable items");
        } catch(InvalidPropertyException e) {
            // expected
        }
    }

    private void assertBindingProperties(TestableBinding b, Class controlType, JComponent control, String property) {
        assertEquals(b.getControlType(), controlType);
        assertEquals(b.getControl(), control);
        assertEquals(b.getFormModel(), sbf.getFormModel());
        assertEquals(b.getProperty(), property);
    }
}