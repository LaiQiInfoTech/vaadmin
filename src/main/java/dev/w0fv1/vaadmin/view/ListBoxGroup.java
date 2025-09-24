package dev.w0fv1.vaadmin.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A draggable group of {@link ListBox ListBoxes} that can be laid out either horizontally
 * or vertically. Each individual ListBox can also render its items either vertically (default)
 * or horizontally via {@link ItemOrientation}.
 * <p>
 * The container automatically adapts its <b>cross-axis</b> dimension:
 * <ul>
 *   <li>If the group is <b>HORIZONTAL</b>, its height is always the tallest ListBox.</li>
 *   <li>If the group is <b>VERTICAL</b>, its width is always the widest ListBox.</li>
 * </ul>
 *
 * @param <T> item type of the underlying {@link ListBox} instances
 */
public class ListBoxGroup<T> extends Scroller {

    /* --------------------------------------------------
     * Public enums
     * -------------------------------------------------- */

    /**
     * Layout direction of the ListBoxGroup itself.
     */
    public enum Orientation {HORIZONTAL, VERTICAL}

    /**
     * Layout direction of the items *inside* each ListBox.
     */
    public enum ItemOrientation {HORIZONTAL, VERTICAL}

    /* --------------------------------------------------
     * Functional interface
     * -------------------------------------------------- */
    @FunctionalInterface
    public interface OnOrderChangeListener<T> {
        void onOrderChange(List<T> newOrder);
    }

    /* --------------------------------------------------
     * Fields
     * -------------------------------------------------- */
    private final List<ListBox<T>> listBoxes = new ArrayList<>();

    private final Orientation orientation;
    private ItemOrientation itemOrientation = ItemOrientation.VERTICAL;

    private Button addButton;
    private FlexComponent listBoxLayout;
    private FlexComponent wrapper;
    private OnOrderChangeListener<T> orderChangeListener;

    private boolean addBtnSizeSet = false;

    /* --------------------------------------------------
     * Constructors
     * -------------------------------------------------- */
    public ListBoxGroup() {
        this(Orientation.HORIZONTAL);
    }

    public ListBoxGroup(Orientation orientation) {
        super();
        this.orientation = orientation;
        build();
    }

    /* --------------------------------------------------
     * Public API
     * -------------------------------------------------- */

    /**
     * Sets how the items inside each {@link ListBox} should be laid out.
     */
    public void setItemOrientation(ItemOrientation itemOrientation) {
        this.itemOrientation = itemOrientation;
        listBoxes.forEach(this::applyItemOrientation);
    }

    /**
     * Adds a new ListBox to the group and wires up drag-and-drop logic plus delete button.
     */
    public void addListBox(ListBox<T> listBox) {
        if (!addBtnSizeSet) {
            if (orientation == Orientation.VERTICAL) {
                addButton.setWidth(listBox.getPreferredWidth());
            } else {
                addButton.setHeight(listBox.getPreferredHeight());
            }
            addBtnSizeSet = true;
        }

        applyItemOrientation(listBox);
        addDeleteButton(listBox);

        listBoxes.add(listBox);
        setupDragAndDrop(listBox);

        listBoxLayout.add(listBox);
        adjustCrossAxisSize();
        triggerOrderChange();
    }

    /**
     * Removes the given ListBox from the group.
     */
    public void removeListBox(ListBox<T> listBox) {
        listBoxes.remove(listBox);
        listBoxLayout.remove(listBox);
        adjustCrossAxisSize();
        triggerOrderChange();
    }

    /**
     * Removes every ListBox from the group.
     */
    public void removeAllListBoxes() {
        listBoxes.clear();
        listBoxLayout.removeAll();
        adjustCrossAxisSize();
        triggerOrderChange();
    }

    /**
     * Returns the ListBox values in current order.
     */
    public List<T> getData() {
        List<T> values = new ArrayList<>();
        listBoxes.forEach(lb -> values.add(lb.getValue()));
        return values;
    }

    /**
     * Registers a callback that is invoked every time the ListBox order changes.
     */
    public void setOnOrderChangeListener(OnOrderChangeListener<T> listener) {
        this.orderChangeListener = listener;
    }

    /**
     * Sets a custom click handler for the + button.
     */
    public void setOnAddButtonClick(Runnable runnable) {
        addButton.addClickListener(e -> runnable.run());
    }

    /* --------------------------------------------------
     * Internal helpers
     * -------------------------------------------------- */

    private void build() {
        // Main direction
        if (orientation == Orientation.HORIZONTAL) {
            listBoxLayout = new HorizontalLayout();
            wrapper = new HorizontalLayout();
            setScrollDirection(ScrollDirection.HORIZONTAL);
        } else {
            listBoxLayout = new VerticalLayout();
            wrapper = new VerticalLayout();
            setScrollDirection(ScrollDirection.VERTICAL);
        }

        ((ThemableLayout) listBoxLayout).setPadding(false);
        ((ThemableLayout) wrapper).setPadding(false);

        // + button
        addButton = new Button(new Icon(VaadinIcon.PLUS));
        addButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);

        wrapper.add(addButton);
        wrapper.add((Component) listBoxLayout);
        setContent((Component) wrapper);
    }

    /**
     * Applies the current {@link ItemOrientation} theme to the given ListBox.
     */
    private void applyItemOrientation(ListBox<T> listBox) {
        if (itemOrientation == ItemOrientation.HORIZONTAL) {
            listBox.getElement().getClassList().add("horizontal");
        } else {
            listBox.getElement().getClassList().remove("horizontal");
        }
    }

    /**
     * Injects a small "X" delete button at the right side of the ListBox.
     */
    private void addDeleteButton(ListBox<T> lb) {
        Button del = new Button(new Icon(VaadinIcon.CLOSE_SMALL));
        del.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        del.getStyle().set("margin-left", "auto"); // pushes to far right in flex container
        //鼠标放上去是指向
        del.getStyle().set("cursor", "pointer");
        del.getElement().setAttribute("title", "删除");
        del.addClickListener(e -> confirmAndRemove(lb));
        lb.add(del);
    }

    private void confirmAndRemove(ListBox<T> lb) {
        Dialog d = new Dialog();
        d.setHeaderTitle("是否删除该选项?");
        VerticalLayout v = new VerticalLayout();
        v.setPadding(false);
        Button yes = new Button("是", e -> {
            removeListBox(lb);
            d.close();
            Notification.show("已删除", 2000, Notification.Position.BOTTOM_CENTER);
        });
        Button no = new Button("否", e -> d.close());
        no.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        v.add(new HorizontalLayout(yes, no));
        d.add(v);
        d.open();
    }

    /**
     * Ensures the container's cross-axis dimension (height for HORIZONTAL, width for VERTICAL)
     * matches the largest ListBox currently present.
     */
    private void adjustCrossAxisSize() {
        if (listBoxes.isEmpty()) {
            if (orientation == Orientation.HORIZONTAL) {
                wrapper.setHeight(null);
                listBoxLayout.setHeight(null);
            } else {
                wrapper.setWidth(null);
                listBoxLayout.setWidth(null);
            }
            return;
        }

        if (orientation == Orientation.HORIZONTAL) {
            int max = listBoxes.stream()
                    .map(ListBox::getPreferredHeight)
                    .mapToInt(this::pxToInt)
                    .max()
                    .orElse(0);
            String h = max + "px";
            wrapper.setHeight(h);
            listBoxLayout.setHeight(h);
        } else {
            int max = listBoxes.stream()
                    .map(ListBox::getPreferredWidth)
                    .mapToInt(this::pxToInt)
                    .max()
                    .orElse(0);
            String w = max + "px";
            wrapper.setWidth(w);
            listBoxLayout.setWidth(w);
        }
    }

    private int pxToInt(String px) {
        if (px == null) return 0;
        try {
            return Integer.parseInt(px.replace("px", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void setupDragAndDrop(ListBox<T> lb) {
        DragSource<ListBox<T>> drag = DragSource.create(lb);
        drag.addDragStartListener(e -> lb.getStyle().set("opacity", "0.5"));
        drag.addDragEndListener(e -> lb.getStyle().set("opacity", "1"));

        DropTarget<ListBox<T>> drop = DropTarget.create(lb);
        drop.addDropListener(e -> {
            ListBox<T> src = (ListBox<T>) e.getDragSourceComponent().orElse(null);
            if (src != null && src != lb) {
                int from = listBoxes.indexOf(src);
                int to = listBoxes.indexOf(lb);
                listBoxes.remove(from);
                listBoxes.add(to, src);
                listBoxLayout.removeAll();
                listBoxes.forEach(listBoxLayout::add);
                triggerOrderChange();
            }
        });
    }

    private void triggerOrderChange() {
        if (orderChangeListener != null) {
            orderChangeListener.onOrderChange(getData());
        }
    }
}