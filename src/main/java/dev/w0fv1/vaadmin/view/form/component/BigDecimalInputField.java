package dev.w0fv1.vaadmin.view.form.component;

import com.vaadin.flow.component.textfield.NumberField;
import dev.w0fv1.vaadmin.view.form.model.BaseFormModel;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.math.BigDecimal;

/**
 * BigDecimalInputField
 * 用于绑定 BigDecimal 类型的数值输入框控件。
 */
@Slf4j
public class BigDecimalInputField extends BaseFormFieldComponent<BigDecimal> {

    private NumberField numberField;     // UI控件
    private BigDecimal data = BigDecimal.ZERO; // 内部持有的数据

    public BigDecimalInputField(Field field, BaseFormModel formModel) {
        super(field, formModel);
        super.initialize();

    }

    @Override
    void initStaticView() {
        this.numberField = new NumberField();
        this.numberField.setId(getField().getName());
        this.numberField.setPlaceholder("请输入 " + getFormField().title());
        this.numberField.setWidthFull();
        this.numberField.setEnabled(getFormField().enabled());
        this.numberField.setStep(0.01); // 小数步长

        this.numberField.addValueChangeListener(event -> {
            Double value = event.getValue();
            if (value != null) {
                setData(BigDecimal.valueOf(value));
            } else {
                setData(null);
            }
        });

        add(this.numberField);
    }



    @Override
    public void pushViewData() {
        if (numberField != null) {
            if (data == null) {
                numberField.clear();
            } else {
                Double currentUIValue = numberField.getValue();
                if (currentUIValue == null || BigDecimal.valueOf(currentUIValue).compareTo(data) != 0) {
                    numberField.setValue(data.doubleValue());
                }
            }
        }
    }

    @Override
    public BigDecimal getData() {
        return data;
    }

    @Override
    public void setInternalData(BigDecimal data) {
        this.data = (data == null) ? BigDecimal.ZERO : data;
    }

    @Override
    public void clearData() {
        this.data = BigDecimal.ZERO;
    }

    @Override
    public void clearUI() {
        if (numberField != null) {
            numberField.clear();
        }
    }
}
