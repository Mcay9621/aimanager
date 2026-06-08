package com.example.aimanager.common;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogAudit {

    String action() default "OTHER";

    String target() default "";

    String detail() default "";
}
