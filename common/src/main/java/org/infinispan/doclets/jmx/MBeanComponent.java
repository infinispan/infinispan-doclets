package org.infinispan.doclets.jmx;

import java.util.Map;
import java.util.TreeMap;

/**
 * * An MBean component
 *
 * @author Manik Surtani
 * @since 4.0
 */
public class MBeanComponent extends JmxComponent {
   public String className;
   public Map<String, MBeanOperation> operations = new TreeMap<>();
   public Map<String, MBeanAttribute> attributes = new TreeMap<>();

   public MBeanComponent(String className, String name) {
      super(name);
      this.className = className;
   }

   @Override
   public String toString() {
      return "MBean component " + name + " (class " + className + ") op = " + operations + " attr = " + attributes;
   }
}
