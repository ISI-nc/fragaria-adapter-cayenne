package nc.isi.fragaria_adapter_cayenne;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
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
import nc.isi.fragaria_adapter_rewrite.entities.views.GenericQueryViews.All;
import nc.isi.fragaria_adapter_rewrite.entities.views.ViewConfig;
import nc.isi.fragaria_adapter_rewrite.enums.State;
import nc.isi.fragaria_adapter_rewrite.resources.DataSourceProvider;
import nc.isi.fragaria_adapter_rewrite.resources.Datasource;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * 
 * @author bjonathas
 *
 * This adapter is based on Cayenne interface and allows Session to manipulate 
 * data from relational database. It will be used for all the data from 
 * datasource where datatype = "Cayenne". 
 */

public class CayenneAdapter extends AbstractAdapter implements Adapter{
	private final DataSourceProvider dataSourceProvider;
	private final ElasticSearchAdapter elasticSearchAdapter;
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
			CayenneSerializer serializer) {
		this.dataSourceProvider = dataSourceProvider;
		this.elasticSearchAdapter = elasticSearchAdapter;
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
			checkNotNull(bVQuery.getView());
			Class<T> resultType = (Class<T>)bVQuery.getResultType();
			ObjectContext context = getContext(new EntityMetadata(resultType));
			CollectionQueryResponse<T> response = null;
			if(bVQuery.getView() != All.class){
				response = selectFromView(bVQuery,
						resultType, context);
			}else if(bVQuery.getView() == All.class){
				response = selectFromTable(bVQuery,
						resultType, context);
			}
			if (bVQuery.getPredicate() == null) {
				return response;
			}
			T entity = alias(query.getResultType());
			return buildQueryResponse(from($(entity), response.getResponse())
					.where(bVQuery.getPredicate()).list($(entity)));
		}
		if (query instanceof SearchQuery) {
			return elasticSearchAdapter.executeQuery((SearchQuery<T>) query);
		}
		throw new IllegalArgumentException(String.format(
				"Type de query inconnu : %s", query.getClass()));
	}
	private <T extends Entity> CollectionQueryResponse<T> selectFromTable(
			ByViewQuery<T> bVQuery, Class<T> resultType, ObjectContext context) {
		Expression e = null;
		Map<String, Object> filter = bVQuery.getFilter();
		for(String key : filter.keySet()){
			if(e==null)
				e = ExpressionFactory.likeIgnoreCaseExp(key,filter.get(key));
			else
				e.andExp(ExpressionFactory.likeIgnoreCaseExp(key,filter.get(key)));
		}
		SelectQuery selectQuery = new SelectQuery(resultType.getSimpleName(),e);
		Collection<CayenneDataObject> result = (Collection<CayenneDataObject>) context.performQuery(selectQuery);	
		CollectionQueryResponse<T> response = new CollectionQueryResponse<>(serializer.deSerialize(result, resultType));
		return response;
	}
	
	private <T extends Entity> CollectionQueryResponse<T> selectFromView(
			ByViewQuery<T> bVQuery, Class<T> resultType, ObjectContext context) {
		Map<String, Object> filter = bVQuery.getFilter();
		String sql = "select * from "+bVQuery.getView().getSimpleName().toUpperCase();
		if(filter.size()>0)
			sql+=" where ";
		for(String key : filter.keySet()){
			sql+=key+" #bindEqual($"+key+")";
		}
		SQLTemplate selectQuery = new SQLTemplate(resultType.getSimpleName(),sql);
		for(String key : filter.keySet()){
			selectQuery.setParameters(Collections.singletonMap(
					key, filter.get(key)));
		}
		Collection<CayenneDataObject> result = (Collection<CayenneDataObject>) context.performQuery(selectQuery);	
		CollectionQueryResponse<T> response = new CollectionQueryResponse<>(serializer.deSerialize(result, resultType,bVQuery.getView()));
		return response;
	}
	
	public <T extends Entity> UniqueQueryResponse<T> executeUniqueQuery(
			String id, Class<T> type) {
		checkNotNull(id);
		checkNotNull(type);
		EntityMetadata entityMetadata = new EntityMetadata(type);
		ObjectId objectId = new ObjectId(type.getSimpleName(),Entity.ID,id);
		ObjectIdQuery query = new ObjectIdQuery(objectId,false,ObjectIdQuery.CACHE);
		CayenneDataObject cayenneDO = (CayenneDataObject) Cayenne.objectForQuery(getContext(entityMetadata),query);
		if(cayenneDO==null)
			return buildQueryResponse((T)null);
		T entity = serializer.deSerialize(cayenneDO,type);
		return buildQueryResponse(entity);
	}

	@Override
	public <T extends Entity> UniqueQueryResponse<T> executeUniqueQuery(
			Query<T> query) {
		checkNotNull(query);
		if (query instanceof IdQuery) {
			return executeUniqueQuery(((IdQuery<T>) query).getId(),
					query.getResultType());
		}
		CollectionQueryResponse<T> response = executeQuery(query);
		checkState(response.getResponse().size() <= 1,
				"La requête a renvoyé trop de résultat : %s",
				response.getResponse());
		return buildQueryResponse(response.getResponse().size() == 0 ? null
				: response.getResponse().iterator().next());
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
			ObjectContext context = getContext(entity.metadata());
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
			if (entity.getState()!=State.DELETED)
				entity.setState(State.COMMITED);
		}
	}
	
	private void register(ObjectContext context, Entity entity) {
		switch (entity.getState()) {
		case NEW:
			CayenneDataObject cayenneDOToCreate = serializer.serialize(entity);
			context.registerNewObject(cayenneDOToCreate);
			break;
		case MODIFIED:
			ObjectId idMod = new ObjectId(entity.getClass().getSimpleName(), Entity.ID, entity.getId());
			ObjectIdQuery queryMod = new ObjectIdQuery(idMod, false, ObjectIdQuery.CACHE);
			CayenneDataObject cayenneDOToMod = (CayenneDataObject) Cayenne.objectForQuery(context,queryMod);
			serializer.fillProperties(cayenneDOToMod, entity);
			break;
		case DELETED:
			ObjectId idDel = new ObjectId(entity.getClass().getSimpleName(), Entity.ID, entity.getId());
			ObjectIdQuery queryDel = new ObjectIdQuery(idDel, false, ObjectIdQuery.CACHE);
			CayenneDataObject cayenneDOToDel = (CayenneDataObject) Cayenne.objectForQuery(context,queryDel);
			context.deleteObjects(cayenneDOToDel);
			break;
		default:
			break;
		}	

	}

	protected ObjectContext getContext(EntityMetadata entityMetadata) {
		checkNotNull(entityMetadata);
		Datasource ds = dataSourceProvider.provide(entityMetadata.getDsKey());
		ObjectContext context;
		try {
			context = contextCache.get(((CayenneConnectionData)ds.getDsMetadata().getConnectionData()).getConfFileName());
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
		return context;
	}

	private List<Entity> cleanMultipleEntries(List<Entity> entities) {
		List<Entity> filtered = new LinkedList<>();
		Multimap<State, Entity> dispatch = HashMultimap.create();
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
		case NEW:
			switch (oldState) {
			case NEW:
				dispatch.put(oldState, entity);
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
					"tableName", viewConfig.getName()));
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
		if(entityMetadata.getClass().getSimpleName().toLowerCase()!=cayenneViewConfig.getName()){
			checkNotNull(cayenneViewConfig.getScript());
			SQLTemplate createView = new SQLTemplate(entityMetadata.getEntityClass().getSimpleName(),cayenneViewConfig.getScript());	
			context.performGenericQuery(createView);
		}
	}
	
}
