package org.infinispan.jmx.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is a copy of the real annotation found in Infinispan source tree. Should be kept in sync.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface MBean {

   String objectName() default "";

   String description() default "";
}
