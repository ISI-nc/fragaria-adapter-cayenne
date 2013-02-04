package nc.isi.fragaria_adapter_cayenne;

import java.util.Collection;

import nc.isi.fragaria_adapter_rewrite.entities.Entity;
import nc.isi.fragaria_adapter_rewrite.entities.EntityBuilder;
import nc.isi.fragaria_adapter_rewrite.entities.EntityMetadata;
import nc.isi.fragaria_adapter_rewrite.entities.views.View;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

public class CayenneSerializerImpl implements CayenneSerializer{
	private final EntityBuilder entityBuilder;
	private final ObjectMapper mapper;
	
	public CayenneSerializerImpl(
			EntityBuilder entityBuilder) {
		super();
		this.entityBuilder = entityBuilder;
		this.mapper = new ObjectMapper();
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
		ObjectNode node = mapper.createObjectNode();
		EntityMetadata metadata = new EntityMetadata(entityClass);
		for(String propertyName : metadata.propertyNames())
			node.put(propertyName, mapper.valueToTree(object.readProperty(propertyName)));
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
		ObjectNode node = mapper.createObjectNode();
		EntityMetadata metadata = new EntityMetadata(entityClass);
		for(String propertyName : metadata.propertyNames(view))
			node.put(propertyName, mapper.valueToTree(object.readProperty(propertyName)));
		return entityBuilder.build(node, entityClass);
	}



}
