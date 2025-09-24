package dev.w0fv1.vaadmin.view.form.component;

import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import dev.w0fv1.vaadmin.view.form.model.BaseFormModel;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * DateTimeField
 * 绑定 OffsetDateTime 类型的时间选择控件。
 */
@Slf4j
public class DateTimeField extends BaseFormFieldComponent<OffsetDateTime> {

    private DateTimePicker dateTimePicker; // UI控件
    private OffsetDateTime data;            // 内部持有的数据

    public DateTimeField(Field field, BaseFormModel formModel) {
        super(field, formModel);
        super.initialize();

    }

    @Override
    void initStaticView() {
        this.dateTimePicker = new DateTimePicker();
        this.dateTimePicker.setId(getField().getName());
        this.dateTimePicker.setStep(Duration.ofMinutes(30)); // 步长30分钟
        this.dateTimePicker.setWidthFull();
        this.dateTimePicker.setEnabled(getFormField().enabled());

        this.dateTimePicker.addValueChangeListener(event -> {
            LocalDateTime localDateTime = event.getValue();
            if (localDateTime == null) {
                setData(null);
            } else {
                setData(localDateTime.atOffset(ZoneOffset.UTC));
            }
        });

        add(this.dateTimePicker);
    }



    @Override
    public void pushViewData() {
        if (dateTimePicker != null) {
            if (data == null) {
                dateTimePicker.clear();
            } else {
                LocalDateTime uiValue = dateTimePicker.getValue();
                LocalDateTime newValue = data.toLocalDateTime();
                if (uiValue == null || !uiValue.equals(newValue)) {
                    dateTimePicker.setValue(newValue);
                }
            }
        }
    }

    @Override
    public OffsetDateTime getData() {
        return data;
    }

    @Override
    public void setInternalData(OffsetDateTime data) {
        if (data == null) {
            data = OffsetDateTime.now();
        }
        this.data = data;
    }

    @Override
    public void clearData() {
        setData(null);
    }

    @Override
    public void clearUI() {
        if (dateTimePicker != null) {
            dateTimePicker.clear();
        }
    }
}
