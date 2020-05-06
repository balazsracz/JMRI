package jmri.jmrit.beantable;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SortOrder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableRowSorter;
import jmri.*;
import jmri.swing.RowSorterUtil;
import jmri.util.AlphanumComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class AbstractTableTabAction<E extends NamedBean> extends AbstractTableAction<E> {

    protected JPanel dataPanel;
    protected JTabbedPane dataTabs;

    protected boolean init = false;

    public AbstractTableTabAction(String s) {
        super(s);
    }

    @Override
    protected void createModel() {
        dataPanel = new JPanel();
        dataTabs = new JTabbedPane();
        dataPanel.setLayout(new BorderLayout());
        if (getManager() instanceof jmri.managers.AbstractProxyManager) {
            // build the list, with default at start and internal at end (if present)
            jmri.managers.AbstractProxyManager<E> proxy = (jmri.managers.AbstractProxyManager<E>) getManager();

            tabbedTableArray.add(new TabbedTableItem<>(Bundle.getMessage("All"), true, getManager(), getNewTableAction("All"))); // NOI18N

            List<jmri.Manager<E>> managerList = proxy.getDisplayOrderManagerList();
            for (Manager<E> manager : managerList) {
                String manuName = manager.getMemo().getUserName();
                TabbedTableItem<E> itemModel = new TabbedTableItem<>(manuName, true, manager, getNewTableAction(manuName)); // connection name to display in Tab
                tabbedTableArray.add(itemModel);
            }
            
        } else {
            String manuName = getManager().getMemo().getUserName();
            tabbedTableArray.add(new TabbedTableItem<>(manuName, true, getManager(), getNewTableAction(manuName)));
        }
        for (int x = 0; x < tabbedTableArray.size(); x++) {
            AbstractTableAction<E> table = tabbedTableArray.get(x).getAAClass();
            table.addToPanel(this);
            dataTabs.addTab(tabbedTableArray.get(x).getItemString(), null, tabbedTableArray.get(x).getPanel(), null);
        }
        dataTabs.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent evt) {
                setMenuBar(f);
            }
        });
        dataPanel.add(dataTabs, BorderLayout.CENTER);
        init = true;
    }

    abstract protected Manager<E> getManager();

    abstract protected AbstractTableAction<E> getNewTableAction(String choice);

    @Override
    public JPanel getPanel() {
        if (!init) {
            createModel();
        }
        return dataPanel;
    }

    protected ArrayList<TabbedTableItem<E>> tabbedTableArray = new ArrayList<>();

    @Override
    protected void setTitle() {
        //atf.setTitle("multiple sensors");
    }

    @Override
    abstract protected String helpTarget();

    @Override
    protected void addPressed(ActionEvent e) {
        log.warn("This should not have happened");
    }

    @Override
    public void addToFrame(BeanTableFrame<E> f) {
        try {
            TabbedTableItem<E> table = tabbedTableArray.get(dataTabs.getSelectedIndex());
            if (table != null) {
                table.getAAClass().addToFrame(f);
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            log.error("{} in add to Frame {} {}", ex.toString(), dataTabs.getSelectedIndex(), dataTabs.getSelectedComponent());
        }
    }

    @Override
    public void setMenuBar(BeanTableFrame<E> f) {
        try {
            tabbedTableArray.get(dataTabs.getSelectedIndex()).getAAClass().setMenuBar(f);
        } catch (ArrayIndexOutOfBoundsException ex) {
            log.error("{} in add to Menu {} {}", ex.toString(), dataTabs.getSelectedIndex(), dataTabs.getSelectedComponent());
        }
    }

    public void addToBottomBox(JComponent c, String str) {
        tabbedTableArray.forEach((table) -> {
            String item = table.getItemString();
            if (item != null && item.equals(str)) {
                table.addToBottomBox(c);
            }
        });
    }

    @Override
    public void print(javax.swing.JTable.PrintMode mode, java.text.MessageFormat headerFormat, java.text.MessageFormat footerFormat) {
        try {
            tabbedTableArray.get(dataTabs.getSelectedIndex()).getDataTable().print(mode, headerFormat, footerFormat);
        } catch (java.awt.print.PrinterException e1) {
            log.warn("error printing: {}", e1, e1);
        } catch (NullPointerException ex) {
            log.error("Trying to print returned a NPE error");
        }
    }

    @Override
    public void dispose() {
        for (int x = 0; x < tabbedTableArray.size(); x++) {
            tabbedTableArray.get(x).dispose();
        }
        super.dispose();
    }

    static protected class TabbedTableItem<E extends NamedBean> {  // E comes from the parent

        AbstractTableAction<E> tableAction;
        String itemText;
        BeanTableDataModel<E> dataModel;
        JTable dataTable;
        JScrollPane dataScroll;
        Box bottomBox;
        boolean addToFrameRan = false;
        Manager<E> manager;

        int bottomBoxIndex; // index to insert extra stuff
        static final int bottomStrutWidth = 20;

        boolean standardModel = true;

        final JPanel dataPanel = new JPanel();

        public TabbedTableItem(String choice, boolean stdModel, Manager<E> manager, AbstractTableAction<E> tableAction) {

            this.tableAction = tableAction;
            itemText = choice;
            standardModel = stdModel;
            this.manager = manager;

            //If a panel model is used, it should really add to the bottom box
            //but it can be done this way if required.
            bottomBox = Box.createHorizontalBox();
            bottomBox.add(Box.createHorizontalGlue());
            bottomBoxIndex = 0;
            dataPanel.setLayout(new BorderLayout());
            if (stdModel) {
                createDataModel();
            } else {
                addPanelModel();
            }
        }

        void createDataModel() {
            if (manager != null) {
                tableAction.setManager(manager);
            }
            dataModel = tableAction.getTableDataModel();
            TableRowSorter<BeanTableDataModel> sorter = new TableRowSorter<>(dataModel);
            dataTable = dataModel.makeJTable(dataModel.getMasterClassName() + ":" + getItemString(), dataModel, sorter);
            dataScroll = new JScrollPane(dataTable);

            RowSorterUtil.setSortOrder(sorter, BeanTableDataModel.SYSNAMECOL, SortOrder.ASCENDING);

            sorter.setComparator(BeanTableDataModel.USERNAMECOL, new AlphanumComparator());
            RowSorterUtil.setSortOrder(sorter, BeanTableDataModel.USERNAMECOL, SortOrder.ASCENDING);

            dataModel.configureTable(dataTable);

            java.awt.Dimension dataTableSize = dataTable.getPreferredSize();
            // width is right, but if table is empty, it's not high
            // enough to reserve much space.
            dataTableSize.height = Math.max(dataTableSize.height, 400);
            dataScroll.getViewport().setPreferredSize(dataTableSize);

            // set preferred scrolling options
            dataScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            dataScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

            //dataPanel.setLayout(new BoxLayout(dataPanel, BoxLayout.Y_AXIS));
            dataPanel.add(dataScroll, BorderLayout.CENTER);

            dataPanel.add(bottomBox, BorderLayout.SOUTH);
            if (tableAction.includeAddButton()) {
                JButton addButton = new JButton(Bundle.getMessage("ButtonAdd"));
                addToBottomBox(addButton);
                addButton.addActionListener((ActionEvent e) -> {
                    tableAction.addPressed(e);
                });
            }
            if (dataModel.getPropertyColumnCount() > 0) {
                final JCheckBox propertyVisible = new JCheckBox(Bundle.getMessage
                        ("ShowSystemSpecificProperties"));
                propertyVisible.setToolTipText(Bundle.getMessage
                        ("ShowSystemSpecificPropertiesToolTip"));
                addToBottomBox(propertyVisible);
                propertyVisible.addActionListener((ActionEvent e) -> {
                    dataModel.setPropertyColumnsVisible(dataTable, propertyVisible.isSelected());
                });
                dataModel.setPropertyColumnsVisible(dataTable, false);
            }

        }

        void addPanelModel() {
            dataPanel.add(tableAction.getPanel(), BorderLayout.CENTER);
            dataPanel.add(bottomBox, BorderLayout.SOUTH);
        }

        public boolean getStandardTableModel() {
            return standardModel;
        }

        public String getItemString() {
            return itemText;
        }

        public AbstractTableAction<E> getAAClass() {
            return tableAction;
        }

        public JPanel getPanel() {
            return dataPanel;
        }

        public boolean getAdditionsToFrameDone() {
            return addToFrameRan;
        }

        public void setAddToFrameRan() {
            addToFrameRan = true;
        }

        public JTable getDataTable() {
            return dataTable;
        }

        protected void addToBottomBox(JComponent comp) {
            try {
                bottomBox.add(Box.createHorizontalStrut(bottomStrutWidth), bottomBoxIndex);
                ++bottomBoxIndex;
                bottomBox.add(comp, bottomBoxIndex);
                ++bottomBoxIndex;
            } catch (java.lang.IllegalArgumentException ex) {
                log.error(ex.getLocalizedMessage(), ex);
            }
        }

        protected void dispose() {
            if (dataModel != null) {
                dataModel.stopPersistingTable(dataTable);
                dataModel.dispose();
            }
            dataModel = null;
            dataTable = null;
            dataScroll = null;
        }
    }

    private final static Logger log = LoggerFactory.getLogger(AbstractTableTabAction.class);

}
