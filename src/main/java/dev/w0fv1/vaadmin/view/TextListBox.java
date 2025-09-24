package dev.w0fv1.vaadmin.view;

import com.vaadin.flow.component.html.Div;
import lombok.Getter;
import lombok.Setter;

/**
 * A ListBox implementation that displays items as horizontal text bars.
 *
 * @param <T> The type of the value associated with each list box item.
 */
public class TextListBox<T> extends ListBox<T> {
    private final T value;
    private String title;

    /**
     * Constructs a TextListBox with the specified value and title.
     *
     * @param value The value associated with this TextListBox.
     * @param title The text to display in the list box.
     */
    public TextListBox(T value, String title) {
        this(value, title, null);
    }

    /**
     * Constructs a TextListBox with the specified value, title, and additional CSS classes.
     *
     * @param value          The value associated with this TextListBox.
     * @param title          The text to display in the list box.
     * @param additionalCss  Additional CSS classes for custom styling (optional).
     */
    public TextListBox(T value, String title, String additionalCss) {
        this.value = value;
        this.title = title;

        // Set styles for the horizontal text bar
        getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "space-between")
                .set("width", "160px")
                .set("border", "1px solid #ccc")
                .set("border-radius", "4px")
                .set("padding", "4px 16px")
                .set("background-color", "#fff")
//                抓握小手
                .set("cursor", "grab")
                .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)")
                .set("transition", "background-color 0.3s, box-shadow 0.3s");

        // Add title
        Div titleDiv = new Div();
        titleDiv.setText(title);
        titleDiv.getStyle()
                .set("font-size", "16px")
                .set("color", "#333")
                .set("flex-grow", "1");
        add(titleDiv);

        // Optionally, add an icon or indicator on the right
        if (additionalCss != null && !additionalCss.isEmpty()) {
            addClassName(additionalCss);
        }
    }


    @Override
    public T getValue() {
        return value;
    }

    @Override
    public String getPreferredHeight() {
        return "50px";
    }

    @Override
    public String getPreferredWidth() {
        return 160+16+16+"px"; // Adjust as needed, e.g., "300px" for fixed width
    }
}
