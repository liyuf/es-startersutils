package com.leyou.starter.elastic.annotaions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个索引库信息
 * @author 虎哥
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Index {
    /**
     * 索引库名称，必填
     * @return 索引库名称
     */
    String value();
}