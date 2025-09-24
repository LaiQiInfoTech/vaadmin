package dev.w0fv1.vaadmin.view.form.component;

import com.vaadin.flow.component.textfield.NumberField;
import dev.w0fv1.vaadmin.view.form.model.BaseFormModel;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

/**
 * IdField
 * 用于显示Double类型的ID字段，只读模式。
 */
@Slf4j
public class IdField extends BaseFormFieldComponent<Double> {

    private NumberField numberField; // UI控件
    private Double data = null;       // 内部持有数据

    public IdField(Field field, BaseFormModel formModel) {
        super(field, formModel);
        super.initialize();

    }

    @Override
    void initStaticView() {
        this.numberField = new NumberField();
        this.numberField.setId(getField().getName());
        this.numberField.setPlaceholder("请输入 " + getFormField().title());
        this.numberField.setWidthFull();
        this.numberField.setStep(1.0);
        this.numberField.setEnabled(getFormField().enabled());
        this.numberField.setReadOnly(true); // ID默认只读

        this.numberField.addValueChangeListener(event -> {
            Double value = event.getValue();
            setData(value);
        });

        add(this.numberField);
    }



    @Override
    public void pushViewData() {
        if (this.numberField != null) {
            if (data == null) {
                numberField.clear();
            } else {
                Double currentUIValue = numberField.getValue();
                if (currentUIValue == null || !currentUIValue.equals(data)) {
                    numberField.setValue(data);
                }
            }
        }
    }

    @Override
    public Double getData() {
        return data;
    }

    @Override
    public void setInternalData(Double data) {
        this.data = data;
    }

    @Override
    public void clearData() {
        this.data = null;
    }

    @Override
    public void clearUI() {
        if (this.numberField != null) {
            this.numberField.clear();
        }
    }
}
