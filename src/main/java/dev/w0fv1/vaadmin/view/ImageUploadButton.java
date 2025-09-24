package dev.w0fv1.vaadmin.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;

import java.util.Base64;

import static dev.w0fv1.vaadmin.view.tools.Notifier.showNotification;

public class ImageUploadButton<T> extends Div {

    private String initImageUrl;
    private final Div imageContainer;
    private final Button uploadButton;
    private final Button applyButton;
    private final Upload upload;

    // 用于暂存 handleUploadSucceeded 返回的结果
    private T uploadResult;

    public ImageUploadButton(String initImageUrl, ImageUploadHandler<T> handler) {
        this.initImageUrl = initImageUrl;

        FlexLayout layout = new FlexLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        // ===== 1. 预览图容器 =====
        imageContainer = new Div();
        imageContainer.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("width", "150px")
                .set("height", "100px")
                .set("border", "1px solid #ccc")
                .set("border-radius", "8px")
                .set("cursor", "pointer")
                .set("position", "relative")
                .set("overflow", "hidden")
                .set("background-size", "cover")
                .set("background-position", "center");

        if (initImageUrl != null && !initImageUrl.isEmpty()) {
            imageContainer.getStyle().set("background-image", "url('" + initImageUrl + "')");
        } else {
            imageContainer.getStyle().set("background-color", "#f0f0f0").set("color", "#555");
        }

        // ===== 2. 使用 UploadHandler 替代 MemoryBuffer =====
        UploadHandler inMemoryHandler = UploadHandler.inMemory((meta, bytes) -> {
            showNotification("Uploaded: " + meta.fileName(), NotificationVariant.LUMO_CONTRAST);

            String base64Image = handler.getBase64(bytes, meta.contentType());
            imageContainer.getStyle().set("background-image", "url('" + base64Image + "')");

            uploadResult = handler.handleUploadSucceeded(bytes, meta.contentType(), meta.fileName());
        });

        upload = new Upload(inMemoryHandler);
        upload.setAcceptedFileTypes("image/*");
        upload.setDropAllowed(false);

        uploadButton = new Button("上传图片");
        uploadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        upload.setUploadButton(uploadButton);

        upload.addAllFinishedListener(event -> {
            uploadButton.setText("重新上传");
            uploadButton.setEnabled(true);
            upload.clearFileList();
        });

        upload.getElement()
                .addEventListener("max-files-reached-changed", e -> {
                    boolean maxFilesReached = e.getEventData().getBoolean("event.detail.value");
                    uploadButton.setEnabled(!maxFilesReached);
                })
                .addEventData("event.detail.value");

        // ===== 3. 应用按钮 =====
        applyButton = new Button("应用");
        applyButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        applyButton.getStyle().set("margin-left", "5px");

        applyButton.addClickListener(e -> {
            if (uploadResult != null) {
                handler.apply(uploadResult);
                showNotification("应用成功！", NotificationVariant.LUMO_SUCCESS);
            } else {
                showNotification("还没有任何可应用的数据！", NotificationVariant.LUMO_WARNING);
            }
        });

        Div spacer = new Div(" ");
        spacer.setWidth("10px");

        layout.add(imageContainer, spacer, upload, applyButton);
        add(layout);
    }

    public void setInitImageUrl(String initImageUrl) {
        this.initImageUrl = initImageUrl;
        if (initImageUrl != null && !initImageUrl.isEmpty()) {
            imageContainer.getStyle().set("background-image", "url('" + initImageUrl + "')");
        } else {
            imageContainer.getStyle().remove("background-image");
        }
    }

    // ===== 新接口：使用 byte[] 替代 MemoryBuffer =====
    public interface ImageUploadHandler<T> {

        /**
         * 处理上传成功后接收到的字节内容
         */
        T handleUploadSucceeded(byte[] fileContent, String mimeType, String fileName);

        /**
         * 点击“应用”时的业务逻辑
         */
        void apply(T data);

        /**
         * 默认的 base64 工具
         */
        default String getBase64(byte[] bytes, String mimeType) {
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        }
    }
}
