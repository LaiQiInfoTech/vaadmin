package dev.w0fv1.vaadmin.test;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import dev.w0fv1.vaadmin.view.ListBoxGroup;
import dev.w0fv1.vaadmin.view.TextListBox;
import dev.w0fv1.vaadmin.view.UpdateTableButton;
import lombok.extern.slf4j.Slf4j;

/**
 * Dev (开发中) 页面 —— 展示 {@link ListBoxGroup} + {@link TextListBox} 组合使用的示例。
 */
@Slf4j
@Route(value = "/dev", layout = MainView.class)
public class DevView extends VerticalLayout {

    public DevView() {
        add(new H1("Dev Playground"));

        UpdateTableButton uploadBtn = new UpdateTableButton(
                // onSuccess：如果想把原文件落库，可在此持久化 bytes
                (fileName, bytes) -> log.info("上传成功：{} ({} bytes)", fileName, bytes.length),

                // onComplete：拿到解析后的 Table
                (UpdateTableButton.Table table) -> {
                    log.info("解析完成：{} 行, {} 列", table.getRowCount(), table.getColumnCount());
                    log.info("表数据：{}", table);
                    Notification.show("已解析 " + table.getRowCount() + " 行", 3000, Notification.Position.TOP_CENTER);
                },

                // onError
                throwable -> Notification.show("文件解析失败：" + throwable.getMessage(), 5000, Notification.Position.TOP_CENTER)
        );

        add(uploadBtn);
    }
}
