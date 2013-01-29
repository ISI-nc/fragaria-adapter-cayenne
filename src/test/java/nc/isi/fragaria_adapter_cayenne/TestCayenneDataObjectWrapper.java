package nc.isi.fragaria_adapter_cayenne;

import java.util.ArrayList;
import java.util.Collection;
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
import nc.isi.fragaria_adapter_rewrite.entities.EntityMetadata;
import nc.isi.fragaria_adapter_rewrite.entities.views.ViewConfig;
import nc.isi.fragaria_adapter_rewrite.services.FragariaDomainModule;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;

public class TestCayenneDataObjectWrapper extends TestCase{
	private static final Registry REGISTRY = RegistryBuilder
			.buildAndStartupRegistry(FragariaDomainModule.class);
	final EntityBuilder entityBuilder = REGISTRY
			.getService(EntityBuilder.class);
	
	
	public void testInsertSimplePropIntoDb(){
		Session session = buildSession();
		ServerRuntime cayenneRuntime = new ServerRuntime("cayenne-datamap.xml");
		ObjectContext context = cayenneRuntime.getContext();
		Etablissement etablissement = session.create(Etablissement.class);
		etablissement.setName("ATIRSimple");
		MyCayenneDataObject cayenneEtablissement = new MyCayenneDataObject(etablissement);
		context.registerNewObject(cayenneEtablissement);
		context.commitChanges();
	}
	
	public void testInsertToOneRelIntoDbWhenRelIsFromCayenne(){
		Session session = buildSession();
		ServerRuntime cayenneRuntime = new ServerRuntime("cayenne-datamap.xml");
		ObjectContext context = cayenneRuntime.getContext();
		Etablissement etablissement = session.create(Etablissement.class);
		etablissement.setName("ATIR2");

		MyCayenneDataObject cayenneEtablissement = new MyCayenneDataObject(etablissement);
		MyCayenneDataObject cayenneEtablissement2 = new MyCayenneDataObject(etablissement);
		context.registerNewObject(cayenneEtablissement);
		context.registerNewObject(cayenneEtablissement2);
		Directeur directeur = session.create(Directeur.class);
		directeur.setName("David");
		etablissement.setDirecteur(directeur);
		MyCayenneDataObject cayenneDirecteur = new MyCayenneDataObject(directeur);
		context.registerNewObject(cayenneDirecteur);	
		context.commitChanges();
	}
	
	public void testGetFromCayenne(){
		ServerRuntime cayenneRuntime = new ServerRuntime("cayenne-datamap.xml");
		ObjectContext context = cayenneRuntime.getContext();
		Expression e = ExpressionFactory.likeIgnoreCaseExp("name", "%ATIR%");
		SelectQuery q = new SelectQuery(Etablissement.class.getSimpleName());
		for (MyCayenneDataObject obj :  (Collection<MyCayenneDataObject>)context.performQuery(q)){
			System.out.println(obj);
		}
	}
	
	
	public void testSpecificGetFromCayenne(){
		ServerRuntime cayenneRuntime = new ServerRuntime("cayenne-datamap.xml");
		ObjectContext context = cayenneRuntime.getContext();
		ObjectIdQuery query = new ObjectIdQuery(new ObjectId(Etablissement.class.getSimpleName(),"id","7b08ff41-3413-4d23-9268-0b812bb1f434"));
		MyCayenneDataObject cayenneDO = (MyCayenneDataObject) context.performQuery(query).get(0);
		System.out.println(cayenneDO);
	}
	public Session buildSession() {

		SessionImpl session = new SessionImpl(new AdapterManager() {
		

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

			@Override
			public Boolean exist(ViewConfig viewConfig,
					EntityMetadata entityMetadata) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Boolean exist(ViewConfig viewConfig,
					Class<? extends Entity> entityClass) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void buildView(ViewConfig viewConfig,
					EntityMetadata entityMetadata) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void buildView(ViewConfig viewConfig,
					Class<? extends Entity> entityClass) {
				// TODO Auto-generated method stub
				
			}
		}, entityBuilder);

		return (Session) session;
	}
}
