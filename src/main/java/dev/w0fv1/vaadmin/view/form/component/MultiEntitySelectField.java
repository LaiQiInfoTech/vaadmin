package dev.w0fv1.vaadmin.view.form.component;

import dev.w0fv1.vaadmin.GenericRepository;
import dev.w0fv1.vaadmin.entity.BaseManageEntity;
import dev.w0fv1.vaadmin.view.EntitySelectButton;
import dev.w0fv1.vaadmin.view.form.model.BaseEntityFormModel;
import dev.w0fv1.vaadmin.view.form.model.FormField;
import dev.w0fv1.vaadmin.view.form.model.FormEntitySelectField;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * MultiEntitySelectField
 * 多选实体选择器，绑定 {@code List<ID>} 类型数据。
 */
@Slf4j
public class MultiEntitySelectField<E extends BaseManageEntity<ID>, ID> extends BaseFormFieldComponent<List<ID>> {

    private EntitySelectButton<E, ID> entitySelectButton; // UI控件
    private List<ID> data = new ArrayList<>();            // 内部持有数据
    private final GenericRepository genericRepository;

    public MultiEntitySelectField(Field field, BaseEntityFormModel<E, ID> formModel, GenericRepository genericRepository) {
        super(field, formModel, true);
        this.genericRepository = genericRepository;
        super.initialize();
    }

    @Override
    void initStaticView() {
        FormField formField = getFormField();
        FormEntitySelectField formEntitySelectField = getField().getAnnotation(FormEntitySelectField.class);

        this.entitySelectButton = new EntitySelectButton<>(
                "选择" + formField.title(),
                (Class<E>) formEntitySelectField.entityField().entityType(),
                false // 这里明确是多选
        );
        this.entitySelectButton.setEnabled(formField.enabled());

        this.entitySelectButton.setGenericRepository(
                this.genericRepository,
                formEntitySelectField.enablePredicate() ? ((BaseEntityFormModel) getFormModel()).getEntityPredicateBuilder() : null, null, false);

        this.entitySelectButton.setOnValueChangeListener(selectedIds -> {
            setData(new ArrayList<>(selectedIds));
        });

        add(this.entitySelectButton);
    }


    @Override
    public void pushViewData() {
        log.info("pushViewData:{}pushViewData");

        if (entitySelectButton != null) {
            if (data == null || data.isEmpty()) {
                entitySelectButton.clear();
            } else {
                log.info("data:{}", data);
                entitySelectButton.setValue(new ArrayList<>(data));
            }
        }
    }

    @Override
    public List<ID> getData() {
        return data;
    }

    @Override
    public void setInternalData(List<ID> data) {
        this.data.clear();
        if (data != null) {
            this.data.addAll(data);
        }
    }

    @Override
    public void clearData() {
        this.data.clear();
    }

    @Override
    public void clearUI() {
        if (entitySelectButton != null) {
            entitySelectButton.clear();
        }
    }
}
