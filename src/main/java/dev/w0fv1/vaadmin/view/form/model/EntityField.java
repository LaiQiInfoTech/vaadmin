package dev.w0fv1.vaadmin.view.form.model;

import dev.w0fv1.vaadmin.entity.BaseManageEntity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EntityField {
    boolean entity() default true;

    /**
     * Mapper class used to apply an entity (or entity list) onto the target model.
     *
     * <p>Kept as {@code Class<?>} to avoid coupling to a specific mapper API, since the upstream
     * mapper library may introduce breaking changes between versions.</p>
     */
    Class<?> entityMapper();

    Class<? extends BaseManageEntity<?>> entityType();
}
