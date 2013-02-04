package nc.isi.fragaria_adapter_cayenne;

import junit.framework.TestCase;
import nc.isi.fragaria_adapter_cayenne.model.CayenneQaRegistry;
import nc.isi.fragaria_adapter_cayenne.model.Directeur;
import nc.isi.fragaria_adapter_cayenne.model.Etablissement;
import nc.isi.fragaria_adapter_rewrite.dao.Session;
import nc.isi.fragaria_adapter_rewrite.dao.SessionManager;

import org.apache.tapestry5.ioc.Registry;

public class TestCayenneAdapter  extends TestCase {
	private static final Registry registry = CayenneQaRegistry.INSTANCE
			.getRegistry();
	
	public void testCreate(){
		Session session;
		SessionManager sessionManager = registry
				.getService(SessionManager.class);
		session = sessionManager.create();
		Etablissement etablissement = session.create(Etablissement.class);
		etablissement.setName("ATIR");
		Directeur directeur = session.create(Directeur.class);
		directeur.setName("Jean-Michel");
		System.out.println(directeur.toJSON());
		etablissement.setDirecteur(directeur);
		session.post();
		
	}
}
