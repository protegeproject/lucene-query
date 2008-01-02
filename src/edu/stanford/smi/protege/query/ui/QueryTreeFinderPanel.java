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
import java.util.Iterator;
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
import javax.swing.tree.TreePath;

import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.ModelUtilities;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.model.WidgetDescriptor;
import edu.stanford.smi.protege.query.AdvancedQueryPlugin;
import edu.stanford.smi.protege.resource.ResourceKey;
import edu.stanford.smi.protege.util.ComponentFactory;
import edu.stanford.smi.protege.util.ComponentUtilities;
import edu.stanford.smi.protege.util.Disposable;
import edu.stanford.smi.protege.util.LabeledComponent;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.StandardAction;
import edu.stanford.smi.protegex.owl.ui.ProtegeUI;
import edu.stanford.smi.protegex.owl.ui.dialogs.ModalDialogFactory;


/**
 * This class instantiates the Advanced Query Plugin, so that it can be called
 * as a finder component, not as a tab widget.
 * This class is implemented as a singleton.
 * 
 * @author Tania Tudorache
 */
public class QueryTreeFinderPanel extends JPanel implements Disposable {

	private static final String ADVANCED_QUERY_JAVA_CLASS = "edu.stanford.smi.protege.query.AdvancedQueryPlugin";

	private KnowledgeBase kb;

	private final JFrame frame = createJFrame();

	private AdvancedQueryPlugin advanceQueryTabWidget;

	private static List<String> searchedForStrings = new ArrayList<String>();

	private JComboBox _comboBox;

	private Action _findButtonAction;

	private Cls selectedCls;

	private JTree tree;


	private QueryTreeFinderPanel(KnowledgeBase kb, JTree tree) {
		this.kb = kb;
		this.tree = tree;

		initialize();
	}


	public static QueryTreeFinderPanel getQueryTreeFinderPanel(KnowledgeBase kb, JTree tree) {
		return new QueryTreeFinderPanel(kb, tree);
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

	private void showAdvanceQueryDialog(String text) {
		//Initial case - should be called only once. It could also be moved to the constructor or initialize.

		if (advanceQueryTabWidget == null) {
			advanceQueryTabWidget = getAdvanceQueryTabWidget();

			if (advanceQueryTabWidget == null) {
				Log.getLogger().warning("Advanced Query Plugin not found. Please check whether the plugin is installed correctly.");
				return;
			}
		}

		if (advanceQueryTabWidget != null) {
			//120706
			advanceQueryTabWidget.setQueryComponent(null, text);

			if (text != null && text.length() > 0)
				advanceQueryTabWidget.doSearch();

			// 011907 KLO
			advanceQueryTabWidget.setViewButtonsVisible(false);

			LabeledComponent lc = new LabeledComponent("", advanceQueryTabWidget);
			lc.setPreferredSize(new Dimension(750, 350));

			selectedCls = null;
			int r = ProtegeUI.getModalDialogFactory().showDialog(this, lc, "Search for Class", ModalDialogFactory.MODE_OK_CANCEL);
			if (r == ModalDialogFactory.OPTION_OK) {
				// Get user selection
				Collection c = advanceQueryTabWidget.getSelection();
				if (c != null && c.size() > 0)
				{
					Iterator it = c.iterator();
					Object obj = it.next();
					selectedCls = (Cls) obj;
					setExpandedCls(selectedCls, c);
				}
			}
			dispose();
		}
	}




    private Cls getSelection()
    {
		return selectedCls;
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
				if (command.equals("comboBoxChanged")
						&& (modifiers & InputEvent.BUTTON1_MASK) != 0) {
					doFind();
				}

			}
		});
		_comboBox.getEditor().getEditorComponent().addKeyListener(
				new KeyAdapter() {
					public void keyTyped(KeyEvent e) {
						if (e.getKeyChar() == KeyEvent.VK_ENTER) {
							doFind();
						}
					}
				});
		_comboBox.addPopupMenuListener(createPopupMenuListener());
		return _comboBox;
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


	private PopupMenuListener createPopupMenuListener() {
		return new PopupMenuListener() {

			public void popupMenuCanceled(PopupMenuEvent e) {
				// do nothing
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				// do nothing
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				_comboBox.setModel(new DefaultComboBoxModel(searchedForStrings
						.toArray()));
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


	public AdvancedQueryPlugin getAdvanceQueryTabWidget() {
		if (advanceQueryTabWidget == null) {
			Project prj = kb.getProject();

			advanceQueryTabWidget = new AdvancedQueryPlugin();

			WidgetDescriptor wd = prj.createWidgetDescriptor();
			wd.setName("Advanced Query");
			wd.setWidgetClassName(ADVANCED_QUERY_JAVA_CLASS);

			advanceQueryTabWidget.setup(wd, prj);

			advanceQueryTabWidget.initialize();
		}

		return advanceQueryTabWidget;
	}


	public Action getFindAction() {
		if (_findButtonAction != null)
			return _findButtonAction;

		_findButtonAction = new StandardAction(ResourceKey.CLASS_SEARCH_FOR) {
			public void actionPerformed(ActionEvent arg0) {
				doFind();
			}
		};
		return _findButtonAction;
	}


    private void bringFrameToFront() {
        if (frame != null && frame.isVisible()) {

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    frame.toFront();
                }
            });

        }
    }

	public void dispose() {
		//120606
		if (advanceQueryTabWidget != null)
		{
			advanceQueryTabWidget.dispose();
			advanceQueryTabWidget = null;
		}
	}


    private void setExpandedCls(Cls cls, Collection c) {
		Collection objectPath = ModelUtilities.getPathToRoot(cls);

		TreePath path = ComponentUtilities.getTreePath(tree, objectPath);

		if (path != null) {			
			tree.scrollPathToVisible(path);
			tree.setSelectionPath(path);						
		}
	}
        
    
}
