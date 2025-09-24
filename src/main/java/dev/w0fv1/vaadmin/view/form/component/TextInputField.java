package dev.w0fv1.vaadmin.view.form.component;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.textfield.TextField;
import dev.w0fv1.vaadmin.view.form.model.BaseFormModel;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

/**
 * TextInputField
 * 单行文本输入框，绑定 String 数据。
 */
@Slf4j
public class TextInputField extends BaseFormFieldComponent<String> {

    private TextField textField; // UI控件
    private String data = ""; // 内部持有数据

    public TextInputField(Field field, BaseFormModel formModel) {
        super(field, formModel);
        super.initialize();
    }

    @Override
    void initStaticView() {
        this.textField = new TextField();
        this.textField.setId(getField().getName());
        this.textField.setPlaceholder("请输入 " + getFormField().title());
        this.textField.setEnabled(getFormField().enabled());
        this.textField.setWidthFull();
        this.textField.setReadOnly(!getFormField().enabled()); // 只有enabled=false时才设置readonly=true

        this.textField.addValueChangeListener(e -> {
            String oldValue = getData();
            String newValue = e.getValue();
            logDebug("TextField值变化: [{}] -> [{}]", oldValue, newValue);
            // 只更新内部数据，不直接控制UI
            setData(e.getValue());
        });

        add(this.textField);

        logDebug("初始化 TextField 完成，placeholder='{}'", this.textField.getPlaceholder());
    }

    @Override
    public void pushViewData() {
        if (data != null && !data.equals(textField.getValue())) {
            logDebug("pushViewData：将内部 data [{}] 推送到 TextField，当前 TextField 值 [{}]", data, textField.getValue());
            this.textField.setValue(data);
        } else {
            logDebug("pushViewData：数据未变化，跳过刷新");
        }
    }

    @Override
    public String getData() {
        return data;
    }

    @Override
    public void setInternalData(String data) {
        String oldData = this.data;
        this.data = data == null ? "" : data;
        logDebug("setData：由 [{}] 更新为 [{}]", oldData, this.data);
    }

    @Override
    public void clearData() {
        logDebug("clearData：内部 data 清空（原值为 [{}]）", data);
        this.data = "";
    }

    @Override
    public void clearUI() {
        if (this.textField != null) {
            logDebug("clearUI：清空 TextField（当前值为 [{}]）", this.textField.getValue());
            this.textField.clear();
        }
    }

}
