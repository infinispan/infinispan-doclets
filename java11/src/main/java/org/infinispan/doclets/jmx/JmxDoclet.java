package org.infinispan.doclets.jmx;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.util.DocTrees;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

/**
 * A Doclet that generates a guide to all JMX components exposed by Infinispan (and properly annotated) .
 *
 * @author Manik Surtani
 * @author Tristan Tarrant
 * @since 4.0
 */
public final class JmxDoclet implements Doclet {

   public static final String MANAGED_ATTRIBUTE_CLASSNAME = "org.infinispan.jmx.annotations.ManagedAttribute";
   public static final String MANAGED_OPERATION_CLASSNAME = "org.infinispan.jmx.annotations.ManagedOperation";
   public static final String MBEAN_CLASSNAME = "org.infinispan.jmx.annotations.MBean";

   private static String outputDirectory;
   private static String title;

   private static String jmxTitle() {
      String s = "JMX Components";
      if (title != null && !title.isEmpty()) {
         s += " (" + title + ")";
      }
      return s;
   }

   private static MBeanComponent fromMXBeanInterface(TypeElement cd, DocTrees docTrees) {
      MBeanComponent mbc = new MBeanComponent(cd.getQualifiedName().toString(), cd.getSimpleName().toString());
      mbc.desc = docTreeToText(docTrees.getDocCommentTree(cd));

      cd.getEnclosedElements().forEach(e -> {
         if (e instanceof ExecutableElement) {
            ExecutableElement ee = (ExecutableElement) e;

            String methodName = ee.getSimpleName().toString();
            if (methodName.startsWith("get") || methodName.startsWith("is")) {
               MBeanAttribute attr = mbc.attributes.computeIfAbsent(fromBeanConvention(methodName), MBeanAttribute::new);
               attr.type = ee.getReturnType().toString();
               attr.desc = docTreeToText(docTrees.getDocCommentTree(e));
            } else if (methodName.startsWith("set")) {
               MBeanAttribute attr = mbc.attributes.computeIfAbsent(fromBeanConvention(methodName), MBeanAttribute::new);
               attr.writable = true;
            } else {
               MBeanOperation o = mbc.operations.computeIfAbsent(ee.getSimpleName().toString(), MBeanOperation::new);
               o.returnType = getTypeName(ee.getReturnType());
               o.desc = docTreeToText(docTrees.getDocCommentTree(e));
               for (VariableElement p : ee.getParameters()) {
                  o.addParam(getTypeName(p.asType()), p.getSimpleName().toString());
               }
            }
         }
      });

      return mbc;
   }

   private static String docTreeToText(DocCommentTree commentTree) {
      StringBuilder sb = new StringBuilder();
      if (commentTree != null) {
         commentTree.getFullBody().forEach(d -> sb.append(d.toString()));
      }
      return sb.toString();
   }

   private static MBeanComponent fromMBeanAnnotations(TypeElement cd, AnnotationMirror am) {
      String objectName = getAnnotationValue(am, "objectName", cd.getSimpleName().toString());
      MBeanComponent mbc = new MBeanComponent(cd.getQualifiedName().toString(), objectName);
      mbc.desc = getAnnotationValue(am, "description", "");

      cd.getEnclosedElements().forEach(e -> {
         if (e instanceof ExecutableElement) {
            ExecutableElement ee = (ExecutableElement) e;
            for (AnnotationMirror a : ee.getAnnotationMirrors()) {
               String annotationName = ((TypeElement) a.getAnnotationType().asElement()).getQualifiedName().toString();
               if (annotationName.equals(MANAGED_OPERATION_CLASSNAME)) {
                  MBeanOperation o = mbc.operations.computeIfAbsent(ee.getSimpleName().toString(), MBeanOperation::new);
                  setNameDesc(a.getElementValues(), o);
                  o.returnType = getTypeName(ee.getReturnType());
                  for (VariableElement p : ee.getParameters()) {
                     o.addParam(getTypeName(p.asType()), p.getSimpleName().toString());
                  }
               } else if (annotationName.equals(MANAGED_ATTRIBUTE_CLASSNAME)) {
                  // if this is a getter, look at the return type
                  String methodName = ee.getSimpleName().toString();
                  MBeanAttribute attr = mbc.attributes.computeIfAbsent(fromBeanConvention(methodName), MBeanAttribute::new);
                  if (methodName.startsWith("get") || methodName.startsWith("is")) {
                     attr.type = ee.getReturnType().toString();
                  } else if (ee.getParameters().size() > 0) {
                     attr.type = ee.getParameters().get(0).getSimpleName().toString();
                  }
                  setNameDesc(a.getElementValues(), attr);
                  setWritable(a.getElementValues(), attr);
               }
            }
         } else if (e instanceof VariableElement) {
            VariableElement ve = (VariableElement) e;
            for (AnnotationMirror a : ve.getAnnotationMirrors()) {
               String annotationName = ((TypeElement) a.getAnnotationType().asElement()).getQualifiedName().toString();
               if (annotationName.equals(MANAGED_ATTRIBUTE_CLASSNAME)) {
                  MBeanAttribute attr = mbc.attributes.computeIfAbsent(ve.getSimpleName().toString(), MBeanAttribute::new);
                  attr.type = getTypeName(ve.asType());
                  setNameDesc(a.getElementValues(), attr);
                  setWritable(a.getElementValues(), attr);
               }
            }
         }
      });

      return mbc;
   }

   private static MBeanComponent toJmxComponent(TypeElement cd, DocTrees docTrees) {
      String className = cd.getQualifiedName().toString();
      if (className.endsWith("MXBean")) {
         return fromMXBeanInterface(cd, docTrees);
      } else {
         for (AnnotationMirror a : cd.getAnnotationMirrors()) {
            DeclaredType atd = a.getAnnotationType();
            String annotationName = atd.toString();

            if (annotationName.equals(MBEAN_CLASSNAME)) {
               return fromMBeanAnnotations(cd, a);
            }
         }
      }
      return null;
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

   private static String getAnnotationValue(AnnotationMirror a, String name, String defaultValue) {
      for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> evp : a.getElementValues().entrySet()) {
         String annotationName = evp.getKey().getSimpleName().toString();
         if (name.equals(annotationName)) {
            return evp.getValue().getValue().toString();
         }
      }
      return defaultValue;
   }

   private static void setNameDesc(Map<? extends ExecutableElement, ? extends AnnotationValue> evps, JmxComponent mbc) {
      for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> evp : evps.entrySet()) {
         String name = evp.getKey().getSimpleName().toString();
         if ("description".equals(name)) {
            mbc.desc = evp.getValue().getValue().toString();
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
      return SourceVersion.latestSupported();
   }

   @Override
   public boolean run(DocletEnvironment docletEnvironment) {
      DocTrees docTrees = docletEnvironment.getDocTrees();
      List<MBeanComponent> mbeans = docletEnvironment.getIncludedElements().stream()
                                                     .filter(e -> e instanceof TypeElement)
                                                     .map(e -> (TypeElement) e)
                                                     .map(e -> toJmxComponent(e, docTrees))
                                                     .filter(Objects::nonNull)
                                                     .sorted()  // sort components alphabetically
                                                     .collect(Collectors.toList());

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
