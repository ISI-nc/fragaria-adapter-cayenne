package nc.isi.fragaria_adapter_cayenne;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.mysema.query.alias.Alias.$;
import static com.mysema.query.alias.Alias.alias;
import static com.mysema.query.collections.MiniApi.from;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import nc.isi.fragaria_adapter_cayenne.views.CayenneViewConfig;
import nc.isi.fragaria_adapter_rewrite.dao.ByViewQuery;
import nc.isi.fragaria_adapter_rewrite.dao.CollectionQueryResponse;
import nc.isi.fragaria_adapter_rewrite.dao.IdQuery;
import nc.isi.fragaria_adapter_rewrite.dao.Query;
import nc.isi.fragaria_adapter_rewrite.dao.SearchQuery;
import nc.isi.fragaria_adapter_rewrite.dao.UniqueQueryResponse;
import nc.isi.fragaria_adapter_rewrite.dao.adapters.AbstractAdapter;
import nc.isi.fragaria_adapter_rewrite.dao.adapters.Adapter;
import nc.isi.fragaria_adapter_rewrite.dao.adapters.ElasticSearchAdapter;
import nc.isi.fragaria_adapter_rewrite.entities.Entity;
import nc.isi.fragaria_adapter_rewrite.entities.EntityMetadata;
import nc.isi.fragaria_adapter_rewrite.entities.EntityMetadataFactory;
import nc.isi.fragaria_adapter_rewrite.entities.views.GenericQueryViews.All;
import nc.isi.fragaria_adapter_rewrite.entities.views.ViewConfig;
import nc.isi.fragaria_adapter_rewrite.enums.State;
import nc.isi.fragaria_adapter_rewrite.resources.DataSourceProvider;
import nc.isi.fragaria_adapter_rewrite.resources.Datasource;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

public class CayenneAdapter extends AbstractAdapter implements Adapter{
	private final DataSourceProvider dataSourceProvider;
	private final ElasticSearchAdapter elasticSearchAdapter;
	private final EntityMetadataFactory entityMetadataFactory;
	private final CayenneSerializer serializer;
	private static final long MAX_INSTANCE_TIME = 60L;
	private final LoadingCache<String, ObjectContext> contextCache = CacheBuilder
			.newBuilder()
			.expireAfterAccess(MAX_INSTANCE_TIME, TimeUnit.MINUTES)
			.build(new CacheLoader<String, ObjectContext>() {

				@Override
				public ObjectContext load(String key) {
					ServerRuntime cayenneRuntime = new ServerRuntime(key);
					return cayenneRuntime.getContext();
				}

			});
	
	public CayenneAdapter(DataSourceProvider dataSourceProvider,
			ElasticSearchAdapter elasticSearchAdapter,
			EntityMetadataFactory entityMetadataFactory,
			CayenneSerializer serializer) {
		this.dataSourceProvider = dataSourceProvider;
		this.elasticSearchAdapter = elasticSearchAdapter;
		this.entityMetadataFactory = entityMetadataFactory;
		this.serializer = serializer;
	}
	public <T extends Entity> CollectionQueryResponse<T> executeQuery(
			final Query<T> query) {
		checkNotNull(query);
		if (query instanceof IdQuery) {
			throw new IllegalArgumentException(
					"Impossible de renvoyer une Collection depuis une IdQuery");
		}
		if (query instanceof ByViewQuery) {
			ByViewQuery<T> bVQuery = (ByViewQuery<T>) query;
			Class<T> resultType = (Class<T>)bVQuery.getResultType();
			ObjectContext context = getContext(entityMetadataFactory.create(resultType));
			
			if(bVQuery.getView()!=null){
				String sql = "select * from $view";
				Map<String, Object> filter = bVQuery.getFilter();
				if(filter.size()>0)
					sql+=" where ";
				for(String key : filter.keySet()){
					sql+=key+" #bindEqual($"+key+")";
				}
				SQLTemplate selectQuery = new SQLTemplate(resultType.getSimpleName(),sql);
				selectQuery.setParameters(Collections.singletonMap(
						"view", bVQuery.getView().getSimpleName()));
				for(String key : filter.keySet()){
					selectQuery.setParameters(Collections.singletonMap(
							key, filter.get(key)));
				}
				Collection<EntityCayenneDataObject> result = (Collection<EntityCayenneDataObject>) context.performQuery(selectQuery);	
				CollectionQueryResponse<T> response = new CollectionQueryResponse<>(serializer.deSerialize(result, resultType,bVQuery.getView()));
				if (bVQuery.getPredicate() == null) {
					return response;
				}
				T entity = alias(query.getResultType());
				return buildQueryResponse(from($(entity), response.getResponse())
						.where(bVQuery.getPredicate()).list($(entity)));
			}else if(bVQuery.getView() == null || bVQuery.getView() == All.class){
				Expression e = null;
				Map<String, Object> filter = bVQuery.getFilter();
				for(String key : filter.keySet()){
					if(e==null)
						e = ExpressionFactory.likeIgnoreCaseExp(key,filter.get(key));
					else
						e.andExp(ExpressionFactory.likeIgnoreCaseExp(key,filter.get(key)));
				}
				SelectQuery selectQuery = new SelectQuery(resultType.getSimpleName(),e);
				Collection<EntityCayenneDataObject> result = (Collection<EntityCayenneDataObject>) context.performQuery(selectQuery);	
				CollectionQueryResponse<T> response = new CollectionQueryResponse<>(serializer.deSerialize(result, resultType));
				if (bVQuery.getPredicate() == null) {
					return response;
				}
				T entity = alias(query.getResultType());
				return buildQueryResponse(from($(entity), response.getResponse())
						.where(bVQuery.getPredicate()).list($(entity)));
			}
		}
		if (query instanceof SearchQuery) {
			return elasticSearchAdapter.executeQuery((SearchQuery<T>) query);
		}
		throw new IllegalArgumentException(String.format(
				"Type de query inconnu : %s", query.getClass()));
	}
	
	@Override
	public <T extends Entity> UniqueQueryResponse<T> executeUniqueQuery(
			Query<T> query) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void post(Entity... entities) {
		LinkedList<Entity> list = new LinkedList<>();
		for (Entity entity : entities) {
			list.addLast(checkNotNull(entity));
		}
		post(list);
	}

	public void post(List<Entity> entities) {
		checkNotNull(entities);
		List<Entity> filtered = cleanMultipleEntries(entities);
		for (Entity entity : filtered) {
			ObjectContext context = getContext(entity
					.getMetadata());
			register(context,entity);
		}
		for (String key : contextCache.asMap().keySet()) {
			try {
				contextCache.get(key).commitChanges();
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
		for (Entity entity : filtered) {
			entity.setState(State.COMMITED);
		}
	}
	
	private void register(ObjectContext context, Entity entity) {
		EntityCayenneDataObject object = serializer.serialize(entity);
		context.registerNewObject(object);
		if(entity.getState()==State.NEW)
			object.setPersistenceState(2);
		else if (entity.getState()==State.MODIFIED){
			object.setPersistenceState(4);
		}else if(entity.getState()==State.DELETED){
			object.setPersistenceState(6);
		}
	}

	private ObjectId getObjectId(Entity entity) {
		return new ObjectId(entity.getClass().getSimpleName(), "id", entity.getId());
	}

	protected ObjectContext getContext(EntityMetadata entityMetadata) {
		checkNotNull(entityMetadata);
		Datasource ds = dataSourceProvider.provide(entityMetadata.getDsKey());
		ObjectContext context;
		try {
			context = contextCache.get(((CayenneConnectionData)ds.getDsMetadata().getConnectionData()).getDatamapName());
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		return context;
	}

	private List<Entity> cleanMultipleEntries(List<Entity> entities) {
		List<Entity> filtered = new LinkedList<>();
		Multimap<State, Entity> dispatch = LinkedListMultimap.create();
		for (Entity entity : entities) {
			State state = entity.getState();
			if (!dispatch.containsValue(entity)) {
				dispatch.put(state, entity);
				continue;
			}
			State oldState = lookForEntityState(dispatch, entity);
			manage(dispatch, state, oldState, entity);
		}
		for (State state : dispatch.keySet()) {
			filtered.addAll(dispatch.get(state));
		}
		return filtered;
	}

	private void manage(Multimap<State, Entity> dispatch, State state,
			State oldState, Entity entity) {
		switch (state) {
		case MODIFIED:
			switch (oldState) {
			case NEW:
				dispatch.put(oldState, entity);
				break;
			case MODIFIED:
				dispatch.put(oldState, entity);
				break;
			default:
				commitError(entity, oldState, state);
			}
			break;
		case DELETED:
			switch (oldState) {
			case NEW:
				dispatch.remove(oldState, entity);
				break;
			case MODIFIED:
				dispatch.remove(oldState, entity);
				dispatch.put(state, entity);
				break;
			default:
				commitError(entity, oldState, state);
			}
			break;
		default:
			commitError(entity, oldState, state);
		}

	}

	private State lookForEntityState(Multimap<State, Entity> dispatch,
			Entity entity) {
		for (State state : dispatch.keySet()) {
			if (dispatch.get(state).contains(entity)) {
				return state;
			}
		}
		return null;
	}
	
	protected boolean isEntity(Class<?> cl) {
		Boolean isEntity = false;
		Class<?> type = cl;
		while(type!=null){
			if(type.equals(Entity.class)){
				isEntity = true;
				break;
			}else{
				type = type.getSuperclass();
			}
		}
		return isEntity;
	}

	@Override
	public Boolean exist(ViewConfig viewConfig, EntityMetadata entityMetadata) {
		ObjectContext context = getContext(entityMetadata);
		Boolean exists = true;
		String sql = "select true from $tableName";
		try {
			SQLTemplate checkIfExists = new SQLTemplate(entityMetadata.getEntityClass().getSimpleName(),sql);
			checkIfExists.setParameters(Collections.singletonMap(
					"tableName", entityMetadata.getEntityClass().getSimpleName().toLowerCase()));
			context.performGenericQuery(checkIfExists);
		} catch (Exception e) {
			exists = false;
 		}
			return exists;
	}
	@Override
	public void buildView(ViewConfig viewConfig, EntityMetadata entityMetadata) {
		checkNotNull(entityMetadata);
		checkNotNull(viewConfig);
		if (!(viewConfig instanceof CayenneViewConfig))
			throw new IllegalArgumentException(String.format(
					"Seules les %s sont géré par %s", CayenneViewConfig.class,
					CayenneAdapter.class));
		ObjectContext context = checkNotNull(getContext(entityMetadata));
		CayenneViewConfig cayenneViewConfig = (CayenneViewConfig)viewConfig;
		SQLTemplate createView = new SQLTemplate(entityMetadata.getEntityClass().getSimpleName(),cayenneViewConfig.getScript());	
		context.performGenericQuery(createView);
	}
	
}
