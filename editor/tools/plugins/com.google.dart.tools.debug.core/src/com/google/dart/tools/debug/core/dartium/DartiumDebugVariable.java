/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.debug.core.dartium;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;
import com.google.dart.tools.debug.core.webkit.WebkitPropertyDescriptor;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

/**
 * The IVariable implementation of the Dartium Debug Element
 */
public class DartiumDebugVariable extends DartiumDebugElement implements IVariable {
  private WebkitPropertyDescriptor descriptor;

  private DartiumDebugVariable parent;
  private DartiumDebugValue value;
  private boolean isThisObject;

  /**
   * Create a new Dartium Debug Variable
   * 
   * @param target
   * @param descriptor
   */
  public DartiumDebugVariable(DartiumDebugTarget target, WebkitPropertyDescriptor descriptor) {
    this(target, descriptor, false);
  }

  /**
   * Create a new Dartium Debug Variable
   * 
   * @param target
   * @param descriptor
   * @param isThisObject
   */
  public DartiumDebugVariable(DartiumDebugTarget target, WebkitPropertyDescriptor descriptor,
      boolean isThisObject) {
    super(target);

    this.descriptor = descriptor;
    this.isThisObject = isThisObject;
  }

  /**
   * @return a user-consumable string for the variable name
   */
  public String getDisplayName() {
    if (isListMember()) {
      return "[" + getName() + "]";
    }

    return getName();
  }

  @Override
  public String getName() {
    return descriptor.getName();
  }

  @Override
  public String getReferenceTypeName() throws DebugException {
    return getValue().getReferenceTypeName();
  }

  @Override
  public IValue getValue() throws DebugException {
    if (value == null) {
      value = new DartiumDebugValue(getTarget(), this, descriptor.getValue());
    }

    return value;
  }

  @Override
  public boolean hasValueChanged() throws DebugException {
    // TODO(keertip): check to see if value has changed

    return false;
  }

  public boolean isListValue() {
    return value.isList();
  }

  public boolean isPrimitiveValue() {
    return value.isPrimitive();
  }

  public boolean isThisObject() {
    return isThisObject;
  }

  @Override
  public void setValue(IValue value) throws DebugException {
    setValue(value.getValueString());
  }

  @Override
  public void setValue(String expression) throws DebugException {
    if (DartDebugCorePlugin.VM_SUPPORTS_VALUE_MODIFICATION) {
      // TODO(devoncarew):

      System.out.println("change: " + expression);
    }
  }

  @Override
  public boolean supportsValueModification() {
    if (DartDebugCorePlugin.VM_SUPPORTS_VALUE_MODIFICATION) {
      return descriptor.isWritable() && descriptor.getValue().isPrimitive();
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return descriptor.toString();
  }

  @Override
  public boolean verifyValue(IValue value) throws DebugException {
    return verifyValue(value.getValueString());
  }

  @Override
  public boolean verifyValue(String expression) throws DebugException {
    // TODO(devoncarew): do verification for numbers

    return true;
  }

  protected void setParent(DartiumDebugVariable parent) {
    this.parent = parent;
  }

  private boolean isListMember() {
    if (parent != null && parent.isListValue()) {
      return true;
    } else {
      return false;
    }
  }

}
