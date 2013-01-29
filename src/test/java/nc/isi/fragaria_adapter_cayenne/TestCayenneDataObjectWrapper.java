package nc.isi.fragaria_adapter_cayenne;

import java.util.ArrayList;
import java.util.List;

import nc.isi.fragaria_adapter_cayenne.model.Directeur;
import nc.isi.fragaria_adapter_cayenne.model.Etablissement;
import nc.isi.fragaria_adapter_rewrite.dao.CollectionQueryResponse;
import nc.isi.fragaria_adapter_rewrite.dao.Query;
import nc.isi.fragaria_adapter_rewrite.dao.Session;
import nc.isi.fragaria_adapter_rewrite.dao.SessionImpl;
import nc.isi.fragaria_adapter_rewrite.dao.UniqueQueryResponse;
import nc.isi.fragaria_adapter_rewrite.dao.adapters.AdapterManager;
import nc.isi.fragaria_adapter_rewrite.entities.Entity;
import nc.isi.fragaria_adapter_rewrite.entities.EntityBuilder;
import nc.isi.fragaria_adapter_rewrite.services.FragariaDomainModule;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestCayenneDataObjectWrapper {
	private ObjectContext context;
	private static final Registry REGISTRY = RegistryBuilder
			.buildAndStartupRegistry(FragariaDomainModule.class);
	final EntityBuilder entityBuilder = REGISTRY
			.getService(EntityBuilder.class);
	
	
	
	@BeforeClass public void setUp() {
		ServerRuntime cayenneRuntime = new ServerRuntime("cayenne-datamap.xml");
		context = cayenneRuntime.getContext();
     }
	
	@Test public void testInsertSimplePropIntoDb(){
		Etablissement etablissement = entityBuilder.build(Etablissement.class);
		etablissement.setName("ATIR");
		MyCayenneDataObject cayenneEtablissement = new MyCayenneDataObject(etablissement);
		context.registerNewObject(cayenneEtablissement);
		context.commitChanges();
	}
	
	@Test public void testInsertToOneRelIntoDbWhenRelIsFromCayenne(){
		Session session = buildSession();
		Etablissement etablissement = session.create(Etablissement.class);
		etablissement.setName("ATIR2");
		MyCayenneDataObject cayenneEtablissement = new MyCayenneDataObject(etablissement);
		context.registerNewObject(cayenneEtablissement);
		Directeur directeur = session.create(Directeur.class);
		directeur.setName("David");
		MyCayenneDataObject cayenneDirecteur = new MyCayenneDataObject(directeur);
		context.registerNewObject(cayenneDirecteur);

		etablissement.setDirecteur(directeur);
		//cayenneEtablissement.setToOneTarget("directeur", cayenneDirecteur, false);

		cayenneEtablissement.update(etablissement);
		
		context.commitChanges();
	}
	
	@Test public void testGetFromCayenne(){
		Session session = buildSession();
		Etablissement etablissement = session.create(Etablissement.class);
		etablissement.setName("ATIR2");
		MyCayenneDataObject cayenneEtablissement = new MyCayenneDataObject(etablissement);
		context.registerNewObject(cayenneEtablissement);
		Directeur directeur = session.create(Directeur.class);
		directeur.setName("David");
		MyCayenneDataObject cayenneDirecteur = new MyCayenneDataObject(directeur);
		context.registerNewObject(cayenneDirecteur);

		etablissement.setDirecteur(directeur);
		cayenneEtablissement.setToOneTarget("directeur", cayenneDirecteur, false);

		cayenneEtablissement.update(etablissement);
		
		context.commitChanges();
	}
	
	public Session buildSession() {

		SessionImpl session = new SessionImpl(new AdapterManager() {

			@Override
			public void post(List<Entity> entities) {

			}

			@Override
			public void post(Entity... entities) {
				// TODO Auto-generated method stub

			}

			@Override
			public <T extends Entity> UniqueQueryResponse<T> executeUniqueQuery(
					Query<T> query) {
				return null;
			}

			@Override
			public <T extends Entity> CollectionQueryResponse<T> executeQuery(
					Query<T> query) {

				return  new CollectionQueryResponse<>(new  ArrayList<T>());
			}
		}, entityBuilder);

		return (Session) session;
	}
}
