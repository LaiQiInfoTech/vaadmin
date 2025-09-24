package dev.w0fv1.vaadmin.view.form.component;

import com.vaadin.flow.component.textfield.NumberField;
import dev.w0fv1.vaadmin.view.form.model.BaseFormModel;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

/**
 * LongIdField
 * 用于只读显示Long类型ID字段，绑定Long数据。
 */
@Slf4j
public class LongIdField extends BaseFormFieldComponent<Long> {

    private NumberField numberField; // UI控件
    private Long data = null;         // 内部持有的数据

    public LongIdField(Field field, BaseFormModel formModel) {
        super(field, formModel);
        super.initialize();

    }

    @Override
    void initStaticView() {
        this.numberField = new NumberField();
        this.numberField.setId(getField().getName());
        this.numberField.setPlaceholder("请输入 " + getFormField().title());
        this.numberField.setWidthFull();
        this.numberField.setStep(1); // 步长为整数

        this.numberField.setEnabled(getFormField().enabled());
        this.numberField.setReadOnly(true); // ID字段默认只读，防止误操作

        this.numberField.addValueChangeListener(event -> {
            Double value = event.getValue();
            if (value != null) {
                setData(value.longValue());
            } else {
                setData(null);
            }
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
                if (currentUIValue == null || !currentUIValue.equals(data.doubleValue())) {
                    numberField.setValue(data.doubleValue());
                }
            }
        }
    }

    @Override
    public Long getData() {
        return data;
    }

    @Override
    public void setInternalData(Long data) {
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
