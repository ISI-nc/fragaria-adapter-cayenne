package nc.isi.fragaria_adapter_cayenne;

import java.util.Collection;

import nc.isi.fragaria_adapter_rewrite.entities.Entity;
import nc.isi.fragaria_adapter_rewrite.entities.views.View;

public interface CayenneSerializer {

	public Collection<EntityCayenneDataObject> serialize(Collection<Entity> objects);

	public EntityCayenneDataObject serialize(Entity object);

	public <E extends Entity> Collection<E> deSerialize(
			Collection<EntityCayenneDataObject> objects, Class<E> entityClass);

	public <E extends Entity> E deSerialize(EntityCayenneDataObject object,
			Class<E> entityClass);
	
	public <E extends Entity> Collection<E> deSerialize(
			Collection<EntityCayenneDataObject> objects, Class<E> entityClass, Class<? extends View> view);

	public <E extends Entity> E deSerialize(EntityCayenneDataObject object,
			Class<E> entityClass,Class<? extends View> view);

}