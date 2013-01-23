package nc.isi.fragaria_adapter_rewrite.services.domain;

import java.io.IOException;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

public class ObjectResolverImpl implements ObjectResolver {
	private final ObjectMapper objectMapper;
	private static final Logger LOGGER = Logger
			.getLogger(ObjectResolverImpl.class);

	public ObjectResolverImpl(ObjectMapperProvider objectMapperProvider) {
		this.objectMapper = objectMapperProvider.provide();
	}

	@Override
	public <T> T resolve(ObjectNode node, Class<T> propertyType,
			String propertyName, Entity entity) {
		T result = null;
		if (node.has(entity.getMetadata().getJsonPropertyName(propertyName))) {
			try {
				if (isEntity(propertyType))
					return objectMapper
							.readerWithView(
									entity.getMetadata().getEmbeded(
											propertyName))
							.readValue(
									node.get(entity.getMetadata()
											.getJsonPropertyName(propertyName)));
				return objectMapper.treeToValue(
						node.get(entity.getMetadata().getJsonPropertyName(
								propertyName)), propertyType);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			if (entity.getCompletion() == Completion.FULL)
				return result;
			complete(node, entity);
			return entity.readProperty(propertyType, propertyName);
		}
	}

	private void complete(ObjectNode node, Entity entity) {
		if (entity.getState() != State.NEW) {
			completeFromDS(node, entity);
		}
		entity.setCompletion(Completion.FULL);
	}

	protected void completeFromDS(ObjectNode node, Entity entity) {
		Class<? extends Entity> entityClass = entity.getClass();
		Entity fromDB = entity.getSession().getUnique(
				new IdQuery<>(entityClass, entity.getId()));
		EntityMetadata entityMetadata = entity.getMetadata();
		for (String propertyName : entityMetadata.propertyNames()) {
			if (node.has(entityMetadata.getJsonPropertyName(propertyName)))
				continue;
			Class<?> propertyType = entityMetadata.propertyType(propertyName);
			Object propertyValue = (Collection.class
					.isAssignableFrom(propertyType)) ? fromDB.readCollection(
					entityMetadata.propertyParameterClasses(propertyName)[0],
					propertyName) : fromDB.readProperty(propertyType,
					propertyName);
			entity.writeProperty(propertyName, propertyValue);
		}
	}

	@Override
	public <T> Collection<T> resolveCollection(ObjectNode node,
			Class<T> propertyType, String propertyName, Entity entity) {
		Collection<T> result = null;
		if (node.has(entity.getMetadata().getJsonPropertyName(propertyName))) {
			result = Lists.newArrayList();
			ArrayNode arrayNode = (ArrayNode) node.get(entity.getMetadata()
					.getJsonPropertyName(propertyName));
			try {
				for (JsonNode jsonNode : arrayNode) {
					result.add(objectMapper.treeToValue(jsonNode, propertyType));
				}
				return result;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			if (entity.getCompletion() == Completion.FULL) {
				if (entity.getMetadata().getEmbeded(propertyName) != null
						|| !isEntity(propertyType))
					return result;
				Class<? extends Entity> propertyEntity = propertyType
						.asSubclass(Entity.class);
				entity.writeProperty(
						propertyName,
						entity.getSession()
								.get(new ByViewQuery<>(propertyEntity, entity
										.getMetadata().getPartial(propertyName))
										.filterBy(
												entity.getMetadata()
														.getBackReference(
																propertyName),
												entity.getId())));
			} else {
				complete(node, entity);
			}
			return entity.readCollection(propertyType, propertyName);
		}
	}

	@Override
	public void write(ObjectNode node, String propertyName, Object value,
			Entity entity) {
		if (value != null) {
			if (isEntity(value)) {
				Class<? extends View> view = entity.getMetadata().getEmbeded(
						propertyName);
				Entity property = Entity.class.cast(value);
				node.put(
						entity.getMetadata().getJsonPropertyName(propertyName),
						getJson(property, view));
				return;
			}
			if (Collection.class.isAssignableFrom(value.getClass())) {
				Class<?> propertyType = entity.getMetadata()
						.propertyParameterClasses(propertyName)[0];
				if (isEntity(propertyType)) {
					Collection<? extends Entity> collection = Collection.class
							.cast(value);
					ArrayNode array = objectMapper.createArrayNode();
					for (Entity temp : collection) {
						array.add(getJson(temp, entity.getMetadata()
								.getEmbeded(propertyName)));
					}
					node.put(
							entity.getMetadata().getJsonPropertyName(
									propertyName), array);
					return;
				}
			}
		}
		node.put(entity.getMetadata().getJsonPropertyName(propertyName),
				objectMapper.valueToTree(value));
	}

	private ObjectNode getJson(Entity entity, Class<? extends View> view) {
		return view == null ? entity.toJSON() : entity.toJSON(view);
	}

	public ObjectNode clone(ObjectNode node, Class<? extends View> view,
			Entity entity) {
		ObjectNode copy = objectMapper.createObjectNode();
		for (String property : entity.getMetadata().propertyNames(view)) {
			JsonNode value = objectMapper.valueToTree(entity.readProperty(
					entity.getMetadata().propertyType(property), property));
			copy.put(entity.getMetadata().getJsonPropertyName(property), value);
		}
		System.out.println("copy: " + copy);
		return copy;
	}

	protected JsonNode getNodeToWrite(Object value, Class<? extends View> view) {
		return objectMapper.valueToTree(value);
	}

	protected boolean isEntity(Object o) {
		return isEntity(o.getClass());
	}

	protected boolean isEntity(Class<?> cl) {
		return Entity.class.isAssignableFrom(cl);
	}

}
