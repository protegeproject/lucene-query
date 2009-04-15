package edu.stanford.smi.protege.query.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.model.WidgetDescriptor;
import edu.stanford.smi.protege.query.LuceneQueryPlugin;
import edu.stanford.smi.protege.resource.ResourceKey;
import edu.stanford.smi.protege.util.ComponentFactory;
import edu.stanford.smi.protege.util.ComponentUtilities;
import edu.stanford.smi.protege.util.Disposable;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.StandardAction;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLOntology;
import edu.stanford.smi.protegex.owl.model.impl.OWLUtil;

/**
 * This class instantiates the Lucene Query Plugin, so that it can be called
 * as a finder component, not as a tab widget.
 * This class is implemented as a singleton.
 * @author Tania Tudorache
 *
 */
public class QueryTreeFinderComponent extends JPanel implements Disposable {

	private static final String ADVANCED_QUERY_JAVA_CLASS = "edu.stanford.smi.protege.query.LuceneQueryPlugin";

	private static QueryTreeFinderComponent queryTreeFinderComponent = null;

	private KnowledgeBase kb;

	private final JFrame frame = createJFrame();
	private LuceneQueryPlugin advanceQueryTabWidget;	
	private JComboBox _comboBox;
	private Action _findButtonAction;
	
	private static List<String> searchedForStrings = new ArrayList<String>();


	private QueryTreeFinderComponent(KnowledgeBase kb) {
		this.kb = kb;
		initialize();
	}


	public static QueryTreeFinderComponent getQueryTreeFinderComponent(
			KnowledgeBase kb) {
		if (queryTreeFinderComponent != null)
			return queryTreeFinderComponent;

		return new QueryTreeFinderComponent(kb);
	}


	private void initialize() {
		_findButtonAction = getFindAction();

		setLayout(new BorderLayout());
		add(createTextField(), BorderLayout.CENTER);
		add(createFindButton(), BorderLayout.EAST);
		setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
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
		return ontnam.substring(0, ontnam.indexOf(".owl"));
	}
	
	private void showAdvanceQueryDialog(String text) {
		//Initial case - should be called only once. It could also be moved to the constructor or initialize.
		if (advanceQueryTabWidget == null) {
			advanceQueryTabWidget = getAdvanceQueryTabWidget();
			if (advanceQueryTabWidget == null) {
				Log.getLogger().warning("Advanced Query Plugin not found. Please check whether the plugin is installed correctly.");
				return;
			}			
			frame.getContentPane().add(advanceQueryTabWidget);
			frame.setTitle(getOntologyPrettyName() + " Lucene Query");
			frame.pack();
		}
		
		advanceQueryTabWidget.setQueryComponent(null, text);
		
		if (!frame.isVisible()) {
			ComponentUtilities.center(frame); 
			frame.setVisible(true);
			bringFrameToFront();
		} else {
			frame.setVisible(false);
			frame.setVisible(true);
			frame.toFront();
		}
			
		if (text != null && text.length() > 0) {
			advanceQueryTabWidget.doSearch();
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


	public LuceneQueryPlugin getAdvanceQueryTabWidget() {		
		if (advanceQueryTabWidget == null) {
			Project prj = kb.getProject();
			
			advanceQueryTabWidget = new LuceneQueryPlugin();
			
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


    protected void bringFrameToFront() {
        if (frame != null && frame.isVisible()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    frame.toFront();
                }
            });
        }
    }

	public void dispose() {
        if (frame != null) {
        	ComponentUtilities.deregisterWindow(frame);        	
        }
        if (advanceQueryTabWidget != null) {
            advanceQueryTabWidget.dispose();
        }        
        queryTreeFinderComponent = null;
	}

}
