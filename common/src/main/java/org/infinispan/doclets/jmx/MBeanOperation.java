package org.infinispan.doclets.jmx;

/**
 * An MBean operation.
 *
 * @author Manik Surtani
 * @since 4.0
 */
final class MBeanOperation extends JmxComponent {
   public String returnType = "void";
   public String signature = "";

   MBeanOperation(String name) {
      super(name);
   }

   @Override
   public String toString() {
      return "Operation(name = " + name + ", desc = " + desc + ", sig = " + signature + ", retType = " + returnType + ")";
   }

   void addParam(String paramType, String paramName) {
      if (!signature.isEmpty()) signature += ", ";
      signature += paramType + " " + paramName;
   }
}
