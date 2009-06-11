package edu.stanford.smi.protege.query.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.model.WidgetDescriptor;
import edu.stanford.smi.protege.query.LuceneQueryPlugin;
import edu.stanford.smi.protege.resource.ResourceKey;
import edu.stanford.smi.protege.util.ComponentFactory;
import edu.stanford.smi.protege.util.ComponentUtilities;
import edu.stanford.smi.protege.util.Disposable;
import edu.stanford.smi.protege.util.FrameWithBrowserText;
import edu.stanford.smi.protege.util.LabeledComponent;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.StandardAction;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLOntology;
import edu.stanford.smi.protegex.owl.model.impl.OWLUtil;
import edu.stanford.smi.protegex.owl.ui.ProtegeUI;
import edu.stanford.smi.protegex.owl.ui.dialogs.ModalDialogFactory;

/**
 * This class instantiates the Lucene Query Plugin, so that it can be called as a finder component, not as a tab widget.
 * This class is implemented as a singleton.
 * 
 * @author Tania Tudorache
 */
public class QueryTreeFinderPanel extends JPanel implements Disposable {

    public static final long serialVersionUID = 923455029L;

    private static final String ADVANCED_QUERY_JAVA_CLASS = "edu.stanford.smi.protege.query.LuceneQueryPlugin";

    private KnowledgeBase kb;
    private LuceneQueryPlugin advanceQueryTabWidget;
    private static List<String> searchedForStrings = new ArrayList<String>();
    private JComboBox _comboBox;
    private Action _findButtonAction;
    private Cls selectedCls;
    private JTree tree;

    // frame used when LQT is brought up as a separate window
    private final JFrame frame = createJFrame();

    private boolean viewButtons = true;
    private boolean modal = true;

    public void setViewButtonsVisible(boolean b) {
        viewButtons = b;
    }

    /**
     * 
     * @param kb
     * @param tree
     * @param modal controls whether LQT is brought up as a modal dialog for selecting or as a standalone pane in it's
     *            own frame
     */
    private QueryTreeFinderPanel(KnowledgeBase kb, JTree tree, boolean modal) {
        this.kb = kb;
        this.tree = tree;
        this.modal = modal;
        initialize();
    }

    public static QueryTreeFinderPanel getQueryTreeFinderPanel(KnowledgeBase kb, JTree tree, boolean b) {
        return new QueryTreeFinderPanel(kb, tree, b);
    }

    private void initialize() {
        _findButtonAction = getFindAction();

        setLayout(new BorderLayout());
        add(createTextField(), BorderLayout.CENTER);
        add(createFindButton(), BorderLayout.EAST);
        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        selectedCls = null;

    }

    private void doFind() {
        String text = (String) _comboBox.getSelectedItem();

        if (text != null && text.length() > 0)
            recordItem(text);

        showAdvanceQueryDialog(text);
    }

    private String getOntologyPrettyName() {
        OWLOntology activeOntology = OWLUtil.getActiveOntology((OWLModel) kb);
        String ontnam = activeOntology.getLocalName();
        int ind = ontnam.indexOf(".owl");
        return ontnam.substring(0, ind < 0 ? ontnam.length() : ind);
    }

    private void showAdvanceQueryDialog(String text) {

        if (advanceQueryTabWidget == null) {
            advanceQueryTabWidget = getAdvanceQueryTabWidget(false);
            if (advanceQueryTabWidget == null) {
                Log.getLogger().warning(
                        "Lucene Query Plugin not found. Please check whether the plugin is installed correctly.");
                return;
            }
            if (!modal) {
                frame.setTitle(getOntologyPrettyName() + " Lucene Query");
                frame.getContentPane().add(advanceQueryTabWidget);
                frame.pack();
            }
        }

        if (advanceQueryTabWidget != null) {
            // 120706
            advanceQueryTabWidget.setQueryComponent(null, text);

            if (!modal) {

                // hack to bring frame to front if hidden by other window
                frame.setVisible(false);
                frame.setVisible(true);
            }

            if (text != null && text.length() > 0)
                advanceQueryTabWidget.doSearch();

            if (modal) {
                // 011907 KLO
                advanceQueryTabWidget.setViewButtonsVisible(viewButtons);

                LabeledComponent lc = new LabeledComponent("", advanceQueryTabWidget);
                lc.setPreferredSize(new Dimension(750, 350));

                selectedCls = null;
                int r = ProtegeUI.getModalDialogFactory().showDialog(this, lc,
                        getOntologyPrettyName() + " Lucene Query", ModalDialogFactory.MODE_OK_CANCEL);
                if (r == ModalDialogFactory.OPTION_OK) {
                    // Get user selection
                    Collection selections = advanceQueryTabWidget.getSelection();
                    for (Object selected : selections) {
                        if (selected instanceof FrameWithBrowserText) {
                            FrameWithBrowserText selectedFrame = (FrameWithBrowserText) selected;
                            if (selectedFrame.getFrame() instanceof Cls) {
                                selectedCls = (Cls) selectedFrame.getFrame();
                                break;
                            }
                        }
                    }
                    if (selectedCls != null) {
                        setExpandedCls(selectedCls, selections);
                    }
                }
                // TODO: Tania, this seems odd to do this, as it defeats the purpose of reusing the
                // advancedQueryTabWidget
                // it seems to be set up to reuse it but if I comment out this line the panel doesn't show up
                dispose();
            }

        }
    }

    private JComponent createFindButton() {
        JToolBar toolBar = ComponentFactory.createToolBar();
        ComponentFactory.addToolBarButton(toolBar, _findButtonAction);
        return toolBar;
    }

    private JComponent createTextField() {
        _comboBox = ComponentFactory.createComboBox();
        _comboBox.setEditable(true);
        _comboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String command = event.getActionCommand();
                int modifiers = event.getModifiers();
                if (command.equals("comboBoxChanged") && (modifiers & InputEvent.BUTTON1_MASK) != 0) {
                    doFind();
                }

            }
        });
        _comboBox.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    doFind();
                }
            }
        });
        _comboBox.addPopupMenuListener(createPopupMenuListener());
        return _comboBox;
    }

    private PopupMenuListener createPopupMenuListener() {
        return new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent e) {
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                _comboBox.setModel(new DefaultComboBoxModel(searchedForStrings.toArray()));
            }
        };
    }

    private static void recordItem(String text) {
        searchedForStrings.remove(text);
        searchedForStrings.add(0, text);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        _comboBox.setEnabled(enabled);
        _findButtonAction.setEnabled(enabled);
    }

    public LuceneQueryPlugin getAdvanceQueryTabWidget(boolean enableClicks) {
        if (advanceQueryTabWidget == null) {
            Project prj = kb.getProject();

            // instantiate LQT but disable double clicking of search results and don't add project menu
            advanceQueryTabWidget = new LuceneQueryPlugin(enableClicks);

            WidgetDescriptor wd = prj.createWidgetDescriptor();
            wd.setName(getOntologyPrettyName() + " Lucene Query");
            wd.setWidgetClassName(ADVANCED_QUERY_JAVA_CLASS);

            advanceQueryTabWidget.setup(wd, prj);
            advanceQueryTabWidget.setRunsInWindow(true);
            advanceQueryTabWidget.initialize();

        }

        return advanceQueryTabWidget;
    }

    public Action getFindAction() {
        if (_findButtonAction != null)
            return _findButtonAction;

        _findButtonAction = new StandardAction(ResourceKey.CLASS_SEARCH_FOR) {
            public static final long serialVersionUID = 923456089L;

            public void actionPerformed(ActionEvent arg0) {
                doFind();
            }
        };
        return _findButtonAction;
    }

    public void dispose() {
        // 120606
        if (advanceQueryTabWidget != null) {
            advanceQueryTabWidget.dispose();
            advanceQueryTabWidget = null;
        }
    }

    protected void bringFrameToFront() {
        if (frame != null && frame.isVisible()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    frame.toFront();
                }
            });
        }
    }

    private void setExpandedCls(Cls cls, Collection c) {
        ComponentUtilities.setSelectedNode(cls.getKnowledgeBase(), tree, new FrameWithBrowserText(cls));
    }

    private JFrame createJFrame() {
        final JFrame frame = ComponentFactory.createFrame();
        frame.setTitle("Advanced search");

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                frame.setVisible(false);
            }
        });

        return frame;
    }

}
