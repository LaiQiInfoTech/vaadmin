// dev.w0fv1.vaadmin.view.form.model.ConditionalDisplay
package dev.w0fv1.vaadmin.view.form.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field in a BaseFormModel to have its visibility controlled by a specific condition.
 * The condition is evaluated by a class implementing FormVisibilityCondition.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionalDisplay {
    /**
     * The class that implements the visibility logic.
     * This class must implement FormVisibilityCondition and have a no-argument constructor.
     */
    Class<? extends FormVisibilityCondition> [] value();
}