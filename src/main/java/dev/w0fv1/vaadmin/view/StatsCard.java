package dev.w0fv1.vaadmin.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * 统计卡片组件。
 *
 * 创建时传入图标、标题、数据获取回调以及刷新间隔；组件 attach 后自动启动定时刷新任务，
 * 定时调用回调获取最新字符串并展示。
 *
 * 内部用守护线程池统一调度，UI 更新通过 {@link UI#access} 保证线程安全。
 */
public class StatsCard extends Composite<Div> {

    /** 所有卡片共用的守护线程池，避免为每个实例创建线程。 */
    private static final ScheduledExecutorService EXECUTOR =
            Executors.newScheduledThreadPool(
                    Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
                    r -> {
                        Thread t = new Thread(r, "StatsCard-Refresher");
                        t.setDaemon(true);
                        return t;
                    });

    private final Supplier<String> dataSupplier;
    private final Duration refreshInterval;
    private ScheduledFuture<?> task;
    private final Span valueSpan = new Span();

    /**
     * @param icon            左侧图标
     * @param title           标题
     * @param dataSupplier    数据获取回调；返回值直接显示在卡片
     * @param refreshInterval 刷新间隔
     */
    public StatsCard(Icon icon,
                     String title,
                     Supplier<String> dataSupplier,
                     Duration refreshInterval) {

        this.dataSupplier    = Objects.requireNonNull(dataSupplier, "数据回调不能为空");
        this.refreshInterval = Objects.requireNonNull(refreshInterval, "刷新间隔不能为空");

        /* -------- 构建 UI -------- */
        HorizontalLayout header = new HorizontalLayout(icon, new Span(title));
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassNames(LumoUtility.FontWeight.SEMIBOLD,
                LumoUtility.FontSize.LARGE);

        valueSpan.addClassNames(LumoUtility.FontSize.XXXLARGE,
                LumoUtility.FontWeight.BOLD);

        VerticalLayout content = new VerticalLayout(header, valueSpan);
        content.setPadding(false);
        content.setSpacing(false);
        content.setAlignItems(FlexComponent.Alignment.START);

        Div root = getContent();
        root.add(content);
        root.addClassNames(
                LumoUtility.Border.ALL,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Padding.LARGE,
                LumoUtility.BoxShadow.SMALL
                /* ❌ 取消原先 LumoUtility.Background.CONTRAST，避免暗色模式下出现黑底 */
        );

        /* ✅ 统一使用 5 % 对比度背景：在亮色主题是浅灰，在暗色主题是稍亮的灰，保证文字永远可读 */
        root.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
    }

    /* ---------- 生命周期钩子 ---------- */

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        // UI 挂载时启动定时任务
        task = EXECUTOR.scheduleAtFixedRate(
                this::refresh,
                0,
                refreshInterval.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onDetach(DetachEvent event) {
        super.onDetach(event);
        // UI 卸载时停止任务，防内存泄漏
        if (task != null) {
            task.cancel(true);
        }
    }

    /* ---------- 内部方法 ---------- */

    /** 定时线程回调，负责取数并刷到 UI。 */
    private void refresh() {
        UI ui = getUI().orElse(null);
        if (ui == null) return; // UI 已销毁

        try {
            String value = dataSupplier.get();
            ui.access(() -> valueSpan.setText(value));
        } catch (Exception ex) {
            ui.access(() -> valueSpan.setText("ERR"));
            ex.printStackTrace();
        }
    }
}
