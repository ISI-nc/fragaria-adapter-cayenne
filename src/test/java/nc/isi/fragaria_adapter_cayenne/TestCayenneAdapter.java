package nc.isi.fragaria_adapter_cayenne;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;
import nc.isi.fragaria_adapter_cayenne.model.CayenneQaRegistry;
import nc.isi.fragaria_adapter_cayenne.model.Directeur;
import nc.isi.fragaria_adapter_cayenne.model.Etablissement;
import nc.isi.fragaria_adapter_cayenne.views.abc;
import nc.isi.fragaria_adapter_rewrite.dao.ByViewQuery;
import nc.isi.fragaria_adapter_rewrite.dao.IdQuery;
import nc.isi.fragaria_adapter_rewrite.dao.Session;
import nc.isi.fragaria_adapter_rewrite.dao.SessionManager;
import nc.isi.fragaria_adapter_rewrite.entities.views.GenericQueryViews.All;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.util.GenericResponse;
import org.apache.tapestry5.ioc.Registry;

import com.beust.jcommander.internal.Lists;

public class TestCayenneAdapter extends TestCase{
	private static final Registry registry = CayenneQaRegistry.INSTANCE
			.getRegistry();
	private Session session;
	private final static String idToTest = "f62cea7c-fdc6-4140-b1a5-997e3f31e9ee";
	private final Class<abc> viewToTest = abc.class;
	private final static String nameForTesting = "ToTestPost";
	private final static String modifPrefix = "Modified";
	private final static int nbObjectsToCreate = 10;
	private List<Etablissement> etablissementsCreated = Lists.newArrayList();
	private List<Directeur> directeursCreated = Lists.newArrayList();
	
	public void init(){
		SessionManager sessionManager = registry
				.getService(SessionManager.class);
		session = sessionManager.create();
	}
	
	
	public void testPostCreate(){
		init();
//		Collection<Etablissement> collInit =  
//				session.get(new ByViewQuery<>(Etablissement.class, viewToTest).where("name","ATIR").
//						or("name", "ToTestPost").
//						or("name", "ATIR3").
//						or("name", "ATIR2").
//						or("name", "ToTestPost").
//						or("name", "ATIRSimple").
//						or("name","TESTUPDATEMODIFIED"));
//		Collection<Etablissement> collInit =  
//				session.get(new ByViewQuery<>(Etablissement.class, viewToTest).where("name",nameForTesting));
		for (int i = 0; i <nbObjectsToCreate;i++){
			Etablissement etablissement = session.create(Etablissement.class);
			etablissement.setName(nameForTesting);
			Directeur directeur = session.create(Directeur.class);
			etablissement.setDirecteur(directeur);
			etablissementsCreated.add(etablissement);
			directeursCreated.add(directeur);
		}
		session.post();
		SessionManager sessionManager = registry
				.getService(SessionManager.class);
		Session session2 = sessionManager.create();
		Collection<Etablissement> coll =  
				session2.get(new ByViewQuery<>(Etablissement.class, viewToTest).where("name",nameForTesting));
		//checkArgument(coll.size()==collInit.size()+nbObjectsToCreate);
	}
	
	public void testPostUpdate(){
		init();
		Collection<Etablissement> collInit =  
				session.get(new ByViewQuery<>(Etablissement.class, viewToTest).where("name",nameForTesting));		
		for(Etablissement etablissement : session.get(new ByViewQuery<>(Etablissement.class, viewToTest).where("name",nameForTesting)))
			etablissement.setName(nameForTesting+modifPrefix);
		session.post();
		SessionManager sessionManager = registry
				.getService(SessionManager.class);
		Session session2 = sessionManager.create();
		Collection<Etablissement> coll =  
				session2.get(new ByViewQuery<>(Etablissement.class, viewToTest).where("name",nameForTesting+modifPrefix));
		checkArgument(coll.size()==collInit.size());
	}
	
	public void testDelete(){
		init();
		for (int i = 0; i <nbObjectsToCreate;i++){
			Etablissement etablissement = session.create(Etablissement.class);
			etablissement.setName(nameForTesting);
			etablissementsCreated.add(etablissement);
		}
		session.post();
		SessionManager sessionManager = registry
				.getService(SessionManager.class);
		Session session2 = sessionManager.create();
		Collection<Etablissement> coll =  
				session2.get(new ByViewQuery<>(Etablissement.class, viewToTest).where("name",nameForTesting));
		checkArgument(coll.size()>=nbObjectsToCreate);
		session.delete(coll);
		session.post();
		Session session3 = sessionManager.create();
		Collection<Etablissement> coll2 =  
				session3.get(new ByViewQuery<>(Etablissement.class, viewToTest).where("name",nameForTesting));
		checkArgument(coll2.size()==0);
	}
	
	public void testGetById(){
		init();
		Etablissement etab = session.create(Etablissement.class);
		String id = etab.getId();
		etablissementsCreated.add(etab);
		session.post();
		SessionManager sessionManager = registry
				.getService(SessionManager.class);
		Session session2 = sessionManager.create();
		Etablissement etabGet = 
				(Etablissement) session2.
				getUnique(new IdQuery<>(Etablissement.class,id));
		System.out.println(etabGet.getId());
		checkArgument(etabGet.getId().equals(id));
//		session.delete(etablissement);
//		session.post();

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
	
	public void testGetByViewWithWhereClause(){
		init();
		Collection<Etablissement> coll =  
				session.get(new ByViewQuery<>(Etablissement.class, viewToTest).where("name",nameForTesting));
		ServerRuntime cayenneRuntime = new ServerRuntime("cayenne-config.xml");
		ObjectContext context = cayenneRuntime.getContext();
		SQLTemplate query = new SQLTemplate("Etablissement","select * from $view where name = 'ATIR'");
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
		Collection<CayenneDataObject> response =  (Collection<CayenneDataObject>) context.performQuery(query);
		checkArgument(coll.size()==response.size());
	}
	
}
