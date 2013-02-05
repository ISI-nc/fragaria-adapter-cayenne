package nc.isi.fragaria_adapter_cayenne;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;
import java.util.Collections;

import junit.framework.TestCase;
import nc.isi.fragaria_adapter_cayenne.model.CayenneQaRegistry;
import nc.isi.fragaria_adapter_cayenne.model.Etablissement;
import nc.isi.fragaria_adapter_cayenne.views.abc;
import nc.isi.fragaria_adapter_rewrite.dao.ByViewQuery;
import nc.isi.fragaria_adapter_rewrite.dao.IdQuery;
import nc.isi.fragaria_adapter_rewrite.dao.Session;
import nc.isi.fragaria_adapter_rewrite.dao.SessionManager;
import nc.isi.fragaria_adapter_rewrite.entities.views.GenericQueryViews.All;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.util.GenericResponse;
import org.apache.tapestry5.ioc.Registry;

public class TestCayenneAdapter extends TestCase{
	private static final Registry registry = CayenneQaRegistry.INSTANCE
			.getRegistry();
	private Session session;
	private final static String idToTest = "ac20d40a-6a33-46cc-a143-8425061ee0e2";
	private final Class<abc> viewToTest = abc.class;
	
	public void init(){
		SessionManager sessionManager = registry
				.getService(SessionManager.class);
		session = sessionManager.create();
	}
	
	
	public void testCreate(){
		init();
		Etablissement etablissement = session.create(Etablissement.class);
		etablissement.setName("ATIR");
		session.post();
	}
	
	public void testGetById(){
		init();
		Etablissement etablissement = 
				(Etablissement) session.
				getUnique(new IdQuery<>(Etablissement.class,idToTest));
		System.out.println(etablissement.getId());
		checkArgument(etablissement.getId().equals(idToTest));
	}
	
	public void testGetByView(){
		init();
		Collection<Etablissement> coll =  
				session.get(new ByViewQuery<>(Etablissement.class, viewToTest));
		ServerRuntime cayenneRuntime = new ServerRuntime("cayenne-config.xml");
		ObjectContext context = cayenneRuntime.getContext();
		SQLTemplate query = new SQLTemplate("Etablissement","select * from $view");
		query.setParameters(Collections.singletonMap(
				"view", viewToTest.getSimpleName()));
		GenericResponse response  =  (GenericResponse) context.performGenericQuery(query);
		checkArgument(coll.size()==response.firstList().size());
	}
	
	public void testGetByViewAll(){
		init();
		Collection<Etablissement> coll =  
				session.get(new ByViewQuery<>(Etablissement.class, All.class));
		ServerRuntime cayenneRuntime = new ServerRuntime("cayenne-config.xml");
		ObjectContext context = cayenneRuntime.getContext();
		SelectQuery query = new SelectQuery("Etablissement");
		Collection<EntityCayenneDataObject> response =  (Collection<EntityCayenneDataObject>) context.performQuery(query);
		checkArgument(coll.size()==response.size());
	}
}
