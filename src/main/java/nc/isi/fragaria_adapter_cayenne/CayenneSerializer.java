package nc.isi.fragaria_adapter_cayenne;

import java.util.Collection;

import nc.isi.fragaria_adapter_rewrite.entities.Entity;
import nc.isi.fragaria_adapter_rewrite.entities.views.View;

import org.apache.cayenne.CayenneDataObject;

public interface CayenneSerializer {

	public Collection<CayenneDataObject> serialize(Collection<Entity> entities);

	public CayenneDataObject serialize(Entity entity);

	public CayenneDataObject fillProperties(CayenneDataObject cayenneDO,Entity entity);
	
	public <E extends Entity> Collection<E> deSerialize(
			Collection<CayenneDataObject> cayenneDOs, Class<E> entityClass);
	
	public <E extends Entity> E deSerialize(CayenneDataObject cayenneDO,
			Class<E> entityClass);
	
	public <E extends Entity> Collection<E> deSerialize(
			Collection<CayenneDataObject> cayenneDOs, Class<E> entityClass, Class<? extends View> view);

	public <E extends Entity> E deSerialize(CayenneDataObject cayenneDO,
			Class<E> entityClass,Class<? extends View> view);

}