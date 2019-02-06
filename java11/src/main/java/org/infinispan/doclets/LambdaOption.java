package org.infinispan.doclets;

import java.util.List;
import java.util.function.BiFunction;

import jdk.javadoc.doclet.Doclet;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 * @private
 **/
public class LambdaOption implements Doclet.Option {
   final int argumentCount;
   final String description;
   final Kind kind;
   final List<String> names;
   final String parameters;
   final BiFunction<String, List<String>, Boolean> processor;

   public LambdaOption(int argumentCount, String description, Kind kind, List<String> names, String parameters, BiFunction<String, List<String>, Boolean> processor) {
      this.argumentCount = argumentCount;
      this.description = description;
      this.kind = kind;
      this.names = names;
      this.parameters = parameters;
      this.processor = processor;
   }

   @Override
   public int getArgumentCount() {
      return argumentCount;
   }

   @Override
   public String getDescription() {
      return description;
   }

   @Override
   public Kind getKind() {
      return kind;
   }

   @Override
   public List<String> getNames() {
      return names;
   }

   @Override
   public String getParameters() {
      return parameters;
   }

   @Override
   public boolean process(String s, List<String> list) {
      return processor.apply(s, list);
   }
}
