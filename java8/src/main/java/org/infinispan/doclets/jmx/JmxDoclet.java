package org.infinispan.doclets.jmx;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.infinispan.doclets.html.HtmlGenerator;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationTypeDoc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import com.sun.tools.doclets.standard.Standard;
import com.sun.tools.javadoc.Main;

/**
 * A Doclet that generates a guide to all JMX components exposed by Infinispan
 *
 * @author Manik Surtani
 * @since 4.0
 */
public class JmxDoclet {
   public static final String MANAGED_ATTRIBUTE_CLASSNAME = "org.infinispan.jmx.annotations.ManagedAttribute";
   public static final String MANAGED_OPERATION_CLASSNAME = "org.infinispan.jmx.annotations.ManagedOperation";
   public static final String MBEAN_CLASSNAME = "org.infinispan.jmx.annotations.MBean";

   private static String outputDirectory;
   private static String encoding;
   private static String bottom;
   private static String footer;
   private static String header;
   private static String title;

   public static void main(String[] args) {
      String name = JmxDoclet.class.getName();
      Main.execute(name, name, args);
   }

   public static boolean start(RootDoc root) throws IOException {
      ClassDoc[] classes = root.classes();

      List<MBeanComponent> mbeans = new LinkedList<>();

      for (ClassDoc cd : classes) {
         MBeanComponent mbean = toJmxComponent(cd);
         if (mbean != null)
            mbeans.add(mbean);
      }

      // sort components alphabetically
      Collections.sort(mbeans);

      HtmlGenerator generator = new JmxHtmlGenerator(jmxTitle(), "JMX components exposed by Infinispan",
            "JMX, Infinispan, Data Grids, Documentation, Reference, MBeans, Management, Console", mbeans);
      generator.generateHtml(new File(outputDirectory, "jmxComponents.html").getAbsolutePath());

      return true;
   }

   public static LanguageVersion languageVersion() {
      return LanguageVersion.JAVA_1_5;
   }

   public static int optionLength(String option) {
      return Standard.optionLength(option);
   }

   public static boolean validOptions(String[][] options, DocErrorReporter reporter) {
      for (String[] option : options) {
         if (option[0].equals("-d"))
            outputDirectory = option[1];
         else if (option[0].equals("-encoding"))
            encoding = option[1];
         else if (option[0].equals("-bottom"))
            bottom = option[1];
         else if (option[0].equals("-footer"))
            footer = option[1];
         else if (option[0].equals("-header"))
            header = option[1];
         else if (option[0].equals("-doctitle"))
            title = option[1];
      }
      return true;
   }

   private static String jmxTitle() {
      String s = "JMX Components";
      if (title == null || title.length() == 0)
         return s;
      else {
         s += " (" + title + ")";
         return s;
      }
   }

   private static MBeanComponent toJmxComponent(ClassDoc cd) {
      boolean isMBean = false;
      MBeanComponent mbc = new MBeanComponent(cd.qualifiedName(), cd.typeName());

      for (AnnotationDesc a : cd.annotations()) {
         AnnotationTypeDoc atd = a.annotationType();
         String annotationName = atd.qualifiedTypeName();

         if (annotationName.equals(MBEAN_CLASSNAME)) {
            isMBean = true;
            setNameDesc(a.elementValues(), mbc);
         }
      }

      // now to test method level annotations
      for (MethodDoc method : cd.methods()) {
         for (AnnotationDesc a : method.annotations()) {
            String annotationName = a.annotationType().qualifiedTypeName();
            if (annotationName.equals(MANAGED_OPERATION_CLASSNAME)) {
               isMBean = true;
               MBeanOperation o = mbc.operations.computeIfAbsent(method.name(), MBeanOperation::new);
               setNameDesc(a.elementValues(), o);
               o.returnType = method.returnType().simpleTypeName();
               for (Parameter p : method.parameters())
                  o.addParam(p.type().simpleTypeName(), p.name());

            } else if (annotationName.equals(MANAGED_ATTRIBUTE_CLASSNAME)) {
               isMBean = true;
               MBeanAttribute attr = mbc.attributes.computeIfAbsent(fromBeanConvention(method.name()), MBeanAttribute::new);
               // if this is a getter, look at the return type
               if (method.name().startsWith("get") || method.name().startsWith("is")) {
                  attr.type = method.returnType().simpleTypeName();
               } else if (method.parameters().length > 0) {
                  attr.type = method.parameters()[0].type().simpleTypeName();
               }
               setNameDesc(a.elementValues(), attr);
               setWritable(a.elementValues(), attr);
            }
         }
      }

      // and field level annotations
      for (FieldDoc field : cd.fields(false)) {

         for (AnnotationDesc a : field.annotations()) {
            String annotationName = a.annotationType().qualifiedTypeName();

            if (annotationName.equals(MANAGED_ATTRIBUTE_CLASSNAME)) {
               isMBean = true;
               MBeanAttribute attr = mbc.attributes.computeIfAbsent(field.name(), MBeanAttribute::new);
               attr.type = field.type().simpleTypeName();
               setNameDesc(a.elementValues(), attr);
               setWritable(a.elementValues(), attr);
            }
         }
      }

      if (isMBean) {
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

   private static void setNameDesc(AnnotationDesc.ElementValuePair[] evps, JmxComponent mbc) {
      for (AnnotationDesc.ElementValuePair evp : evps) {
         if (evp.element().name().equals("description")) {
            mbc.desc = evp.value().value().toString();
         }
      }
   }

   private static void setWritable(AnnotationDesc.ElementValuePair[] evps, MBeanAttribute attr) {
      for (AnnotationDesc.ElementValuePair evp : evps) {
         if (evp.element().name().equals("writable")) {
            attr.writable = (Boolean) evp.value().value();
         }
      }
   }
}
