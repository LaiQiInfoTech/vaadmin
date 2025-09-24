package dev.w0fv1.vaadmin.view.form.component;

import dev.w0fv1.vaadmin.view.form.model.BaseFormModel;
import dev.w0fv1.vaadmin.view.TagInput;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * TagInputField
 * 多标签输入控件，绑定 List<String> 数据。
 */
@Slf4j
public class TagInputField extends BaseFormFieldComponent<List<String>> {

    private TagInput tagInput; // UI控件
    private List<String> data = new ArrayList<>(); // 内部持有数据

    public TagInputField(Field field, BaseFormModel formModel) {
        super(field, formModel);
        super.initialize();

    }

    @Override
    void initStaticView() {
        this.tagInput = new TagInput();
        this.tagInput.setEnabled(getFormField().enabled());
        this.tagInput.setOnChangeListener(tags -> {
            log.debug("getData<UNK>setOnChangeListener{}",tags);
            // 只更新内部数据，不操作UI
            setData(new ArrayList<>(tags));
        });
        add(this.tagInput);
    }

    @Override
    public void pushViewData() {

        if (getData() != null) {
            this.tagInput.setTags(getData());
        }
    }

    @Override
    public List<String> getData() {
        return new ArrayList<>(data);
    }

    @Override
    public void setInternalData(List<String> data) {
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
        if (this.tagInput != null) {
            this.tagInput.clear();
        }
    }
}
