package dev.w0fv1.vaadmin.view.form.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;
import dev.w0fv1.vaadmin.view.form.model.BaseFormModel;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Base64;

/**
 * FileUploadFieldComponent
 * 文件上传表单字段基类，绑定自定义的数据类型。
 *
 * 设计要求：
 * - 控件初始化放在 initStaticView；
 * - 内部持有数据；
 * - 数据与UI分离；
 * - 支持幂等 pushViewData。
 */
@Slf4j
public abstract class BaseFileUploadFieldComponent<Type> extends BaseFormFieldComponent<Type> {

    private Upload upload;       // 上传控件
    private Button uploadButton; // 上传按钮

    private Type data; // 内部持有数据

    public BaseFileUploadFieldComponent(Field field, BaseFormModel formModel) {
        super(field, formModel);
    }

    @Override
    public void initStaticView() {
        this.uploadButton = new Button("上传" + this.getFormField().title());

        // 新版 UploadHandler，内存接收器
        UploadHandler handler = UploadHandler.inMemory((UploadMetadata meta, byte[] fileContent) -> {
            log.info("Uploaded file: {}, size: {}", meta.fileName(), fileContent.length);

            // 子类实现该方法处理数据
            handleUploadSucceeded(fileContent, meta.contentType(), meta.fileName());

            // 更新按钮状态
            pushViewData();
        });

        this.upload = new Upload(handler);
        this.upload.setUploadButton(uploadButton);
        this.upload.setWidthFull();
        this.upload.setEnabled(getFormField().enabled());

        add(this.upload);
    }

    @Override
    public void pushViewData() {
        if (uploadButton != null) {
            if (data != null) {
                uploadButton.setText("已上传文件 (点击重新上传)");
            } else {
                uploadButton.setText("上传" + this.getFormField().title());
            }
        }
    }

    @Override
    public Type getData() {
        return data;
    }

    @Override
    public void setInternalData(Type data) {
        this.data = data;
    }

    @Override
    public void clearData() {
        this.data = null;
    }

    @Override
    public void clearUI() {
        if (upload != null) {
            upload.clearFileList();
            if (uploadButton != null) {
                uploadButton.setText("上传" + this.getFormField().title());
            }
        }
    }

    /**
     * 子类必须实现：处理上传成功后的文件。
     * 将文件字节数据绑定为指定类型。
     */
    public abstract void handleUploadSucceeded(byte[] fileContent, String mimeType, String fileName);

    /**
     * 可选工具：转为 base64 数据 URL
     */
    protected String toBase64Image(byte[] content, String mimeType) {
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(content);
    }
}
