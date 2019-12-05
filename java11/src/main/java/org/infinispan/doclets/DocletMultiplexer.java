package org.infinispan.doclets;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import jdk.javadoc.doclet.StandardDoclet;

/**
 * This doclet just delegates to StandardDoclet and also adds our JmxDoclet in the mix. It also attempts to perform
 * exclusion of classes that have the "private" tag.
 */
public final class DocletMultiplexer implements Doclet {

   private final Doclet[] doclets = {
         new StandardDoclet(),
         //new JmxDoclet()
   };

   @Override
   public void init(Locale locale, Reporter reporter) {
      for (Doclet doclet : doclets) {
         doclet.init(locale, reporter);
      }
   }

   @Override
   public String getName() {
      return getClass().getSimpleName();
   }

   @Override
   public Set<? extends Option> getSupportedOptions() {
      Set<Option> options = new HashSet<>();
      for (Doclet doclet : doclets) {
         options.addAll(doclet.getSupportedOptions());
      }
      return options;
   }

   @Override
   public SourceVersion getSupportedSourceVersion() {
      return doclets[0].getSupportedSourceVersion();
   }

   @Override
   public boolean run(DocletEnvironment environment) {
      boolean result = true;
      for (Doclet doclet : doclets) {
         result &= doclet.run(environment);
      }
      return result;
   }
}
