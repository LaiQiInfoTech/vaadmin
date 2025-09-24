package dev.w0fv1.vaadmin.view.form.component;

import com.vaadin.flow.component.textfield.TextField;
import dev.w0fv1.vaadmin.view.form.model.BaseFormModel;

import java.lang.reflect.Field;

/**
 * SampleFormFieldComponent
 * 定制化Form组件示例，绑定 String 类型数据。
 * 严格遵循 BaseFormFieldComponent 设计规范。
 */
public class SampleFormFieldComponent extends BaseFormFieldComponent<String> {

    private TextField textField; // UI控件
    private String data = "";    // 内部持有数据

    public SampleFormFieldComponent(Field field, BaseFormModel formModel) {
        super(field, formModel);
        super.initialize();

    }

    @Override
    void initStaticView() {
        this.textField = new TextField();
        this.textField.setId(getField().getName()); // 设置唯一Field ID
        this.textField.setPlaceholder("请输入 " + getFormField().title() + "（这是一个定制化Form组件示例）");
        this.textField.setWidthFull();
        this.textField.setEnabled(getFormField().enabled());

        this.textField.addValueChangeListener(event -> {
            // 只更新数据，不直接操作界面
            setData(event.getValue());
        });

        add(this.textField);
    }



    @Override
    public void pushViewData() {
        if (textField != null) {
            if (data == null || data.isEmpty()) {
                textField.clear();
            } else if (!data.equals(textField.getValue())) {
                textField.setValue(data);
            }
        }
    }

    @Override
    public String getData() {
        return data;
    }

    @Override
    public void setInternalData(String data) {
        this.data = data == null ? "" : data;
    }

    @Override
    public void clearData() {
        this.data = "";
    }

    @Override
    public void clearUI() {
        if (textField != null) {
            textField.clear();
        }
    }
}
