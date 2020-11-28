package com.lagou.edu.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Transactional {
    String value() default "TransactionManager";
}
