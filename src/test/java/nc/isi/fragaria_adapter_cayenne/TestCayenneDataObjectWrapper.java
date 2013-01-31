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
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.query.SQLTemplate;
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
		EntityCayenneDataObject cayenneEtablissement = new EntityCayenneDataObject(etablissement);
		context.registerNewObject(cayenneEtablissement);
		context.commitChanges();
	}
	
	public void testInsertToOneRelIntoDbWhenRelIsFromCayenne(){
		Session session = buildSession();
		ServerRuntime cayenneRuntime = new ServerRuntime("cayenne-datamap.xml");
		ObjectContext context = cayenneRuntime.getContext();
		Etablissement etablissement = session.create(Etablissement.class);
		etablissement.setName("ATIR2");

		EntityCayenneDataObject cayenneEtablissement = new EntityCayenneDataObject(etablissement);
		EntityCayenneDataObject cayenneEtablissement2 = new EntityCayenneDataObject(etablissement);
		context.registerNewObject(cayenneEtablissement);
		context.registerNewObject(cayenneEtablissement2);
		Directeur directeur = session.create(Directeur.class);
		directeur.setName("David");
		etablissement.setDirecteur(directeur);
		EntityCayenneDataObject cayenneDirecteur = new EntityCayenneDataObject(directeur);
		context.registerNewObject(cayenneDirecteur);	
		context.commitChanges();
	}
	
	public void testGetFromCayenne(){
		ServerRuntime cayenneRuntime = new ServerRuntime("cayenne-datamap.xml");
		ObjectContext context = cayenneRuntime.getContext();
		Expression e = ExpressionFactory.likeIgnoreCaseExp("name", "%ATIR%");
		SelectQuery q = new SelectQuery(Etablissement.class.getSimpleName());
		for (EntityCayenneDataObject obj :  (Collection<EntityCayenneDataObject>)context.performQuery(q)){
			System.out.println(obj);
		}
	}
	
	public void testFromCayenne(){
		ServerRuntime cayenneRuntime = new ServerRuntime("cayenne-datamap.xml");
		ObjectContext context = cayenneRuntime.getContext();
		//Object couille = context.performGenericQuery(new Q)
		DataMap map = context.getEntityResolver().getDataMap("datamap");
		System.out.println(map.getName());
//		DbEntity dbEntity = new DbEntity("cuile");
//		dbEntity.setName("cuile");
//		dbEntity.setSchema("public");
//		ObjEntity objEntity = new ObjEntity("Cuile");
//		objEntity.setDbEntityName("cuile");
//		objEntity.setSuperClassName("nc.isi.fragaria_adapter_cayenne.EntityCayenneDataObject");
//		map.addDbEntity(dbEntity);
//		map.addObjEntity(objEntity);
		SQLTemplate query = new SQLTemplate("Etablissement","select * from nameetab");
		Collection<EntityCayenneDataObject> object = context.performQuery(query);
		System.out.println(context.performQuery(query).get(0));
		
		
		Boolean exists = true;
		try {
			SQLTemplate query2 = new SQLTemplate("Etablissement","select true from test");
			//SQLTemplate query2 = new SQLTemplate("Etablissement","create view test as select * from etablissement");
			context.performGenericQuery(query2);
		} catch (Exception e) {
			exists = false;
 		}
		System.out.println(exists);
		
	}
	
	public void testParseSql(){
		String sqlScript = "create VIEW test as select * from etablissement";
		System.out.println(sqlScript.indexOf("view"));
		System.out.println(sqlScript.substring(sqlScript.indexOf("view")+5, sqlScript.length()).trim());
		String n = sqlScript.substring(sqlScript.indexOf("view")+5, sqlScript.length()).trim();
		System.out.println(n.substring(0, n.indexOf( " ")).trim());
	}
	
	
	
	public void testUpdate(){
		ServerRuntime cayenneRuntime = new ServerRuntime("cayenne-datamap.xml");
		ObjectContext context = cayenneRuntime.getContext();
		Session session = buildSession();
		Etablissement etablissement = session.create(Etablissement.class);
		etablissement.setName("TESTUPDATE");
		context.registerNewObject(new EntityCayenneDataObject(etablissement));
		context.commitChanges();
		etablissement.setName("TESTUPDATEMODIFIED");
		EntityCayenneDataObject cay = new EntityCayenneDataObject(etablissement);
		context.registerNewObject(cay);
		cay.updateFrom(etablissement);
		context.commitChanges();
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
