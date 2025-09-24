package dev.w0fv1.vaadmin.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;

public abstract class ListBox<T> extends Div {
    /**
     * Returns the value associated with this ListBox.
     */
    public abstract T getValue();

    /**
     * Returns the preferred height of this ListBox.
     */
    public abstract String getPreferredHeight();

    /**
     * Returns the preferred width of this ListBox.
     */
    public abstract String getPreferredWidth();

}
