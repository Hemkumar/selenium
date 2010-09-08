/*
Copyright 2010 WebDriver committers
Copyright 2010 Google Inc.

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


package org.openqa.selenium.internal.seleniumemulation;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.Resources;

/**
 * Add a function backed by the closure-based implementation of Selenium Core.
 */
public class SeleniumMutator implements ScriptMutator {
  private final Pattern pattern;
  private final String method;
  private final String atomicName;
  private final String atom;

  public SeleniumMutator(String method, String atomicName, String atom) {
    String raw = ".*" + method.replace(".", "\\s*\\.\\s*") + ".*";
    this.pattern = Pattern.compile(raw);
    this.method = method;
    this.atomicName = atomicName;
    this.atom = readAtom(atom);
  }

  public void mutate(String script, StringBuilder appendTo) {
    if (!pattern.matcher(script).matches()) {
      return;
    }
    
    // Inject the raw atom.
    appendTo.append(atom);
    // Then alias it
    appendTo.append(method);
    appendTo.append(" = function() { return ");

    // Set "this" to be the pre-declared selenium object
    appendTo.append(atomicName).append(".apply(null, arguments);};");
  }

  protected String readAtom(String path) {
    URL url = getClass().getResource(path);

    if (url == null) {
      throw new RuntimeException("Cannot locate atom for " + path);
    }

    try {
      return Resources.toString(url, Charsets.UTF_8);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}

