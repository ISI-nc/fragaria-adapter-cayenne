package nc.isi.fragaria_adapter_cayenne;

import java.util.Collection;

import nc.isi.fragaria_adapter_rewrite.entities.Entity;
import nc.isi.fragaria_adapter_rewrite.entities.EntityBuilder;
import nc.isi.fragaria_adapter_rewrite.entities.EntityMetadata;
import nc.isi.fragaria_adapter_rewrite.entities.FragariaObjectMapper;
import nc.isi.fragaria_adapter_rewrite.entities.views.View;

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
	public Collection<EntityCayenneDataObject> serialize(
			Collection<Entity> objects) {
		if (objects == null) {
			return null;
		}
		Collection<EntityCayenneDataObject> collection = Lists.newArrayList();
		for (Entity entity : objects) {
			collection.add(serialize(entity));
		}
		return collection;
	}

	@Override
	public EntityCayenneDataObject serialize(Entity object) {
		return new EntityCayenneDataObject(object);
	}

	@Override
	public <E extends Entity> Collection<E> deSerialize(
			Collection<EntityCayenneDataObject> objects, Class<E> entityClass) {
		if (objects == null) {
			return null;
		}
		Collection<E> collection = Lists.newArrayList();
		for (EntityCayenneDataObject object : objects) {
			collection.add(deSerialize(object, entityClass));
		}
		return collection;
	}

	@Override
	public <E extends Entity> E deSerialize(EntityCayenneDataObject object,
			Class<E> entityClass) {
		EntityMetadata metadata = new EntityMetadata(entityClass);
		ObjectNode node = createObjectNode(object,metadata.propertyNames(),metadata);
		return entityBuilder.build(node, entityClass);
	}

	@Override
	public <E extends Entity> Collection<E> deSerialize(
			Collection<EntityCayenneDataObject> objects, Class<E> entityClass,
			Class<? extends View> view) {
		if (objects == null) {
			return null;
		}
		Collection<E> collection = Lists.newArrayList();
		for (EntityCayenneDataObject object : objects) {
			collection.add(deSerialize(object, entityClass,view));
		}
		return collection;
	}

	@Override
	public <E extends Entity> E deSerialize(EntityCayenneDataObject object,
			Class<E> entityClass, Class<? extends View> view) {
		EntityMetadata metadata = new EntityMetadata(entityClass);
		ObjectNode node = createObjectNode(object,metadata.propertyNames(view),metadata);
		return entityBuilder.build(node, entityClass);
	}
	
	private ObjectNode createObjectNode(
			EntityCayenneDataObject object,
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
				}else
					node.put(metadata.getJsonPropertyName(propertyName), 
							mapper.get().valueToTree(object.readProperty(propertyName)));
			}
		}
		return node;
	}



}
