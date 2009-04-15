package edu.stanford.smi.protege.query;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import edu.stanford.smi.protege.action.ExportToCsvAction;
import edu.stanford.smi.protege.action.ExportToCsvUtil;
import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Frame;
import edu.stanford.smi.protege.model.Instance;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.NarrowFrameStore;
import edu.stanford.smi.protege.model.query.Query;
import edu.stanford.smi.protege.query.api.QueryApi;
import edu.stanford.smi.protege.query.api.QueryConfiguration;
import edu.stanford.smi.protege.query.kb.DoQueryJob;
import edu.stanford.smi.protege.query.kb.InvalidQueryException;
import edu.stanford.smi.protege.query.menu.ConfigureLuceneAction;
import edu.stanford.smi.protege.query.menu.InstallIndiciesAction;
import edu.stanford.smi.protege.query.menu.QueryUIConfiguration;
import edu.stanford.smi.protege.query.nci.NCICreateWorkflowAction;
import edu.stanford.smi.protege.query.nci.NCIEditAction;
import edu.stanford.smi.protege.query.nci.NCIViewAction;
import edu.stanford.smi.protege.query.querytypes.VisitableQuery;
import edu.stanford.smi.protege.query.querytypes.impl.AndQuery;
import edu.stanford.smi.protege.query.querytypes.impl.OrQuery;
import edu.stanford.smi.protege.query.ui.NCIExportToCsvAction;
import edu.stanford.smi.protege.query.ui.QueryComponent;
import edu.stanford.smi.protege.query.ui.QueryFrameRenderer;
import edu.stanford.smi.protege.query.ui.QueryRenderer;
import edu.stanford.smi.protege.query.ui.QueryResourceRenderer;
import edu.stanford.smi.protege.query.ui.QueryUtil;
import edu.stanford.smi.protege.query.util.ListPanel;
import edu.stanford.smi.protege.query.util.ListPanelListener;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.server.framestore.RemoteClientFrameStore;
import edu.stanford.smi.protege.ui.ProjectManager;
import edu.stanford.smi.protege.util.AllowableAction;
import edu.stanford.smi.protege.util.CollectionUtilities;
import edu.stanford.smi.protege.util.ComponentFactory;
import edu.stanford.smi.protege.util.ComponentUtilities;
import edu.stanford.smi.protege.util.DoubleClickActionAdapter;
import edu.stanford.smi.protege.util.FrameWithBrowserText;
import edu.stanford.smi.protege.util.LabeledComponent;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.PopupMenuMouseListener;
import edu.stanford.smi.protege.util.SelectableList;
import edu.stanford.smi.protege.util.ViewAction;
import edu.stanford.smi.protege.widget.AbstractTabWidget;
import edu.stanford.smi.protege.widget.TabWidget;
import edu.stanford.smi.protegex.owl.model.OWLModel;
import edu.stanford.smi.protegex.owl.ui.icons.OWLIcons;
import edu.stanford.smi.protegex.owl.ui.icons.OverlayIcon;
import edu.stanford.smi.protegex.util.PagedFrameList;


/**
 * {@link TabWidget} for doing advanced queries.
 * 
 * @author Timothy Redmond, Chris Callendar, Tania Tudorache
 * @date 15-Aug-06
 */
public class LuceneQueryPlugin extends AbstractTabWidget {

	private static final long serialVersionUID = -5589620508506925170L;

	public static final int DEFAULT_MAX_MATCHES = 1000;

	public static final String SEARCHING_ITEM = "Searching...";
	private static final String SEARCH_RESULTS = "Search Results";
	private static final String SEARCH_IN_PROGRESS = "Search Results (search in progress)";
	private static final String SEARCH_NO_RESULTS_FOUND = "No results found.";
	public static final String LUCENE_MENU_NAME = "Lucene";

	private QueryUIConfiguration configuration;

	private KnowledgeBase kb;
	private boolean isOWL;
	private ViewAction viewAction;
	private ViewAction editAction;
	private AllowableAction createWorkflowItemAction;
	
	private JButton viewButton;
	private JButton editButton;
	private ListPanel queriesListPanel;
	private SelectableList lstResults;

	// boolean to disable double click on results when using search in modal dialogs
	private boolean enableClickLstResults = true;

	private JRadioButton btnAndQuery;
	private JRadioButton btnOrQuery;
	private JPanel pnlQueryBottom;
	private JButton btnSearch;
	private PagedFrameList resultsComponent;
	private QueryRenderer queryRenderer;
	private JMenu menu;

	public LuceneQueryPlugin() {
		super();
		this.isOWL = false;
	}

	public LuceneQueryPlugin(boolean b) {
		this();
		enableClickLstResults = b;
	}

	/**
	 * Initializes this {@link TabWidget}. Installs the
	 * {@link NarrowFrameStore} and initializes the UI.
	 */
	@SuppressWarnings("unchecked")
	public void initialize() {
		this.kb = getKnowledgeBase();

		// determine if current project is an OWL project, if so collect
		// OWLProperty values
		this.isOWL = getKnowledgeBase() instanceof OWLModel;

		this.queryRenderer = this.isOWL ? new QueryResourceRenderer() : new QueryFrameRenderer();
		this.queryRenderer.setMatchColor(new Color(24, 72, 124));

		configuration = new QueryUIConfiguration(kb);
		QueryConfiguration qc = new QueryApi(kb).install();
		if (qc != null) {
			configuration.setLuceneSlots(qc.getSearchableSlots());
			configuration.setIndexers(qc.getIndexers());
		}
		// add lucene query menu
		addMenu();
		// add UI components
		createGUI();
		// add the default first query component
		addQueryComponent();
	}

	private void addMenu() {
		JMenuBar menuBar = ProjectManager.getProjectManager()
				.getCurrentProjectMenuBar();
		menu = new JMenu(LUCENE_MENU_NAME);
		if (getUIConfiguration().canIndex()) {
			menu.add(new InstallIndiciesAction(this, kb));
		}
		menu.add(new JMenuItem(new ConfigureLuceneAction(this)));
		menuBar.add(menu);
	}

	public void setLuceneConfiguration(QueryUIConfiguration configuration) {
		this.configuration = configuration;
	}

	public QueryUIConfiguration getUIConfiguration() {
		return configuration;
	}

	/**
	 * Creates the GUI, initializing the components and adding them to the tab.
	 */
	private void createGUI() {
		setLabel("Lucene Query Tab");
		setIcon(ComponentUtilities.loadImageIcon(LuceneQueryPlugin.class,
				"querytab.gif")); // Icons.getQueryIcon(),
									// Icons.getQueryExportIcon();
		setLayout(new BorderLayout());

		JPanel pnlLeft = new JPanel(new BorderLayout(5, 5));
		LabeledComponent lcLeft = new LabeledComponent("Query", pnlLeft, true);

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

		resultsComponent = new PagedFrameList(SEARCH_RESULTS);
		lstResults = resultsComponent.getSelectableList();
		lstResults.setCellRenderer(queryRenderer);
		if (enableClickLstResults) {
			lstResults.addMouseListener(new DoubleClickActionAdapter(
							getEditAction() != null ? getEditAction() : getViewAction()));
		}
		lstResults.addMouseListener(new PopupMenuMouseListener(lstResults) {
			@Override
			protected JPopupMenu getPopupMenu() {
				return createPopupMenu();
			}

			@Override
			protected void setSelection(JComponent c, int x, int y) {
			}
		});

		editButton = resultsComponent.addHeaderButton(getEditAction()); // might be null
		viewButton = resultsComponent.addHeaderButton(getViewAction()); // won't be null
		//init de create workflow action
		createWorkflowItemAction = getCreateWorkflowItemAction();

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
				viewAction = new ViewAction("View Instance", lstResults, Icons.getViewClsIcon()) {

					public void onView(Object o) {
						if (o instanceof FrameWithBrowserText) {
							kb.getProject().show((Instance) ((FrameWithBrowserText) o).getFrame());
						}
					}

					@Override
					public void onSelectionChange() {
						if (!getSelection().isEmpty()) {
							setAllowed(getSelection().iterator().next() instanceof FrameWithBrowserText);
						}
					}
				};
			}
		}
		return viewAction;
	}

	private AllowableAction getCreateWorkflowItemAction() {
		if (createWorkflowItemAction == null) {
			createWorkflowItemAction = new NCICreateWorkflowAction(
					"Create workflow item", lstResults); 
		}
		return createWorkflowItemAction;
	}

	protected JPopupMenu createPopupMenu() {
		JPopupMenu menu = null;
		if (!getSelection().isEmpty()) {
			menu = new JPopupMenu();
			if (getSelection().size() == 1) {
				menu.add(getViewAction());
				menu.add(getEditAction());
				menu.addSeparator();
			}
			menu.add(getCreateWorkflowItemAction());
		}
		return menu;
	}

	private Action createExportAction() {
		// initialize NCI defaults for export configuration
		ExportToCsvUtil.setExportBrowserText(false);
		ExportToCsvUtil.setExportMetadata(true);
		ExportToCsvUtil.setExportSuperclass(true);
		
		return new NCIExportToCsvAction(getKnowledgeBase(), LuceneQueryPlugin.this, false) {			
			@Override
			protected String getStringToExport() {
				VisitableQuery query = getQuery();
				String exportString = query == null ? "Invalid query": "Query:\n\n" + query.toString(0);
				return exportString;
			}
			
			@Override
			protected Collection getClsesToExport() {
				Collection<?> listToExport = ComponentUtilities.getListValues(lstResults);
				if (listToExport.size() == 0 || 
						(listToExport.size() == 1 && 
						CollectionUtilities.getSoleItem(listToExport).equals(SEARCH_NO_RESULTS_FOUND))) {
					listToExport.clear();
				}
				return FrameWithBrowserText.getFrames((Collection<FrameWithBrowserText>) listToExport);				
			}		
		};
	}

	/**
	 * Initializes and returns the query {@link ListPanel}. This is the panel
	 * that contains all the queries (there will always be at least one query).
	 */
	private ListPanel getQueryList() {
		if (queriesListPanel == null) {
			queriesListPanel = new ListPanel(200, false);
			// ensure always one query component exists
			queriesListPanel.addListener(new ListPanelListener() {
				public void panelAdded(JPanel panel, ListPanel listPanel) {
				}

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
	 * Initializes and returns the bottom query panel which contains the "Add
	 * Query", "Clear" and "Search" buttons, as well as the "Match All" and
	 * "Match Any" checkboxes.
	 */
	private JPanel getQueryBottomPanel() {
		if (pnlQueryBottom == null) {
			pnlQueryBottom = new JPanel();
			pnlQueryBottom.setLayout(new BoxLayout(pnlQueryBottom, BoxLayout.LINE_AXIS));
			pnlQueryBottom.setPreferredSize(new Dimension(500, 28));

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

	private Action getAddRestrictionQueryAction() {
		Icon icon = new OverlayIcon(OWLIcons.getImageIcon(
				OWLIcons.PRIMITIVE_OWL_CLASS).getImage(), 5, 5, OWLIcons
				.getImageIcon(OWLIcons.ADD_OVERLAY).getImage(), 15, 13, 15, 16);
		return new AbstractAction("Add a nested query", icon) {
			public void actionPerformed(ActionEvent e) {
				QueryUtil.addRestrictionQueryComponent((OWLModel) kb, LuceneQueryPlugin.this, queriesListPanel);
			}
		};
	}

	public void setQueryComponent(Slot slot, String defaultValue) {
		queriesListPanel.removeAllPanels();
		QueryUtil.addQueryComponent(kb, this, queriesListPanel, defaultValue);
		queriesListPanel.repaint();
		queriesListPanel.revalidate();
	}

	/**
	 * Enables or disables the view and edit buttons in the top right corner of
	 * the results panel.
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
	 * Shows or hides the view and edit buttons in the top right corner of the
	 * results panel.
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
		QueryUtil.addQueryComponent(kb, this, queriesListPanel);
	}

	/**
	 * Removes all the query components and then adds one back as the starting
	 * query.
	 */
	private void clearComponents() {
		queriesListPanel.removeAllPanels();
		addQueryComponent();
		queriesListPanel.repaint();
	}

	/**
	 * test permissions Creates the {@link Query} object from all the
	 * {@link QueryComponent}s. If there are multiple queries then either an
	 * {@link AndQuery} or an {@link OrQuery} are used. Passes the {@link Query}
	 * on to {@link LuceneQueryPlugin#doQuery(Query)} if the query is valid.
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
					VisitableQuery query = QueryUtil.getQueryFromListPanel(queriesListPanel, btnAndQuery.isSelected());
					hits = doQuery(query);
					indicateSearchDone(hits, false);
					setViewButtonsEnabled((hits > 0));
				} catch (InvalidQueryException e) {
					final String msg = "Invalid query: " + e.getMessage();
					System.err.println(msg);
					error = true;
					lstResults.setListData(new String[] { msg });
				} catch (Exception ex) {
					// IOException happens for "sounds like" queries when the
					// ontology hasn't been indexed
					final String msg = "An exception occurred during the query.\n"
							+ "This possibly happened because this ontology hasn't been indexed.\n"
							+ ex.getMessage();
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
		String matchString;
		if (error) {
			matchString = "";
		} else {
			matchString = "  (" + hits + " match" + (hits > 1 ? "es" : "");
			if (hits > configuration.getMaxResultsDisplayed()) {
				matchString = matchString + " shown " + configuration.getMaxResultsDisplayed() + " results at a time)";
			} else {
				matchString = matchString + ")";
			}
		}
		resultsComponent.setHeaderLabel(SEARCH_RESULTS + matchString);
	}

	/**
	 * Executes the query using the {@link KnowledgeBase} and puts the results
	 * into the list.
	 * 
	 * @param q
	 *            the query to perform
	 * @see KnowledgeBase#executeQuery(Query)
	 * @return int number of hits for the query
	 */
	private int doQuery(Query q) {
		List<FrameWithBrowserText> results = null;
		if (q != null) {
			results = new DoQueryJob(kb, q).execute();
			Collection<FrameWithBrowserText> toRemove = new HashSet<FrameWithBrowserText>();
			for (FrameWithBrowserText wrappedFrame : results) {
				Frame frame = wrappedFrame.getFrame();
				if (!configuration.isSearchResultsIncludeClasses() && frame instanceof Cls) {
					toRemove.add(wrappedFrame);
				}
				if (!configuration.isSearchResultsIncludeProperties() && frame instanceof Slot) {
					toRemove.add(wrappedFrame);
				}
				if (!frame.isVisible()) {
					toRemove.add(wrappedFrame);
				}
			}
			results.removeAll(toRemove);
		}
		int hits = results != null ? results.size() : 0;
		if (hits == 0) {
			queryRenderer.setQuery(null); // don't bold anything
			resultsComponent.setAllFrames(new ArrayList<FrameWithBrowserText>());
			lstResults.setListData(new String[] { SEARCH_NO_RESULTS_FOUND });
		} else {
			queryRenderer.setQuery(q); // bold the matching results
			resultsComponent.setAllFrames(results);
			resultsComponent.setPageSize(configuration.getMaxResultsDisplayed());
			lstResults.setSelectedIndex(0);
		}
		return hits;
	}

	public VisitableQuery getQuery() {
		VisitableQuery query = null;
		try {
			query = QueryUtil.getQueryFromListPanel(queriesListPanel, btnAndQuery.isSelected());
		} catch (Exception e) {
			if (Log.getLogger().isLoggable(Level.FINE)) {
				Log.getLogger().log(Level.FINE, "Invalid query", e);
			} else {
				Log.getLogger().warning("Invalid query. Message: " + e.getMessage());
			}
		}
		return query;
	}

}
