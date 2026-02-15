package dev.w0fv1.vaadmin.view.form.component;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import dev.w0fv1.vaadmin.view.form.model.BaseFormModel;
import dev.w0fv1.vaadmin.view.form.model.FormField;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * MultiEnumSelectField
 * 多选枚举下拉框，绑定 {@code List<Enum<?>>} 类型数据。
 */
public class MultiEnumSelectField extends BaseFormFieldComponent<List<Enum<?>>> {

    private MultiSelectComboBox<Enum<?>> multiSelectComboBox; // UI控件
    private List<Enum<?>> data = new ArrayList<>(); // 内部持有数据

    public MultiEnumSelectField(Field field, BaseFormModel formModel) {
        super(field, formModel);
        super.initialize();

    }

    @Override
    void initStaticView() {
        FormField formField = getFormField();
        Class<?> subType = formField.subType();

        this.multiSelectComboBox = new MultiSelectComboBox<>();
        this.multiSelectComboBox.setItems((Enum<?>[]) subType.getEnumConstants());
        this.multiSelectComboBox.setPlaceholder("请选择 " + formField.title());
        this.multiSelectComboBox.setId(getField().getName());
        this.multiSelectComboBox.setWidthFull();
        this.multiSelectComboBox.setEnabled(formField.enabled());

        this.multiSelectComboBox.addValueChangeListener(event -> {
            setData(new ArrayList<>(event.getValue()));
        });

        add(this.multiSelectComboBox);
    }



    @Override
    public void pushViewData() {
        if (multiSelectComboBox != null) {
            if (data == null || data.isEmpty()) {
                multiSelectComboBox.clear();
            } else {
                // 只有不一致时才set
                if (!new ArrayList<>(multiSelectComboBox.getValue()).equals(data)) {
                    multiSelectComboBox.setValue(new ArrayList<>(data));
                }
            }
        }
    }

    @Override
    public List<Enum<?>> getData() {
        return data;
    }

    @Override
    public void setInternalData(List<Enum<?>> data) {
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
        if (this.multiSelectComboBox != null) {
            this.multiSelectComboBox.clear();
        }
    }
}
