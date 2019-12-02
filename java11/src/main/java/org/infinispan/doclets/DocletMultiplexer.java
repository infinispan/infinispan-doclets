package org.infinispan.doclets;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.tools.Diagnostic;

import org.infinispan.doclets.jmx.JmxDoclet;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import jdk.javadoc.doclet.StandardDoclet;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * This doclet just delegates to StandardDoclet and also adds our JmxDoclet in the mix. It also attempts to perform
 * exclusion of classes that have the "private" tag.
 */
public final class DocletMultiplexer implements Doclet {

   private static final String PRIVATE_TAG = "@private";

   private static final String PUBLIC_TAG = "@public";

   private final StandardDoclet standardDoclet = new StandardDoclet();

   private final JmxDoclet jmxDoclet = new JmxDoclet();

   private Reporter reporter;

   @Override
   public void init(Locale locale, Reporter reporter) {
      this.reporter = reporter;
      standardDoclet.init(locale, reporter);
      jmxDoclet.init(locale, reporter);
   }

   @Override
   public String getName() {
      return getClass().getSimpleName();
   }

   @Override
   public Set<? extends Option> getSupportedOptions() {
      Set<Option> options = new HashSet<>();
      options.addAll(standardDoclet.getSupportedOptions());
      options.addAll(jmxDoclet.getSupportedOptions());
      return options;
   }

   @Override
   public SourceVersion getSupportedSourceVersion() {
      return standardDoclet.getSupportedSourceVersion();
   }

   /**
    * Filter an Element based on presence of custom javadoc tags on it and its enclosing elements. Here we handle
    * "@private" and "@public" tags. If both tags are present (should never happen) we consider it private.
    */
   private static boolean isPublicAPI(DocletEnvironment env, Element e) {
      Objects.requireNonNull(env);
      Objects.requireNonNull(e);
      if (hasTag(env, e, PRIVATE_TAG)) {
         return false;
      }
      if (e instanceof ModuleElement) {
         return true;
      }
      if (hasTag(env, e, PUBLIC_TAG)) {
         return !isMarkedPrivate(env, e);
      }
      Element enclosingElement = e.getEnclosingElement();
      if (enclosingElement != null) {
         return isPublicAPI(env, enclosingElement);
      }
      return false;
   }

   /**
    * Checks if the element or one of its enclosing elements is explicitly marked private.
    */
   private static boolean isMarkedPrivate(DocletEnvironment env, Element e) {
      if (hasTag(env, e, PRIVATE_TAG)) {
         return true;
      }
      Element enclosingElement = e.getEnclosingElement();
      return enclosingElement != null && isMarkedPrivate(env, enclosingElement);
   }

   private static boolean hasTag(DocletEnvironment env, Element e, String tag) {
      String docComment = env.getElementUtils().getDocComment(e);
      if (docComment == null) {
         return false;
      }
      int pos = docComment.indexOf(tag);
      if (pos == -1) {
         return false;
      }
      int after = pos + tag.length();
      return after == docComment.length() || Character.isWhitespace(docComment.charAt(after));
   }

   private static String getStackTraceAsString(Throwable throwable) {
      StringWriter stringWriter = new StringWriter();
      throwable.printStackTrace(new PrintWriter(stringWriter));
      return stringWriter.toString();
   }

   @Override
   public boolean run(DocletEnvironment environment) {
      // JMX doclet is executed with full element set
      boolean result = jmxDoclet.run(environment);

      try {
         filterUnmodifiableSet(environment.getSpecifiedElements(), (Predicate<Element>) e -> filterElement(environment, e));
         filterUnmodifiableSet(environment.getIncludedElements(), (Predicate<Element>) e -> filterElement(environment, e));
      } catch (Exception ex) {
         reporter.print(Diagnostic.Kind.ERROR, "Failed to filter element set");
         reporter.print(Diagnostic.Kind.ERROR, getStackTraceAsString(ex));
      }

      DocletEnvironment proxyEnv;
      try {
         proxyEnv = makeDocletEnvironmentProxy(environment);
      } catch (Exception ex) {
         reporter.print(Diagnostic.Kind.ERROR, "Failed to proxify DocletEnvironment");
         reporter.print(Diagnostic.Kind.ERROR, getStackTraceAsString(ex));

         // fallback to unfiltered env
         proxyEnv = environment;
      }

      // standard doclet is executed with filtered element set
      result &= standardDoclet.run(proxyEnv);

      return result;
   }

   private boolean filterElement(DocletEnvironment env, Element e) {
      boolean result = isPublicAPI(env, e);
//      reporter.print(Diagnostic.Kind.NOTE, "Filtering non-API JavaDocs : " + e + " : " + (result ? "PUBLIC" : "PRIVATE"));
      return result;
   }

   /**
    * Horrid hacks to remove select elements from an unmodifiable Set (in place).
    */
   private <X> void filterUnmodifiableSet(Set<? extends X> unmodifiableSet, Predicate<X> predicate) throws Exception {
      Class<?> zeeClazz = unmodifiableSet.getClass();
      Field c = zeeClazz.getSuperclass().getDeclaredField("c");
      c.setAccessible(true);
      Set<X> innerSet = (Set<X>) c.get(unmodifiableSet);
      Set<X> toRemove = new HashSet<>();
      for (X e : innerSet) {
         if (!predicate.test(e)) {
            toRemove.add(e);
         }
      }
      innerSet.removeAll(toRemove);
   }

   /**
    * A second bunch of festering dirty sleazy hacks to create a class proxy for DocletEnvironment. We need to subclass
    * the implementation so casting can work. ByteBuddy is our friend. Normal dynamic proxies do not work for this.
    */
   private DocletEnvironment makeDocletEnvironmentProxy(DocletEnvironment environment) throws Exception {
      Class<? extends DocletEnvironment> envImplClass = environment.getClass();
      // Get all fields via reflection and categorize them by type
      Map<Class<?>, Object> fieldsByType = new HashMap<>();
      for (Field f : envImplClass.getDeclaredFields()) {
         f.setAccessible(true);
         Object v = f.get(environment);
         if (v != null) {
            fieldsByType.put(f.getType(), v);
         }
      }

      // Get the constructor signature with most args
      Optional<Constructor<?>> maxCtor = Arrays.stream(envImplClass.getDeclaredConstructors()).max(Comparator.comparingInt(Constructor::getParameterCount));
      Class<?>[] paramTypes = maxCtor.get().getParameterTypes();
      Object[] paramValues = new Object[paramTypes.length];

      // Try to fill those args with values from internal fields. Match by type. Pray.
      for (int i = 0; i < paramValues.length; i++) {
         paramValues[i] = fieldsByType.get(paramTypes[i]);
      }

      DocletEnvironmentInterceptor interceptor = new DocletEnvironmentInterceptor(environment);

      // Make a proxy class to perform the interception
      Class<? extends DocletEnvironment> proxyClass = new ByteBuddy()
            .subclass(envImplClass)
            .method(ElementMatchers.named("isIncluded")).intercept(MethodDelegation.to(interceptor))
            .method(ElementMatchers.named("isSelected")).intercept(MethodDelegation.to(interceptor))
            .make()
            .load(getClass().getClassLoader())
            .getLoaded();

      // Instantiate the proxy
      Constructor<? extends DocletEnvironment> ctor = proxyClass.getConstructor(paramTypes);
      return ctor.newInstance(paramValues);
   }

   /**
    * Intercepted methods land here.
    */
   public final class DocletEnvironmentInterceptor {

      private final DocletEnvironment original;

      DocletEnvironmentInterceptor(DocletEnvironment original) {
         this.original = original;
      }

      public boolean isIncluded(Element e) {
         return original.isIncluded(e) && filterElement(original, e);
      }

      public boolean isSelected(Element e) {
         return original.isSelected(e) && filterElement(original, e);
      }
   }
}
