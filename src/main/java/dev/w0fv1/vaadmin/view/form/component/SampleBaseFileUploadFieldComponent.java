package dev.w0fv1.vaadmin.view.form.component;

import dev.w0fv1.vaadmin.view.form.model.BaseFormModel;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

/**
 * SampleFileUploadFieldComponent
 * 示例文件上传字段，绑定上传后的文件名（String）。
 */
@Slf4j
public class SampleBaseFileUploadFieldComponent extends BaseFileUploadFieldComponent<String> {

    private String url; // 上传后的文件名或地址

    public SampleBaseFileUploadFieldComponent(Field field, BaseFormModel formModel) {
        super(field, formModel);
        super.initialize();
    }

    @Override
    public void handleUploadSucceeded(byte[] fileContent, String mimeType, String fileName) {
        log.info("上传成功，文件名：{}", fileName);
        setData(fileName);
        pushViewData();
    }


    @Override
    public String getData() {
        return url;
    }

    @Override
    public void setInternalData(String data) {
        this.url = data;
    }

    @Override
    public void clearData() {
        this.url = null;
    }

    @Override
    public void clearUI() {
        super.clearUI(); // 调用父类的upload.clearFileList()
    }

    /**
     * Builder类，用于创建SampleFileUploadFieldComponent。
     */
    public static class SampleFileUploadFieldComponentBuilder implements CustomFormFieldComponentBuilder {

        @Override
        public BaseFormFieldComponent<?> build(Field field, BaseFormModel formModel) {
            return new SampleBaseFileUploadFieldComponent(field, formModel);
        }
    }
}
