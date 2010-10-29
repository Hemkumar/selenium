/*
Copyright 2007-2010 WebDriver committers
Copyright 2007-2010 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.openqa.selenium.interactions;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.interactions.BaseAction;

/**
 * Presses the left mouse button without releasing it.
 *
 */
public class ClickAndHoldAction extends BaseAction implements Action {
  public ClickAndHoldAction(WebDriver parent, WebElement onElement) {
    super(parent, onElement);
  }

  public void perform() {
    getMouse().mouseDown(onElement);
  }
}
