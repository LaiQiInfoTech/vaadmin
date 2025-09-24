// dev.w0fv1.vaadmin.view.form.model.FormVisibilityCondition
package dev.w0fv1.vaadmin.view.form.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Interface for defining custom logic to determine a form field's visibility.
 * Implementations should have a no-argument constructor.
 */
public interface FormVisibilityCondition<F extends BaseFormModel> {
    /**
     * Evaluates whether the field associated with this condition should be visible.
     *
     * @param model The current BaseFormModel instance, allowing access to all form data.
     *              Implementations can use reflection (e.g., model.getClass().getDeclaredField("fieldName").get(model))
     *              to get values of other fields if needed.
     * @return true if the field should be visible, false otherwise.
     */
    boolean evaluate(BaseFormModel model);

    /**
     * Returns a set of field names that this condition explicitly depends on.
     * When any of these fields change, this condition (and thus the dependent field's visibility)
     * will be re-evaluated.
     * <p>
     * It's highly recommended to specify dependent fields for performance. If the condition
     * potentially depends on any field or is very complex, return an empty set.
     * An empty set will trigger re-evaluation on any form field change (less efficient for large forms)
     * or when `reEvaluateAllConditionalFields()` is explicitly called.
     *
     * @return A set of field names (String) that this condition depends on.
     * Return an empty set if it implicitly depends on multiple fields or no specific fields,
     * meaning it will be re-evaluated less frequently (only on full form refresh/reset).
     */
    default Set<String> getDependentFieldNames() {
        return this.getDependentFieldGetter().stream().map(
                FormVisibilityCondition::getFieldName
        ).collect(Collectors.toSet());
    }

    default Set<SerializableFunction<F, ?>> getDependentFieldGetter() {
        return Collections.emptySet();
    }


    public static class DefaultFormVisibilityCondition implements FormVisibilityCondition {

        @Override
        public boolean evaluate(BaseFormModel model) {
            return true;
        }
    }

    @FunctionalInterface
    public interface SerializableFunction<T, R> extends Function<T, R>, Serializable {
    }


    /**
     * 从一个 Getter 方法引用中解析出对应的属性名。
     * 例如，传入 User::getName，将返回 "name"。
     *
     * @param getter 一个可序列化的方法引用，通常是 getter。
     * @param <T>    Bean 的类型。
     * @return 解析出的属性名。
     * @throws IllegalArgumentException 如果无法解析方法名。
     */
    public static <T> String getFieldName(SerializableFunction<T, ?> getter) {
        try {
            // 1. 获取 Lambda 表达式的 "writeReplace" 方法，这是序列化的入口
            Method writeReplaceMethod = getter.getClass().getDeclaredMethod("writeReplace");
            writeReplaceMethod.setAccessible(true);

            // 2. 调用 writeReplace 方法，返回一个 SerializedLambda 对象
            SerializedLambda serializedLambda = (SerializedLambda) writeReplaceMethod.invoke(getter);

            // 3. 从 SerializedLambda 中获取实现该 Lambda 的方法名 (例如 "getName")
            String methodName = serializedLambda.getImplMethodName();

            // 4. 将方法名转换为属性名 (例如 "getName" -> "name")
            return toPropertyName(methodName);
        } catch (Exception e) {
            throw new IllegalArgumentException("无法从给定的方法引用中解析字段名", e);
        }
    }

    /**
     * 将 getter/setter 方法名转换为 Java Bean 属性名。
     * e.g., "getName" -> "name", "isSuccess" -> "success"
     */
    private static String toPropertyName(String methodName) {
        Objects.requireNonNull(methodName, "方法名不能为空");

        if (methodName.startsWith("get")) {
            if (methodName.length() == 3) {
                return ""; // 纯 "get" 方法
            }
            // "getName" -> "Name" -> "name"
            return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        } else if (methodName.startsWith("is")) {
            if (methodName.length() == 2) {
                return ""; // 纯 "is" 方法
            }
            // "isActive" -> "Active" -> "active"
            return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
        } else {
            // 如果不是标准的 getter，可以考虑直接返回或抛出异常
            // 这里我们选择抛出异常，因为调用者期望的是一个属性名
            throw new IllegalArgumentException("方法 '" + methodName + "' 不是一个有效的 getter 方法");
        }
    }
}