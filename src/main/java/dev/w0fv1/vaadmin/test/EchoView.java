package dev.w0fv1.vaadmin.test;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import dev.w0fv1.vaadmin.GenericRepository;
import dev.w0fv1.vaadmin.view.table.BaseRepositoryTablePage;

/**
 * Echo (演示) 页面
 */
@Route(value = "/sample", layout = MainView.class)
public class EchoView extends BaseRepositoryTablePage<EchoT, EchoF, Echo, Long> {

    private final GenericRepository genericRepository;

    public EchoView(GenericRepository genericRepository) {
        super(genericRepository, EchoT.class, EchoF.class, Echo.class);
        this.genericRepository = genericRepository;
        super.initialize();
        setDefaultFromModel(new EchoF("默认内容"));

    }

}

