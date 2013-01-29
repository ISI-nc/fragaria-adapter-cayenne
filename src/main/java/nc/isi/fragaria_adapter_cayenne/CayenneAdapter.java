package nc.isi.fragaria_adapter_cayenne;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import nc.isi.fragaria_adapter_rewrite.dao.adapters.AbstractAdapter;
import nc.isi.fragaria_adapter_rewrite.entities.Entity;
import nc.isi.fragaria_adapter_rewrite.entities.EntityMetadata;
import nc.isi.fragaria_adapter_rewrite.enums.State;
import nc.isi.fragaria_adapter_rewrite.resources.DataSourceProvider;
import nc.isi.fragaria_adapter_rewrite.resources.Datasource;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.query.ObjectIdQuery;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

public class CayenneAdapter extends AbstractAdapter{
	private final DataSourceProvider dataSourceProvider;
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
	
	public CayenneAdapter(DataSourceProvider dataSourceProvider) {
		this.dataSourceProvider = dataSourceProvider;
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for (Entity entity : filtered) {
			entity.setState(State.COMMITED);
		}
	}
	
	private void register(ObjectContext context, Entity entity) {
		if(entity.getState()==State.NEW)
			context.registerNewObject(new EntityCayenneDataObject(entity));
		else if (entity.getState()==State.MODIFIED){
			ObjectIdQuery query = new ObjectIdQuery(getObjectId(entity));
			EntityCayenneDataObject cayenneDO = (EntityCayenneDataObject) context.performQuery(query);
			cayenneDO.updateFrom(entity);
		}else if(entity.getState()==State.DELETED){
			ObjectIdQuery query = new ObjectIdQuery(getObjectId(entity));
			EntityCayenneDataObject cayenneDO = (EntityCayenneDataObject) context.performQuery(query);
			context.deleteObjects(cayenneDO);
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
	
}
