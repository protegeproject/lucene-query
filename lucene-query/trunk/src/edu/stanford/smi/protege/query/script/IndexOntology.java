package edu.stanford.smi.protege.query.script;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.query.api.QueryApi;
import edu.stanford.smi.protege.query.api.QueryConfiguration;
import edu.stanford.smi.protege.server.RemoteProjectManager;
import edu.stanford.smi.protege.ui.ProjectManager;
import edu.stanford.smi.protege.util.Log;

public class IndexOntology {

	private static Logger log = Log.getLogger(IndexOntology.class);

	private static boolean runRemotely = false;

	public static void main(String[] args) {
		if (args.length < 1) {
			log.severe("Missing arg(s). Expected: " + " For local project: (1) ICD pprj file; rest of args ignored "
					+ " For remote project (1) First arg ignored ; (2) Protege Server url; "
					+ "(3) OptionalProtege server user ; (4) Protege server password; (5) Protege server project name.");
			return;
		}

		Project prj = null;

		if (args.length == 1) {
			prj = getLocalProject(args[0]);
		} else if (args.length == 5) {
			prj = connectToRemoteProject(args);
		} else {
			log.warning("Wrong number of arguments. Args = " + args);
			System.exit(1);
		}

		if (prj == null) {
			log.warning("Could not read project. Abort");
			System.exit(1);
		}

		index(prj);
	}

	private static void index(Project prj) {
		KnowledgeBase kb = prj.getKnowledgeBase();
	
		QueryApi api = new QueryApi(kb);
		
		String indexPath = prj.getProjectDirectoryURI().getPath() + File.separator + "lucene" +
				File.separator + prj.getProjectName();
		File indexFile = new File(indexPath);
		
		Log.getLogger().info("Using index configuration from directory: " + indexFile.getAbsolutePath());
		
		QueryConfiguration conf = api.install(indexFile);
		
		log.info("Indexing slots: " + conf.getSearchableSlots());
		log.info("Start indexing .. ");
		
		api.index(conf);
		
		log.info("Ended indexing.");
	}

	private static Project getLocalProject(String path) {
		Collection errors = new ArrayList();
		Project localPrj = Project.loadProjectFromFile(path, errors);

		if (errors != null) {
			ProjectManager.getProjectManager().displayErrors("Errors", errors);
		}
		return localPrj;
	}

	private static Project connectToRemoteProject(String[] args) {
		Project prj = null;
		try {
			prj = RemoteProjectManager.getInstance().getProject(args[1], args[2], args[3], args[4], false);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Cannot connect to remote project: " + args, e.getMessage());
			return null;
		}
		runRemotely = true;
		return prj;
	}

}
