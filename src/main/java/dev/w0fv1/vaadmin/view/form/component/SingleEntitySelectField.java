package dev.w0fv1.vaadmin.view.form.component;

import dev.w0fv1.vaadmin.GenericRepository;
import dev.w0fv1.vaadmin.entity.BaseManageEntity;
import dev.w0fv1.vaadmin.view.EntitySelectButton;
import dev.w0fv1.vaadmin.view.form.model.BaseEntityFormModel;
import dev.w0fv1.vaadmin.view.form.model.BaseFormModel;
import dev.w0fv1.vaadmin.view.form.model.FormEntitySelectField;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * SingleEntitySelectField
 * 实体选择按钮组件，绑定单个实体ID。
 */
@Slf4j
public class SingleEntitySelectField<E extends BaseManageEntity<ID>, ID> extends BaseFormFieldComponent<ID> {

    private EntitySelectButton<E, ID> entitySelectButton; // UI控件
    private ID data; // 内部持有的数据
    private final GenericRepository genericRepository;

    public SingleEntitySelectField(Field field, BaseEntityFormModel<E, ID> formModel, GenericRepository genericRepository) {
        super(field, formModel, true);
        this.genericRepository = genericRepository;
        super.initialize();

    }

    @Override
    void initStaticView() {
        FormEntitySelectField formEntitySelectField = getField().getAnnotation(FormEntitySelectField.class);
        String title = getFormField().title();
        boolean isSingle = !getField().getType().equals(List.class);

        this.entitySelectButton = new EntitySelectButton<>(
                "选择" + title,
                (Class<E>) formEntitySelectField.entityField().entityType(),
                isSingle
        );
        this.entitySelectButton.setEnabled(getFormField().enabled());
        this.entitySelectButton.setGenericRepository(this.genericRepository,
                formEntitySelectField.enablePredicate() ?

                        ((BaseEntityFormModel) getFormModel()).getEntityPredicateBuilder() : null, null, false);
        add(this.entitySelectButton);

        this.entitySelectButton.setOnValueChangeListener(selected -> {
            if (selected == null || selected.isEmpty()) {
                setData(null);
            } else {
                setData(selected.getFirst());
            }
        });
    }


    @Override
    public void pushViewData() {
        if (entitySelectButton != null) {
            if (data == null) {
                entitySelectButton.clear();
            } else {
                List<ID> list = new ArrayList<>();
                list.add(data);
                entitySelectButton.setValue(list);
            }
        }
    }

    @Override
    public ID getData() {
        return data;
    }

    @Override
    public void setInternalData(ID data) {
        this.data = data;
    }

    @Override
    public void clearData() {
        this.data = null;
    }

    @Override
    public void clearUI() {
        if (entitySelectButton != null) {
            entitySelectButton.clear();
        }
    }
}
