package org.infinispan.doclets.sample;

import java.util.Collection;
import java.util.Collections;

import org.infinispan.jmx.annotations.MBean;
import org.infinispan.jmx.annotations.ManagedAttribute;
import org.infinispan.jmx.annotations.ManagedOperation;

/**
 * SampleMBeanC
 * @api.private
 */
@SuppressWarnings("unused")
@MBean(description = "SampleMBeanC has the same objectName as MBeanA, just to confuse you", objectName = "MBeanA")
public class SampleMBeanC {

   private String stuff = "stuff";

   /**
    * get some stuff
    * @return some stuff
    */
   @ManagedAttribute(description = "Gets some stuff", writable = true)
   public String getSomeStuff() {
      return stuff;
   }

   /**
    * set some stuff
    * @param stuff some stuff
    */
   public void setSomeStuff(String stuff) {
      this.stuff = stuff;
   }

   /**
    * Do stuff
    * @param x a value
    * @param i another value
    */
   @ManagedOperation(description = "Do some stuff", displayName = "Do it", name = "Do stuff")
   public void doStuff(String x, int i) {
      // Do stuff !
   }

   /**
    * Do more stuff
    * @param x a value
    * @param i another value
    * @return something
    */
   @ManagedOperation(description = "Do more stuff", displayName = "Do it", name = "Do more stuff")
   public int doMoreStuff(String x, int i) {
      // Do stuff !
      return 0;
   }

   /**
    * Do long stuff
    * @param x a value
    * @param i another value
    * @return stuff
    */
   @ManagedOperation(description = "Do long stuff", displayName = "Do it", name = "Do long stuff")
   public Collection<String> doLongStuff(String x, int i) {
      return Collections.emptyList();
   }
}
