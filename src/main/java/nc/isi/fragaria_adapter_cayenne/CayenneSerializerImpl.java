package nc.isi.fragaria_adapter_cayenne;

import java.util.Collection;

import nc.isi.fragaria_adapter_rewrite.entities.Entity;
import nc.isi.fragaria_adapter_rewrite.entities.EntityBuilder;
import nc.isi.fragaria_adapter_rewrite.entities.EntityMetadata;
import nc.isi.fragaria_adapter_rewrite.entities.FragariaObjectMapper;
import nc.isi.fragaria_adapter_rewrite.entities.views.View;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.ObjectId;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

public class CayenneSerializerImpl implements CayenneSerializer{
	private final EntityBuilder entityBuilder;
	private final FragariaObjectMapper mapper;			
	
	public CayenneSerializerImpl(
			EntityBuilder entityBuilder) {
		super();
		this.entityBuilder = entityBuilder;
		this.mapper = FragariaObjectMapper.INSTANCE;
	}
	
	@Override
	public Collection<CayenneDataObject> serialize(
			Collection<Entity> objects) {
		if (objects == null) {
			return null;
		}
		Collection<CayenneDataObject> collection = Lists.newArrayList();
		for (Entity entity : objects) {
			collection.add(serialize(entity));
		}
		return collection;
	}

	@Override
	public CayenneDataObject serialize(Entity entity) {
		CayenneDataObject cayenneDO =  new CayenneDataObject();
		return fillProperties(cayenneDO, entity);
		
	}

	@Override
	public <E extends Entity> Collection<E> deSerialize(
			Collection<CayenneDataObject> objects, Class<E> entityClass) {
		if (objects == null) {
			return null;
		}
		Collection<E> collection = Lists.newArrayList();
		for (CayenneDataObject object : objects) {
			collection.add(deSerialize(object, entityClass));
		}
		return collection;
	}

	@Override
	public <E extends Entity> E deSerialize(CayenneDataObject object,
			Class<E> entityClass) {
		EntityMetadata metadata = new EntityMetadata(entityClass);
		ObjectNode node = createObjectNode(object,metadata.propertyNames(),metadata);
		return entityBuilder.build(node, entityClass);
	}

	@Override
	public <E extends Entity> Collection<E> deSerialize(
			Collection<CayenneDataObject> objects, Class<E> entityClass,
			Class<? extends View> view) {
		if (objects == null) {
			return null;
		}
		Collection<E> collection = Lists.newArrayList();
		for (CayenneDataObject object : objects) {
			collection.add(deSerialize(object, entityClass,view));
		}
		return collection;
	}

	@Override
	public <E extends Entity> E deSerialize(CayenneDataObject object,
			Class<E> entityClass, Class<? extends View> view) {
		EntityMetadata metadata = new EntityMetadata(entityClass);
		ObjectNode node = createObjectNode(object,metadata.propertyNames(view),metadata);
		return entityBuilder.build(node, entityClass);
	}
	
	private ObjectNode createObjectNode(
			CayenneDataObject object,
			Collection<String> propertyNames,
			EntityMetadata metadata){
		ObjectNode node = mapper.get().createObjectNode();
		for(String propertyName :propertyNames){
			Boolean hasNotToBeWritten = metadata.isNotEmbededList(propertyName);
			if(!hasNotToBeWritten){
				if(propertyName.equals(Entity.ID)){
					String id = object.getObjectId().getIdSnapshot().get(Entity.ID).toString();
					node.put(metadata.getJsonPropertyName(propertyName), 
							mapper.get().valueToTree(id));
				}else if (Entity.class.isAssignableFrom(metadata.propertyType(propertyName))){
					node.put(metadata.getJsonPropertyName(propertyName), 
							mapper.get().valueToTree(
									createPropertyNode(
											metadata,
											(String) object.readProperty(propertyName))));
				}else
					node.put(metadata.getJsonPropertyName(propertyName), 
							mapper.get().valueToTree(object.readProperty(propertyName)));
			}
		}
		return node;
	}

	private Object createPropertyNode(EntityMetadata metadata,String id) {
		ObjectNode node = mapper.get().createObjectNode();
		node.put(metadata.getJsonPropertyName(Entity.ID), id);
		return node;
	}

	@Override
	public CayenneDataObject fillProperties(
			CayenneDataObject cayenneDO, Entity entity) {
		EntityMetadata metadata = entity.metadata();
		for(String propertyName : metadata.propertyNames()){
			Boolean hasToBeWritten = !metadata.isNotEmbededList(propertyName);
			if(hasToBeWritten){
				if(propertyName.equals(Entity.ID)){
					ObjectId id = new ObjectId(entity.getClass().getSimpleName(),Entity.ID,entity.getId());
					cayenneDO.setObjectId(id);
				}else if (Entity.class.isAssignableFrom(metadata.propertyType(propertyName))){
					Entity prop = (Entity) entity.metadata().read(entity,propertyName);
					String propId = null;
					if(prop!=null)
						propId = prop.getId();
					cayenneDO.writeProperty(propertyName, propId);
				}else
					cayenneDO.writeProperty(propertyName, metadata.read(entity, propertyName));
			}
		}
		return cayenneDO;
	}



}
