package org.infinispan.doclets.api;

import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.RootDoc;
import com.sun.tools.doclets.standard.Standard;
import com.sun.tools.javadoc.Main;

public class PublicAPIDoclet {

   public static void main(String[] args) {
      String name = PublicAPIDoclet.class.getName();
      Main.execute(name, name, args);
   }

   public static LanguageVersion languageVersion() {
      return LanguageVersion.JAVA_1_5;
   }

   public static int optionLength(String option) {
      return Standard.optionLength(option);
   }

   public static boolean start(RootDoc root) throws java.io.IOException {
      return Standard.start((RootDoc) PublicAPIFilterHandler.filter(root, RootDoc.class));
   }
}
