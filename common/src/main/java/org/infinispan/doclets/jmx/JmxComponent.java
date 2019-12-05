package org.infinispan.doclets.jmx;

/**
 * A JMX Component.
 *
 * @author Manik Surtani
 * @since 4.0
 */
abstract class JmxComponent implements Comparable<JmxComponent> {
   public final String name;
   public String desc = "";

   JmxComponent(String name) {
      this.name = name;
   }

   @Override
   public int compareTo(JmxComponent other) {
      return name.compareTo(other.name);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      JmxComponent that = (JmxComponent) o;
      return name != null ? name.equals(that.name) : that.name == null;
   }

   @Override
   public int hashCode() {
      return name != null ? name.hashCode() : 0;
   }
}
