package edu.stanford.smi.protege.query;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import edu.stanford.smi.protege.action.ExportToCsvAction;
import edu.stanford.smi.protege.action.ExportToCsvUtil;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.query.api.QueryApi;
import edu.stanford.smi.protege.query.api.QueryConfiguration;
import edu.stanford.smi.protege.query.kb.IndexOntologies;
import edu.stanford.smi.protege.query.kb.InvalidQueryException;
import edu.stanford.smi.protege.query.nci.NCIEditAction;
import edu.stanford.smi.protege.query.nci.NCIViewAction;
import edu.stanford.smi.protege.query.querytypes.AndQuery;
import edu.stanford.smi.protege.query.querytypes.MaxMatchQuery;
import edu.stanford.smi.protege.query.querytypes.OrQuery;
import edu.stanford.smi.protege.query.querytypes.VisitableQuery;
import edu.stanford.smi.protege.query.ui.DefaultInstanceViewAction;
import edu.stanford.smi.protege.query.ui.QueryComponent;
import edu.stanford.smi.protege.query.ui.QueryFrameRenderer;
import edu.stanford.smi.protege.query.ui.QueryRenderer;
import edu.stanford.smi.protege.query.ui.QueryResourceRenderer;
import edu.stanford.smi.protege.query.ui.QueryUtil;
import edu.stanford.smi.protege.query.util.JProgressButton;
import edu.stanford.smi.protege.query.util.ListPanel;
import edu.stanford.smi.protege.query.util.ListPanelListener;
import edu.stanford.smi.protege.query.util.LuceneQueryPluginDefaults;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.server.framestore.RemoteClientFrameStore;
import edu.stanford.smi.protege.server.metaproject.Operation;
import edu.stanford.smi.protege.server.metaproject.impl.UnbackedOperationImpl;
import edu.stanford.smi.protege.ui.ListFinder;
import edu.stanford.smi.protege.util.ComponentFactory;
import edu.stanford.smi.protege.util.ComponentUtilities;
import edu.stanford.smi.protege.util.DoubleClickActionAdapter;
import edu.stanford.smi.protege.util.LabeledComponent;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.SelectableList;
import edu.stanford.smi.protege.util.ViewAction;
import edu.stanford.smi.protege.widget.AbstractTabWidget;
import edu.stanford.smi.protege.widget.TabWidget;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.model.OWLNamedClass;
import edu.stanford.smi.protegex.owl.ui.icons.OWLIcons;
import edu.stanford.smi.protegex.owl.ui.icons.OverlayIcon;

/**
 * {@link TabWidget} for doing advanced queries.
 *
 * @author Timothy Redmond, Chris Callendar
 * @date 15-Aug-06
 */
public class LuceneQueryPlugin extends AbstractTabWidget {

    public static final int DEFAULT_MAX_MATCHES = 1000;

	public static final String SEARCHING_ITEM = "Searching...";
	private static final String SEARCH_RESULTS = "Search Results";
	private static final String SEARCH_IN_PROGRESS = "Search Results (search in progress)";

	private static final long serialVersionUID = -5589620508506925170L;

	public static final Operation INDEX_OPERATION = new UnbackedOperationImpl("Generate Lucene Indices", "");

	private KnowledgeBase kb;
	private Collection<Slot> slots;
	private boolean canIndex;
	private boolean isOWL;
	private Slot defaultSlot = null;
    private int maxMatches = DEFAULT_MAX_MATCHES;

	private ViewAction viewAction;
	private ViewAction editAction;
	private JButton viewButton;
	private JButton editButton;

	private ListPanel queriesListPanel;
	private SelectableList lstResults;
	private JRadioButton btnAndQuery;
	private JRadioButton btnOrQuery;
	private JPanel pnlQueryBottom;

	private JButton btnSearch;
	private LabeledComponent resultsComponent;
	private QueryRenderer queryRenderer;

	public LuceneQueryPlugin() {
		super();
		this.canIndex = false;
		this.isOWL = false;
		this.slots = Collections.emptySet();
	}

	/**
	 * Initializes this {@link TabWidget}.  Installs the {@link NarrowFrameStore} and initializes the UI.
	 */
	@SuppressWarnings("unchecked")
	public void initialize() {
		this.kb = getKnowledgeBase();

		// determine if current project is an OWL project, if so collect OWLProperty values
		this.isOWL = getKnowledgeBase() instanceof OWLModel;

		this.queryRenderer = this.isOWL ? new QueryResourceRenderer() : new QueryFrameRenderer();
		this.queryRenderer.setMatchColor(new Color(24, 72, 124));

		this.slots  = new QueryApi(kb).install(new QueryConfiguration(kb));
		this.canIndex = RemoteClientFrameStore.isOperationAllowed(kb, INDEX_OPERATION);
	
		setDefaultSlot(kb.getSlot(LuceneQueryPluginDefaults.getDefaultSearchSlotName()));

        // add UI components
		createGUI();
		// add the default first query component
		addQueryComponent();
	}

	/**
	 * Creates the GUI, initializing the components and adding them to the tab.
	 */
	private void createGUI() {
		setLabel("Lucene Query Tab");
		setIcon(ComponentUtilities.loadImageIcon(LuceneQueryPlugin.class, "querytab.gif"));	// Icons.getQueryIcon(), Icons.getQueryExportIcon();
        setLayout(new BorderLayout());

        JPanel pnlLeft = new JPanel(new BorderLayout(5, 5));
		LabeledComponent lcLeft = new LabeledComponent("Query", pnlLeft, true);

		// only add if the user has permission to index the ontology
		if (canIndex) {
			addIndexButton(lcLeft);
		}

		JButton btn = lcLeft.addHeaderButton(getAddQueryAction());
		btn.setText("Add Query");
		Dimension dim = new Dimension(100, btn.getPreferredSize().height);
		btn.setPreferredSize(dim);
		btn.setMinimumSize(dim);
		btn.setMaximumSize(dim);
		if (isOWL) {
			btn = lcLeft.addHeaderButton(getAddRestrictionQueryAction());
			dim = new Dimension(124, btn.getPreferredSize().height);
			btn.setText("Add Nested");
			btn.setPreferredSize(dim);
			btn.setMinimumSize(dim);
			btn.setMaximumSize(dim);
		}

		pnlLeft.add(new JScrollPane(getQueryList()), BorderLayout.CENTER);
		pnlLeft.add(getQueryBottomPanel(), BorderLayout.SOUTH);

		SelectableList lst = getResultsList();
		resultsComponent = new LabeledComponent(SEARCH_RESULTS, new JScrollPane(lst), true);
		resultsComponent.setFooterComponent(new ListFinder(lst, "Find Instance"));

		viewButton = resultsComponent.addHeaderButton(getEditAction());	// won't be null
		editButton = resultsComponent.addHeaderButton(getViewAction());	// might be null
		
		if (RemoteClientFrameStore.isOperationAllowed(getKnowledgeBase(), ExportToCsvAction.EXPORT_TO_CSV_OPERATION)) {
			resultsComponent.addHeaderButton(createExportAction());
		}

		JSplitPane splitter = ComponentFactory.createLeftRightSplitPane();
		splitter.setLeftComponent(lcLeft);
		splitter.setRightComponent(resultsComponent);
        add(splitter, BorderLayout.CENTER);
	}

	private Action getEditAction() {
		if (editAction == null) {
			if (NCIEditAction.isValid()) {
				// Add action for showing the selected cls in NCI Edit Tab
				editAction = new NCIEditAction("Edit Cls in the NCI Edit Tab", lstResults, Icons.getViewClsIcon());
			}
			// null otherwise
		}
		return editAction;
	}

	private Action getViewAction() {
		if (viewAction == null) {
			if (NCIViewAction.isValid()) {
				viewAction = new NCIViewAction("View Cls", lstResults, Icons.getViewInstanceIcon());
			} else {
				// add the default view instance action
				viewAction = new DefaultInstanceViewAction("View Instance", lstResults, Icons.getViewClsIcon(), kb);
			}
		}
		return viewAction;
	}
	
	private Action createExportAction() {
		//initialize NCI defaults for export configuration
		ExportToCsvUtil.setSlotsDelimiter(",");
		ExportToCsvUtil.setSlotValuesDelimiter("|");
		ExportToCsvUtil.setExportBrowserText(false);
		
		return new ExportToCsvAction(getKnowledgeBase()) {
			@Override
			public void actionPerformed(ActionEvent event) {
				Collection results = ComponentUtilities.getListValues(lstResults);
				
				// filter out non-cls results - NCI request
				for (Iterator iterator = results.iterator(); iterator.hasNext();) {
					Object object = (Object) iterator.next();
					if (!(object instanceof OWLNamedClass)) {
						iterator.remove();
					}
				}
				
				setInstancesToExport(results);
				setSlotsToExport(getPossibleExportSlots());
				super.actionPerformed(event);
			}
						
			private Collection<Slot> getPossibleExportSlots() {
				ArrayList<Slot> slots = new ArrayList<Slot>();				
				if (isOWL) {
					OWLModel owlModel = (OWLModel) kb;
					slots.add(owlModel.getRDFSLabelProperty());
					slots.add(owlModel.getRDFSCommentProperty());
				}				
				return slots;		
			}
			
			@Override
			protected String getExportValueString(Object value) {
				String delimiter = getSlotsDelimiter();
				String result = super.getExportValueString(value);
				// escape the escape char
				if (result.indexOf(delimiter) > -1) {
					result = "\"" + result.replaceAll("\"", "\"\"") + "\"";
				}
				return result;
			}
		};
	}


	/**
	 * Adds the "Index Ontology" button to the given LabeledComponent.
	 * When clicked it will create an index of the ontology and displays an infinitely looping
	 * {@link JProgressBar} inside the button.  Once the indexing is complete the progress bar disappears.
	 */
	private void addIndexButton(LabeledComponent lc) {
		final JProgressButton btnProgress = new JProgressButton();
		Action action = new AbstractAction("Index Ontology") {
			public void actionPerformed(ActionEvent buttonPushed) {
				int choice = JOptionPane.showConfirmDialog(btnProgress, "Are you sure you want to index this ontology?", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (choice == JOptionPane.YES_OPTION) {
					btnProgress.showProgressBar("Indexing...");
					new Thread(new Runnable() {
						public void run() {
						  try {
							new IndexOntologies(kb).execute();
						  } finally {
							btnProgress.hideProgressBar();
						  }
						}
					}).start();
				}
		    }
		};
		btnProgress.setAction(action);
		// hack - add a fake button to get at the parent
		JButton btnTemp = lc.addHeaderButton(action);
		btnTemp.getParent().add(btnProgress);
		btnTemp.getParent().remove(btnTemp);	// now remove the fake button

		btnProgress.setToolTipText("Create the index for this ontology");
		btnProgress.setPreferredSize(new Dimension(120, 26));
		btnProgress.setFont(btnProgress.getFont().deriveFont(Font.BOLD));
		btnProgress.setForeground(Color.darkGray);
	}

	/**
	 * Initializes and returns the query {@link ListPanel}.  This is the panel that contains
	 * all the queries (there will always be at least one query).
	 */
	private ListPanel getQueryList() {
		if (queriesListPanel == null) {
			queriesListPanel = new ListPanel(200, false);
			// ensure always one query component exists
			queriesListPanel.addListener(new ListPanelListener() {
				public void panelAdded(JPanel panel, ListPanel listPanel) {}
				public void panelRemoved(JPanel comp, ListPanel listPanel) {
					if (listPanel.getPanelCount() == 0) {
						addQueryComponent();
					}
				}
			});
		}
		return queriesListPanel;
	}


	/**
	 * Initializes and returns the bottom query panel which contains
	 * the "Add Query", "Clear" and "Search" buttons, as well as the
	 * "Match All" and "Match Any" checkboxes.
	 */
	private JPanel getQueryBottomPanel() {
		if (pnlQueryBottom == null) {
			pnlQueryBottom = new JPanel();
			pnlQueryBottom.setLayout(new BoxLayout(pnlQueryBottom, BoxLayout.LINE_AXIS));
			pnlQueryBottom.setPreferredSize(new Dimension(500, 28));

			pnlQueryBottom.add(new JButton(getSetMaxMatchesAction()));
			pnlQueryBottom.add(Box.createRigidArea(new Dimension(4, 0)));

			JButton btn = new JButton(new AbstractAction("Clear", Icons.getClearIcon(false, false)) {
				public void actionPerformed(ActionEvent e) {
					clearComponents();
				}
			});
			btn.setToolTipText("Remove all queries and start over");
			pnlQueryBottom.add(btn);
			pnlQueryBottom.add(Box.createRigidArea(new Dimension(8, 0)));

			btnAndQuery = new JRadioButton("Match All  ", true);
			btnOrQuery = new JRadioButton("Match Any  ", false);
			pnlQueryBottom.add(btnAndQuery);
			pnlQueryBottom.add(btnOrQuery);
			ButtonGroup group = new ButtonGroup();
			group.add(btnAndQuery);
			group.add(btnOrQuery);

			pnlQueryBottom.add(Box.createHorizontalGlue());

			btnSearch = new JButton(new AbstractAction("Search", Icons.getFindIcon()) {
				public void actionPerformed(ActionEvent e) {
					doSearch();
				}
			});
			pnlQueryBottom.add(btnSearch);

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (getRootPane() != null) {
						getRootPane().setDefaultButton(btnSearch);
					}
				}
			});
		}
		return pnlQueryBottom;
	}

	private SelectableList getResultsList() {
		if (lstResults == null) {
	        lstResults = ComponentFactory.createSelectableList(null, false);
	        lstResults.setCellRenderer(queryRenderer);
	        lstResults.addMouseListener(new DoubleClickActionAdapter(getEditAction()));
		}
		return lstResults;
	}

// KLO start

    @Override
	@SuppressWarnings("unchecked")
	public Collection getSelection() {
		return lstResults.getSelection();
	}

// KLO end

	private Action getAddQueryAction() {
		return new AbstractAction("Add Query", Icons.getAddQueryLibraryIcon()) {
			public void actionPerformed(ActionEvent e) {
				addQueryComponent();
			}
		};
	}

    private Action getSetMaxMatchesAction() {
        return new AbstractAction("Set Max Matches") {
            public void actionPerformed(ActionEvent e) {
                String userValue = JOptionPane.showInputDialog("Enter max matches count (0 or less for all)",
                                                               new Integer(maxMatches));
                try {
                    maxMatches = Integer.parseInt(userValue);
                }
                catch (NumberFormatException nfe) {
                    Log.getLogger().fine("max matches count not updated.");
                }
            }
        };
    }

	private Action getAddRestrictionQueryAction() {
		Icon icon = new OverlayIcon(OWLIcons.getImageIcon(OWLIcons.PRIMITIVE_OWL_CLASS).getImage(), 5, 5,
									OWLIcons.getImageIcon(OWLIcons.ADD_OVERLAY).getImage(), 15, 13, 15, 16);
		return new AbstractAction("Add a nested query", icon) {
			public void actionPerformed(ActionEvent e) {
				QueryUtil.addRestrictionQueryComponent((OWLModel)kb, slots, defaultSlot, queriesListPanel);
			}
		};
	}

	public void setQueryComponent(Slot slot, String defaultValue ){
		queriesListPanel.removeAllPanels();
		QueryUtil.addQueryComponent(kb, slots, (slot == null ? defaultSlot:slot), queriesListPanel, defaultValue);
		queriesListPanel.repaint();
		queriesListPanel.revalidate();
	}

	/**
	 * Enables or disables the view and edit buttons in the top right corner of the results panel.
	 */
	public void setViewButtonsEnabled(boolean enabled) {
		if (getEditAction() != null) {
			getEditAction().setEnabled(enabled);
		}
		if (getViewAction() != null) {
			getViewAction().setEnabled(enabled);
		}
	}

	/**
	 * Shows or hides the view and edit buttons in the top right corner of the results panel.
	 */
	public void setViewButtonsVisible(boolean visible) {
		if (viewButton != null) {
			viewButton.setVisible(visible);
		}
		if (editButton != null) {
			editButton.setVisible(visible);
		}
	}

	private void addQueryComponent() {
		QueryUtil.addQueryComponent(kb, slots, defaultSlot, queriesListPanel);
	}

	/**
	 * Removes all the query components and then adds one back as the starting query.
	 */
	private void clearComponents() {
		queriesListPanel.removeAllPanels();
		addQueryComponent();
		queriesListPanel.repaint();
	}


	/**
	 * test permissions
	 * Creates the {@link Query} object from all the {@link QueryComponent}s.
	 * If there are multiple queries then either an {@link AndQuery} or an {@link OrQuery} are used.
	 * Passes the {@link Query} on to {@link LuceneQueryPlugin#doQuery(Query)} if the query is valid.
	 */
	public void doSearch() {
		btnSearch.setEnabled(false);
		resultsComponent.setHeaderLabel(SEARCH_IN_PROGRESS);
		final Cursor oldCursor = getCursor();
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		lstResults.setListData(new String[] { SEARCHING_ITEM });

		// start searching in a new thread
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				int hits = 0;
				boolean error = false;
				try {
					VisitableQuery query = QueryUtil.getQueryFromListPanel(queriesListPanel,
															      		   btnAndQuery.isSelected());
					if (maxMatches > 0) {
						query = new MaxMatchQuery(query, maxMatches);
					}
					hits = doQuery(query);
                    indicateSearchDone(hits, false);
					setViewButtonsEnabled((hits > 0));
				} catch (InvalidQueryException e) {
					final String msg = "Invalid query: " + e.getMessage();
					System.err.println(msg);
					error = true;
					lstResults.setListData(new String[] { msg });
				} catch (Exception ex) {
					// IOException happens for "sounds like" queries when the ontology hasn't been indexed
					final String msg = "An exception occurred during the query.\n" +
						"This possibly happened because this ontology hasn't been indexed.\n" + ex.getMessage();
					JOptionPane.showMessageDialog(LuceneQueryPlugin.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
					error = true;
					lstResults.setListData(new String[] { "An exception occurred during the query." });
				} finally {
					setCursor(oldCursor);
					btnSearch.setEnabled(true);
                    if (error) {
                        indicateSearchDone(hits, true);
                    }
				}
			}
		});
	}

    private void indicateSearchDone(int hits, boolean error) {
        String matchString = error ? "" : "  (" + hits + " match" + (hits == 1 ? ")" : "es)");
        resultsComponent.setHeaderLabel(SEARCH_RESULTS + matchString);
    }

	/**
	 * Executes the query using the {@link KnowledgeBase} and puts the results
	 * into the list.
	 * @param q the query to perform
	 * @see KnowledgeBase#executeQuery(Query)
	 * @return int number of hits for the query
	 */
	private int doQuery(Query q) {
		Set<Frame> results = null;
		if (q != null) {
			results = kb.executeQuery(q);
		}
		int hits = results != null ? results.size() : 0;
		if (hits == 0) {
			queryRenderer.setQuery(null);	// don't bold anything
			lstResults.setListData(new String[] { "No results found." });
		} else {
			queryRenderer.setQuery(q);		// bold the matching results
			lstResults.setListData(new Vector<Frame>(results));
			lstResults.setSelectedIndex(0);
		}
		return hits;
	}

	public Slot getDefaultSlot() {
		return defaultSlot;
	}

	public void setDefaultSlot(Slot defaultSlot) {
		if (defaultSlot == null) {
			defaultSlot = kb.getSlot(LuceneQueryPluginDefaults.DEFAULT_SLOT_NAME);
		}

		if (defaultSlot == null) {
			this.defaultSlot = kb.getNameSlot();
		} else {
			this.defaultSlot = defaultSlot;
		}

	}

}
