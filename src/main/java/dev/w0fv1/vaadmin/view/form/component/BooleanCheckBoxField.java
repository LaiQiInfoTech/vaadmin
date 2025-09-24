package dev.w0fv1.vaadmin.view.form.component;

import com.vaadin.flow.component.checkbox.Checkbox;
import dev.w0fv1.vaadmin.view.form.model.BaseFormModel;

import java.lang.reflect.Field;

/**
 * BooleanCheckBoxField
 * 用于绑定Boolean类型的单选框控件。
 */
public class BooleanCheckBoxField extends BaseFormFieldComponent<Boolean> {

    private Checkbox checkBox;    // UI控件
    private Boolean data = false; // 内部持有的数据

    public BooleanCheckBoxField(Field field, BaseFormModel formModel) {
        super(field, formModel);
        super.initialize();

    }

    @Override
    void initStaticView() {
        this.checkBox = new Checkbox();
        this.checkBox.setLabel(getFormField().title());
        this.checkBox.setId(getField().getName());
        this.checkBox.setEnabled(getFormField().enabled());

        this.checkBox.addValueChangeListener(event -> {
            setData(event.getValue());
        });

        add(this.checkBox);
    }



    @Override
    public void pushViewData() {
        if (checkBox != null) {
            Boolean currentUIValue = checkBox.getValue();
            if (currentUIValue == null || !currentUIValue.equals(data)) {
                checkBox.setValue(Boolean.TRUE.equals(data));
            }
        }
    }

    @Override
    public Boolean getData() {
        return data;
    }

    @Override
    public void setInternalData(Boolean data) {
        this.data = data == null ? Boolean.FALSE : data;
    }

    @Override
    public void clearData() {
        this.data = Boolean.FALSE;
    }

    @Override
    public void clearUI() {
        if (checkBox != null) {
            checkBox.clear();
        }
    }
}
