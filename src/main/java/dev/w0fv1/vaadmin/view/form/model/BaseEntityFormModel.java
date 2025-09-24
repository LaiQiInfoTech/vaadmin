package dev.w0fv1.vaadmin.view.form.model;

import dev.w0fv1.vaadmin.GenericRepository;
import dev.w0fv1.vaadmin.entity.BaseManageEntity;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class BaseEntityFormModel<E extends BaseManageEntity<ID>, ID> extends BaseFormModel {
    public abstract ID getId();

    public abstract void setId(ID id);

    public abstract E toEntity();

    public abstract void translate(E model);


    public Class<E> getEntityClass() {
        try {
            Method method = this.getClass().getMethod("toEntity");
            Type returnType = method.getGenericReturnType();
            if (returnType instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) returnType;
                Type actualType = pType.getActualTypeArguments()[0];
                if (actualType instanceof Class<?>) {
                    return (Class<E>) actualType;
                }
            } else if (returnType instanceof Class<?>) {
                return (Class<E>) returnType;
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("无法确定E的类型");
    }


    public GenericRepository.PredicateBuilder<E> getEntityPredicateBuilder() {
        return null;
    }
}
