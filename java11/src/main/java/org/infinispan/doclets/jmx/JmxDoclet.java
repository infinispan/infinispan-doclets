package org.infinispan.doclets.jmx;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.infinispan.doclets.LambdaOption;
import org.infinispan.doclets.html.HtmlGenerator;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

/**
 * A Doclet that generates a guide to all JMX components exposed by Infinispan
 *
 * @author Manik Surtani
 * @author Tristan Tarrant
 * @since 4.0
 */
public class JmxDoclet implements Doclet {
   public static final String MANAGED_ATTRIBUTE_CLASSNAME = "org.infinispan.jmx.annotations.ManagedAttribute";
   public static final String MANAGED_OPERATION_CLASSNAME = "org.infinispan.jmx.annotations.ManagedOperation";
   public static final String MBEAN_CLASSNAME = "org.infinispan.jmx.annotations.MBean";

   static String outputDirectory;
   static String title;

   private static String jmxTitle() {
      String s = "JMX Components";
      if (title == null || title.length() == 0)
         return s;
      else {
         s += " (" + title + ")";
         return s;
      }
   }

   private static MBeanComponent toJmxComponent(TypeElement cd) {
      boolean isMBean = false;
      MBeanComponent mbc = new MBeanComponent();
      mbc.className = cd.getQualifiedName().toString();
      mbc.name = cd.getSimpleName().toString();

      for (AnnotationMirror a : cd.getAnnotationMirrors()) {
         DeclaredType atd = a.getAnnotationType();
         String annotationName = atd.toString();

         if (annotationName.equals(MBEAN_CLASSNAME)) {
            isMBean = true;
            setNameDesc(a.getElementValues(), mbc);
         }
      }

      // now to test method level annotations
      cd.getEnclosedElements().stream().forEach(e -> {
         if (e instanceof ExecutableElement) {
            ExecutableElement ee = (ExecutableElement) e;
            for (AnnotationMirror a : ee.getAnnotationMirrors()) {
               String annotationName = ((TypeElement) a.getAnnotationType().asElement()).getQualifiedName().toString();
               if (annotationName.equals(MANAGED_OPERATION_CLASSNAME)) {
                  MBeanOperation o = new MBeanOperation();
                  setNameDesc(a.getElementValues(), o);
                  o.name = ee.getSimpleName().toString();
                  o.returnType = getTypeName(ee.getReturnType());
                  for (VariableElement p : ee.getParameters()) {
                     o.addParam(getTypeName(p.asType()), p.getSimpleName().toString());
                  }
                  mbc.operations.add(o);

               } else if (annotationName.equals(MANAGED_ATTRIBUTE_CLASSNAME)) {
                  MBeanAttribute attr = new MBeanAttribute();

                  // if this is a getter, look at the return type
                  String methodName = ee.getSimpleName().toString();
                  if (methodName.startsWith("get") || methodName.startsWith("is")) {
                     attr.type = ((DeclaredType) ee.getReturnType()).asElement().getSimpleName().toString();
                  } else if (ee.getParameters().size() > 0) {
                     attr.type = ee.getParameters().get(0).getSimpleName().toString();
                  }

                  attr.name = fromBeanConvention(methodName);
                  setNameDesc(a.getElementValues(), attr);
                  setWritable(a.getElementValues(), attr);
                  mbc.attributes.add(attr);
               }
            }
         } else if (e instanceof VariableElement) {
            VariableElement ve = (VariableElement) e;
            for (AnnotationMirror a : ve.getAnnotationMirrors()) {
               String annotationName = ((TypeElement) a.getAnnotationType().asElement()).getQualifiedName().toString();
               if (annotationName.equals(MANAGED_ATTRIBUTE_CLASSNAME)) {
                  MBeanAttribute attr = new MBeanAttribute();
                  attr.name = ve.getSimpleName().toString();
                  attr.type = getTypeName(ve.asType());
                  setNameDesc(a.getElementValues(), attr);
                  setWritable(a.getElementValues(), attr);
                  mbc.attributes.add(attr);
               }
            }
         }
      });

      if (isMBean) {
         Collections.sort(mbc.attributes);
         Collections.sort(mbc.operations);
         return mbc;
      } else {
         return null;
      }
   }

   private static String fromBeanConvention(String getterOrSetter) {
      if (getterOrSetter.startsWith("get") || getterOrSetter.startsWith("set")) {
         String withoutGet = getterOrSetter.substring(4);
         // not specifically BEAN convention, but this is what is bound in JMX.
         return Character.toUpperCase(getterOrSetter.charAt(3)) + withoutGet;
      } else if (getterOrSetter.startsWith("is")) {
         String withoutIs = getterOrSetter.substring(3);
         return Character.toUpperCase(getterOrSetter.charAt(2)) + withoutIs;
      }
      return getterOrSetter;
   }

   private static void setNameDesc(Map<? extends ExecutableElement, ? extends AnnotationValue> evps, JmxComponent mbc) {
      for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> evp : evps.entrySet()) {
         String name = evp.getKey().getSimpleName().toString();
         switch (name) {
            case "name":
            case "objectName":
               mbc.name = evp.getValue().getValue().toString();
               break;
            case "description":
               mbc.desc = evp.getValue().getValue().toString();
               break;
         }
      }
   }

   private static void setWritable(Map<? extends ExecutableElement, ? extends AnnotationValue> evps, MBeanAttribute attr) {
      for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> evp : evps.entrySet()) {
         if (evp.getKey().getSimpleName().toString().equals("writable")) {
            attr.writable = (Boolean) evp.getValue().getValue();
         }
      }
   }

   @Override
   public void init(Locale locale, Reporter reporter) {

   }

   @Override
   public String getName() {
      return "JMX Doclet";
   }

   @Override
   public Set<? extends Option> getSupportedOptions() {
      Option[] options = {
            new LambdaOption(1, "Doc Title", Option.Kind.STANDARD,
                  Collections.singletonList("-doctitle"), "string", (opt, args) -> {
               title = args.get(0);
               return true;
            }),
            new LambdaOption(1, "Output Directory", Option.Kind.STANDARD,
                  Collections.singletonList("-d"), "directory", (opt, args) -> {
               outputDirectory = args.get(0);
               return true;
            }),

      };
      return new HashSet<>(Arrays.asList(options));
   }

   @Override
   public SourceVersion getSupportedSourceVersion() {
      return SourceVersion.RELEASE_8;
   }

   @Override
   public boolean run(DocletEnvironment docletEnvironment) {
      List<MBeanComponent> mbeans = docletEnvironment.getIncludedElements().stream()
            .filter(e -> e instanceof TypeElement)
            .map(e -> (TypeElement) e)
            .map(e -> toJmxComponent(e))
            .filter(e -> e != null)
            .collect(Collectors.toList());

      // sort components alphabetically
      Collections.sort(mbeans);

      HtmlGenerator generator = new JmxHtmlGenerator(jmxTitle(),
            "JMX components",
            "JMX, Infinispan, Data Grids, Documentation, Reference, MBeans, Management, Console",
            mbeans);
      try {
         generator.generateHtml(new File(outputDirectory, "jmxComponents.html").getAbsolutePath());
      } catch (IOException e) {
         throw new RuntimeException(e);
      }

      return true;
   }

   private static String getTypeName(TypeMirror t) {
      return t.toString();
   }
}
