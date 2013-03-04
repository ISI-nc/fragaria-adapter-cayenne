package nc.isi.fragaria_adapter_cayenne;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;
import nc.isi.fragaria_adapter_cayenne.model.Directeur;
import nc.isi.fragaria_adapter_cayenne.model.Etablissement;
import nc.isi.fragaria_adapter_cayenne.model.QaModule;
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
import org.apache.tapestry5.ioc.RegistryBuilder;

import com.google.common.collect.Lists;

public class TestCayenneAdapter extends TestCase {
	private static final Registry registry = RegistryBuilder.buildAndStartupRegistry(QaModule.class);
	private Session session;
	private final String cayenneconf = "cayenne-datamap.xml";
	private final Class<abc> viewToTest = abc.class;
	private final String field = "name";
	private final String nameForTesting = "ToTestPost";
	private final String nameForTestingWhereClause = "ToTestWhere";
	private final String nameForTestingDelete = "ToTestDelete";
	private final String modifPrefix = "Modified";
	private final static int nbObjectsToCreate = 2;

	public void init() {
		SessionManager sessionManager = registry
				.getService(SessionManager.class);
		session = sessionManager.create();
	}

	public void testCreate() {
		init();
		Collection<Etablissement> collInit = session.get(new ByViewQuery<>(
				Etablissement.class, viewToTest)
				.filterBy(field, nameForTesting));
		for (int i = 0; i < nbObjectsToCreate; i++) {
			Etablissement etablissement = session.create(Etablissement.class);
			etablissement.setName(nameForTesting);
			Directeur directeur = session.create(Directeur.class);
			etablissement.setDirecteur(directeur);
		}
		session.post();
		Collection<Etablissement> coll = session.get(new ByViewQuery<>(
				Etablissement.class, viewToTest)
				.filterBy(field, nameForTesting));
		System.out.println(coll.size());
		System.out.println(collInit.size() + nbObjectsToCreate);
		assertTrue(coll.size() == collInit.size() + nbObjectsToCreate);
	}

	public void testUpdate() {
		init();
		for (int i = 0; i < nbObjectsToCreate; i++) {
			Etablissement etablissement = session.create(Etablissement.class);
			etablissement.setName(nameForTesting);
		}
		session.post();
		Collection<Etablissement> collInitForTesting = session
				.get(new ByViewQuery<>(Etablissement.class, viewToTest)
						.filterBy(field, nameForTesting));
		Collection<Etablissement> collInitForTestingModif = session
				.get(new ByViewQuery<>(Etablissement.class, viewToTest)
						.filterBy(field, nameForTesting + modifPrefix));
		for (Etablissement etablissement : session.get(new ByViewQuery<>(
				Etablissement.class, viewToTest)
				.filterBy(field, nameForTesting))) {
			etablissement.setName(nameForTesting + modifPrefix);
			if (etablissement.getDirecteur() != null)
				System.out.println(etablissement.getDirecteur().getId());
		}

		session.post();
		Collection<Etablissement> coll = session.get(new ByViewQuery<>(
				Etablissement.class, viewToTest).filterBy(field, nameForTesting
				+ modifPrefix));
		assertTrue(coll.size() == collInitForTesting.size()
				+ collInitForTestingModif.size());
	}

	public void testDelete() {
		init();
		List<Etablissement> etabDel = Lists.newArrayList();

		for (int i = 0; i < nbObjectsToCreate; i++) {
			Etablissement etablissement = session.create(Etablissement.class);
			etablissement.setName(nameForTestingDelete);
			etabDel.add(etablissement);
		}
		session.post();
		Collection<Etablissement> coll = session
				.get(new ByViewQuery<Etablissement>(Etablissement.class,
						All.class).filterBy(field, nameForTestingDelete));
		assertTrue(coll.size() >= nbObjectsToCreate);
		session.delete(coll);
		session.post();
		Collection<Etablissement> coll2 = 
				session.get(new ByViewQuery<Etablissement>(
						Etablissement.class
						,All.class).filterBy(field, nameForTestingDelete));
		assertTrue(coll2.size() == 0);
	}

	public void testGetById() {
		init();
		Etablissement etab = session.create(Etablissement.class);
		String id = etab.getId();
		etab.setName("TestIdEtab");
		session.post();
		Etablissement etabGet = (Etablissement) session
				.getUnique(new IdQuery<>(Etablissement.class, id));
		assertTrue(etabGet.getId().equals(id));
	}

	public void testGetByView() {
		init();
		for (int i = 0; i < nbObjectsToCreate; i++) {
			Etablissement etablissement = session.create(Etablissement.class);
			etablissement.setName(nameForTesting);
			Directeur directeur = session.create(Directeur.class);
			etablissement.setDirecteur(directeur);
		}
		session.post();
		Collection<Etablissement> coll = session.get(new ByViewQuery<>(
				Etablissement.class, viewToTest));
		ServerRuntime cayenneRuntime = new ServerRuntime(cayenneconf);
		ObjectContext context = cayenneRuntime.getContext();

		SQLTemplate query = new SQLTemplate("Etablissement",
				"SELECT * FROM $view");
		query.setParameters(Collections.singletonMap("view", "etablissement"));
		GenericResponse response = (GenericResponse) context
				.performGenericQuery(query);
		assertTrue(coll.size() == response.firstList().size());
	}

	public void testGetByViewWithWhereClause() {
		init();

		for (int i = 0; i < nbObjectsToCreate; i++) {
			Etablissement etablissement = session.create(Etablissement.class);
			etablissement.setName(nameForTestingWhereClause);
		}

		for (int i = 0; i < nbObjectsToCreate; i++) {
			Etablissement etablissement = session.create(Etablissement.class);
			etablissement.setName(nameForTesting);
		}
		session.post();
		Collection<Etablissement> coll = session
				.get(new ByViewQuery<Etablissement>(Etablissement.class,
						viewToTest).filterBy(field, nameForTestingWhereClause));
		assertTrue(nbObjectsToCreate == coll.size());
		for(Etablissement et : coll){
			assertTrue(et.getName().equals(nameForTestingWhereClause));
		}
	}

	public void testGetByViewAll() {
		init();
		Collection<Etablissement> coll = session.get(new ByViewQuery<>(Etablissement.class,
				All.class));
		ServerRuntime cayenneRuntime = new ServerRuntime(cayenneconf);
		ObjectContext context = cayenneRuntime.getContext();
		System.out.println(coll);
		SelectQuery query = new SelectQuery("Etablissement");
		Collection<CayenneDataObject> response = (Collection<CayenneDataObject>) context
				.performQuery(query);
		assertTrue(coll.size() == response.size());
	}

}
