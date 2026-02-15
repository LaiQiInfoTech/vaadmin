package dev.w0fv1.vaadmin.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Step-by-step flow component with progress bar, navigation and icons.
 */
public class StepFlowComponent extends VerticalLayout {

    /* ---------- 配置项 ---------- */


    private JustifyContentMode navMode = JustifyContentMode.CENTER;
    private boolean showProgressText = true;          // 全局开关

    /* ---------- Step 定义 ---------- */

    public abstract static class StepItem {
        private final String title;
        private Component icon;
        private String progressText;                  // 进度文案
        @Getter private String contentActionGap = "var(--lumo-space-m)";

        protected StepItem(String title) { this.title = title; }

        public abstract void build(VerticalLayout content);
        public void onShow() {}        // <-- 新增

        public boolean onNext()         { return true; }
        public void    onPrevious()     {}
        public int     nextIndex(int i) { return i + 1; }
        public boolean onFinish()       { return true; }

        /* ---- 链式配置 ---- */
        public StepItem setIcon(Component icon)          { this.icon = icon; return this; }
        public StepItem setProgressText(String text)     { this.progressText = text; return this; }
        public StepItem setContentActionGap(String gap)  { this.contentActionGap = gap; return this; }

        Component getIcon()       { return icon; }
        String    getTitle()      { return title; }
        String    getProgressText(){ return progressText; }
    }

    /* ---------- 内部元素 ---------- */

    private final List<StepItem> steps = new ArrayList<>();
    private final VerticalLayout stepContainer = new VerticalLayout();
    private final ProgressBar    progressBar   = new ProgressBar();
    private Div progressLabel;                                // 进度文案

    private int currentIndex = -1;

    /* ---------- 构造 ---------- */

    public StepFlowComponent() {
        setWidthFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);

        /* ProgressBar 初始样式 */
        progressBar.setWidthFull();
        progressBar.setMin(0);
        progressBar.getStyle()
                .set("--lumo-progress-height", "8px")
                .set("border-radius", "4px")
                .set("transition", "all 0.3s ease");

        stepContainer.setWidthFull();
        stepContainer.setPadding(false);
        stepContainer.setSpacing(false);
        stepContainer.setAlignItems(Alignment.STRETCH);
        add(stepContainer);
    }

    /* ---------- 链式 API ---------- */

    public StepFlowComponent setNavMode(JustifyContentMode mode)              { this.navMode = mode; return this; }
    public StepFlowComponent setShowProgressText(boolean show)     { this.showProgressText = show; return this; }
    public StepFlowComponent setProgressBarMaxWidth(String width)  {
        progressBar.setMaxWidth(width);
        progressBar.getStyle().set("margin-left","auto").set("margin-right","auto");
        return this;
    }
    public StepFlowComponent setProgressBarColor(String cssColor)  {
        progressBar.getStyle().set("--lumo-primary-color", cssColor);
        return this;
    }
    public StepFlowComponent setProgressHeight(String cssHeight)   {
        progressBar.getStyle().set("--lumo-progress-height", cssHeight);
        return this;
    }

    public void addStep(StepItem s) { steps.add(s); }

    public void start() {
        if (steps.isEmpty()) throw new IllegalStateException("No steps have been added");
        showStep(0);
    }

    /* ---------- 按钮工厂 ---------- */

    private Button navButton(String text, VaadinIcon iconEnum, boolean iconAfter,
                             boolean primary,
                             ComponentEventListener<ClickEvent<Button>> cl) {
        Icon icon = iconEnum.create();
        icon.getStyle().set(iconAfter ? "margin-left":"margin-right","0.25rem");
        Button btn = new Button(text, icon, cl);
        btn.setIconAfterText(iconAfter);
        btn.addThemeVariants(primary ? ButtonVariant.LUMO_PRIMARY
                : ButtonVariant.LUMO_TERTIARY);
        btn.getStyle().set("padding-left","24px").set("padding-right","24px");
        return btn;
    }

    /* ---------- 核心渲染 ---------- */

    private void showStep(int idx) {
        if (idx < 0 || idx >= steps.size()) return;
        currentIndex = idx;
        StepItem step = steps.get(idx);

        stepContainer.removeAll();

        /* ---- 标题 ---- */
        HorizontalLayout titleWrap = new HorizontalLayout();
        titleWrap.setWidthFull();
        titleWrap.setPadding(false);
        titleWrap.setSpacing(false);
        titleWrap.setJustifyContentMode(JustifyContentMode.START);

        if (step.getIcon()!=null) {
            step.getIcon().getStyle().set("margin-right","0.5rem");
            titleWrap.add(step.getIcon());
        }
        Div titleDiv = new Div(step.getTitle());
        titleDiv.getStyle().set("font-weight","600")
                .set("font-size","1.1rem")
                .set("color","var(--lumo-primary-text-color)");
        titleWrap.add(titleDiv);
        stepContainer.add(titleWrap);

        /* ---- 进度条 ---- */
        progressBar.setValue(steps.size()==1?1:(double)idx/(steps.size()-1));
        stepContainer.add(progressBar);

        /* ---- 进度文案（可选） ---- */
        if (showProgressText && step.getProgressText()!=null) {
            if (progressLabel==null) progressLabel = new Div();
            progressLabel.setText(step.getProgressText());
            progressLabel.getStyle().set("font-size","0.75rem")
                    .set("text-align","center")
                    .set("margin-top","var(--lumo-space-xs)");
            stepContainer.add(progressLabel);
        }

        /* ---- 内容 ---- */
        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        content.setPadding(false);
        content.setSpacing(false);
        content.setAlignItems(Alignment.STRETCH);
        step.build(content);
        stepContainer.add(content);
        step.onShow();                 // <-- 关键：每次显示完之后调用

        /* ---- 间隔 ---- */
        Div gap = new Div();
        gap.getStyle().set("height", step.getContentActionGap());
        stepContainer.add(gap);

        /* ---- 导航按钮 ---- */
        HorizontalLayout nav = new HorizontalLayout();
        nav.setWidthFull();
        nav.setJustifyContentMode(navMode);

        Button prev = navButton("上一步", VaadinIcon.ARROW_BACKWARD,
                false, false, e -> { step.onPrevious(); showStep(currentIndex-1); });
        prev.setEnabled(idx!=0);

        Button nextOrFinish = null;
        if (idx==steps.size()-1) {
            Button finalNextOrFinish = nextOrFinish;
            nextOrFinish = navButton("完成", VaadinIcon.CHECK,
                    true, true, e -> {
                        if (step.onFinish()) {
                            progressBar.setValue(1.0);
                            finalNextOrFinish.setEnabled(false);
                        }
                    });
        } else {
            nextOrFinish = navButton("下一步", VaadinIcon.ARROW_FORWARD,
                    true, true, e -> {
                        if (step.onNext()) showStep(step.nextIndex(currentIndex));
                    });
        }
        nav.add(prev, nextOrFinish);
        stepContainer.add(nav);
    }
}
