package dev.w0fv1.vaadmin.view.table.model;

import dev.w0fv1.vaadmin.GenericRepository;
import dev.w0fv1.vaadmin.entity.BaseManageEntity;
import dev.w0fv1.vaadmin.view.form.model.BaseEntityFormModel;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public interface BaseEntityTableModel<
        E extends BaseManageEntity<ID>,
        ID
        > extends BaseManageEntity<ID>, BaseTableModel {

    ID getId();

    void setId(ID id);

    BaseEntityFormModel<E, ID> toFormModel();

    default BaseEntityFormModel<E, ID> toFormModel(BaseEntityFormModel<E, ID> formModel) {
        BaseEntityFormModel<E, ID> newFormModel = this.toFormModel();
        newFormModel.loadConfig(formModel);
        return newFormModel;
    }

    void formEntity(E entity);

    @SuppressWarnings("unchecked")
    default Class<E> getEntityClass() {
        // 获取实现类的泛型接口
        Type[] genericInterfaces = this.getClass().getGenericInterfaces();

        for (Type type : genericInterfaces) {
            if (type instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) type;
                Type rawType = pType.getRawType();

                if (rawType instanceof Class && BaseEntityTableModel.class.isAssignableFrom((Class<?>) rawType)) {
                    Type actualType = pType.getActualTypeArguments()[0];
                    if (actualType instanceof Class<?>) {
                        return (Class<E>) actualType;
                    } else if (actualType instanceof ParameterizedType) {
                        return (Class<E>) ((ParameterizedType) actualType).getRawType();
                    }
                }
            }
        }

        // 如果未在直接实现的接口中找到，尝试检查父类
        Type genericSuperclass = this.getClass().getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) genericSuperclass;
            Type actualType = pType.getActualTypeArguments()[0];
            if (actualType instanceof Class<?>) {
                return (Class<E>) actualType;
            }
        }

        throw new RuntimeException("无法确定泛型类型E的Class对象");
    }


}

