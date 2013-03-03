package nc.isi.fragaria_adapter_cayenne;

import java.util.Collection;
import java.util.List;

import nc.isi.fragaria_adapter_rewrite.entities.Entity;
import nc.isi.fragaria_adapter_rewrite.entities.EntityBuilder;
import nc.isi.fragaria_adapter_rewrite.entities.EntityMetadata;
import nc.isi.fragaria_adapter_rewrite.entities.FragariaObjectMapper;
import nc.isi.fragaria_adapter_rewrite.entities.views.View;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.ObjEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

/**
 * 
 * @author bjonathas
 * 
 *         Serializer for Cayenne. It serializes Entity into CayenneDataObject
 *         and deserialize CayenneDataObject into Entity.
 */
public class CayenneSerializerImpl implements CayenneSerializer {
	private final EntityBuilder entityBuilder;
	private final ObjectMapper mapper;

	public CayenneSerializerImpl(EntityBuilder entityBuilder) {
		super();
		this.entityBuilder = entityBuilder;
		this.mapper = FragariaObjectMapper.INSTANCE.get();
	}

	@Override
	public Collection<CayenneDataObject> serialize(
			Collection<Entity> objects
			,ObjectContext context) {
		if (objects == null) {
			return null;
		}
		Collection<CayenneDataObject> collection = Lists.newArrayList();
		for (Entity entity : objects) {
			collection.add(serialize(entity,context));
		}
		return collection;
	}

	@Override
	public CayenneDataObject serialize(
			Entity entity
			,ObjectContext context) {
		CayenneDataObject cayenneDO = new CayenneDataObject();
		return fillProperties(cayenneDO, entity,context);

	}

	@Override
	public <E extends Entity> Collection<E> deSerialize(
			Collection<CayenneDataObject> objects, 
			Class<E> entityClass,
			ObjectContext context) {
		if (objects == null) {
			return null;
		}
		Collection<E> collection = Lists.newArrayList();
		for (CayenneDataObject object : objects) {
			collection.add(deSerialize(object, entityClass,context));
		}
		return collection;
	}

	@Override
	public <E extends Entity> E deSerialize(CayenneDataObject object,
			Class<E> entityClass,ObjectContext context) {
		EntityMetadata metadata = new EntityMetadata(entityClass);
		ObjectNode node = createObjectNode(
				object, 
				metadata.propertyNames(),
				metadata,
				context);
		return entityBuilder.build(node, entityClass);
	}

	@Override
	public <E extends Entity> Collection<E> deSerialize(
			Collection<CayenneDataObject> objects, Class<E> entityClass,
			Class<? extends View> view,ObjectContext context) {
		if (objects == null) {
			return null;
		}
		Collection<E> collection = Lists.newArrayList();
		for (CayenneDataObject object : objects) {
			collection.add(deSerialize(object, entityClass, view,context));
		}
		return collection;
	}

	@Override
	public <E extends Entity> E deSerialize(CayenneDataObject object,
			Class<E> entityClass, Class<? extends View> view,ObjectContext context) {
		if (object == null) {
			return null;
		}
		EntityMetadata metadata = new EntityMetadata(entityClass);
		ObjectNode node = createObjectNode(
				object,
				metadata.propertyNames(view), 
				metadata,
				context);
		return entityBuilder.build(node, entityClass);
	}

	private ObjectNode createObjectNode(
			CayenneDataObject object,
			Collection<String> propertyNames, 
			EntityMetadata metadata,
			ObjectContext context) {
		ObjectNode node = mapper.createObjectNode();
		ObjEntity objEntity = context.getEntityResolver().getObjEntity(
				metadata.getEntityClass().getSimpleName());
		for (String propertyName : propertyNames) {
			Boolean hasToBeWritten = !metadata.isNotEmbededList(propertyName);
			
			if (!hasToBeWritten || objEntity.getAttributeMap().get(propertyName)==null)
				continue;
			String dbAttributeName = objEntity.getAttributeMap()
					.get(propertyName)
					.getDbAttributeName();
	
			if (propertyName.equals(Entity.ID)) {
				String id = object.getObjectId()
									.getIdSnapshot()
									.get(dbAttributeName)
									.toString();
				node.put(metadata.getJsonPropertyName(propertyName),
						mapper.valueToTree(id));
			} else 
			if (Entity.class.isAssignableFrom(metadata
					.propertyType(propertyName))) {
				Object prop = object.readProperty(dbAttributeName);
				ObjectNode propNode = null;
				if (prop!=null)
					propNode = createPropertyNode(metadata,
							(String) object.readProperty(dbAttributeName));
				node.put(metadata.getJsonPropertyName(propertyName),
						mapper.valueToTree(propNode));
			} else{
				node.put(metadata.getJsonPropertyName(propertyName),
						mapper.valueToTree(object.readProperty(dbAttributeName)));
			}
		}
		return node;
	}
	
	private ObjectNode createObjectNode(
			DataRow dataRow,
			Collection<String> propertyNames, 
			EntityMetadata metadata,
			ObjectContext context) {
		
		ObjectNode node = mapper.createObjectNode();
		ObjEntity objEntity = context.getEntityResolver().getObjEntity(
				metadata.getEntityClass().getSimpleName());
		for (String propertyName : propertyNames) {
			Boolean hasToBeWritten = !metadata.isNotEmbededList(propertyName);
			
			if (!hasToBeWritten || objEntity.getAttributeMap().get(propertyName)==null)
				continue;
			String dbAttributeName = objEntity.getAttributeMap()
					.get(propertyName)
					.getDbAttributeName();
	
			node.put(metadata.getJsonPropertyName(propertyName),
						mapper.valueToTree(dataRow.get(dbAttributeName)));
		}
		return node;
	}

	private ObjectNode createPropertyNode(EntityMetadata metadata, String id) {
		ObjectNode node = mapper.createObjectNode();
		node.put(metadata.getJsonPropertyName(Entity.ID), id);
		return node;
	}

	@Override
	public CayenneDataObject fillProperties(
			CayenneDataObject cayenneDO
			,Entity entity
			,ObjectContext context) {
		EntityMetadata metadata = entity.metadata();
		ObjEntity objEntity = context.getEntityResolver().getObjEntity(
				metadata.getEntityClass().getSimpleName());
		for (String propertyName : metadata.propertyNames()) {
			Boolean hasToBeWritten = !metadata.isNotEmbededList(propertyName);
			if (!hasToBeWritten|| objEntity.getAttributeMap().get(propertyName)==null)
				continue;

			String dbAttributeName = objEntity.getAttributeMap()
					.get(propertyName)
					.getDbAttributeName();
			if (propertyName.equals(Entity.ID)) {
				ObjectId id = new ObjectId(entity.getClass().getSimpleName(),
						dbAttributeName, entity.getId());
				cayenneDO.setObjectId(id);
			} else if (Entity.class.isAssignableFrom(metadata
					.propertyType(propertyName))) {
				Entity prop = (Entity) entity.metadata().read(entity,
						propertyName);
				String propId = null;
				if (prop != null)
					propId = prop.getId();
				cayenneDO.writeProperty(dbAttributeName, propId);
			} else
				cayenneDO.writeProperty(dbAttributeName,
						metadata.read(entity, propertyName));

		}
		return cayenneDO;
	}
	

	@Override
	public <E extends Entity> Collection<E> deSerialize(List<DataRow> dataRows,
			Class<E> entityClass, Class<? extends View> view,
			ObjectContext context) {
		Collection<E> collection = Lists.newArrayList();
		for (DataRow dataRow : dataRows) {
			collection.add(deSerialize(dataRow,entityClass, view,context));
		}
		return collection;
	}

	@Override
	public <E extends Entity> E deSerialize(DataRow dataRow,
			Class<E> entityClass, Class<? extends View> view,
			ObjectContext context) {
		EntityMetadata metadata = new EntityMetadata(entityClass);
		ObjectNode node = createObjectNode(
				dataRow, 
				metadata.propertyNames(),
				metadata,
				context);
		return entityBuilder.build(node, entityClass);
	}



}
