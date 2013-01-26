package nc.isi.fragaria_adapter_cayenne;

import java.util.List;

import junit.framework.TestCase;
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

public class TestCayenneDataObjectWrapper extends TestCase{
	private ObjectContext context;
	private static final Registry REGISTRY = RegistryBuilder
			.buildAndStartupRegistry(FragariaDomainModule.class);
	final EntityBuilder entityBuilder = REGISTRY
			.getService(EntityBuilder.class);
	
	 protected void setUp() {
		ServerRuntime cayenneRuntime = new ServerRuntime("cayenne-datamap.xml");
		context = cayenneRuntime.getContext();
     }
	
	public void testInsertSimplePropIntoDb(){
		Etablissement etablissement = entityBuilder.build(Etablissement.class);
		etablissement.setName("ATIR");
		DataObjectWrapper cayenneEtablissement = new DataObjectWrapper(etablissement);
		context.registerNewObject(cayenneEtablissement);
		context.commitChanges();
	}
	
	public void testInsertToOneRelIntoDbWhenRelIsFromCayenne(){
		Session session = buildSession();
		Etablissement etablissement = session.create(Etablissement.class);
		etablissement.setName("ATIR");
		DataObjectWrapper cayenneEtablissement = new DataObjectWrapper(etablissement);
		context.registerNewObject(cayenneEtablissement);
		Directeur directeur = session.create(Directeur.class);
		directeur.setName("David");
		DataObjectWrapper cayenneDirecteur = new DataObjectWrapper(directeur);
		context.registerNewObject(cayenneDirecteur);
		
//		etablissement.setDirecteur(directeur);
//		cayenneEtablissement.update(etablissement);
		
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

				return  null;
			}
		}, entityBuilder);

		return (Session) session;
	}
}