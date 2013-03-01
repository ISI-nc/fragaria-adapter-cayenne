package nc.isi.fragaria_adapter_cayenne;

import java.util.Collection;
import java.util.List;

import nc.isi.fragaria_adapter_rewrite.entities.Entity;
import nc.isi.fragaria_adapter_rewrite.entities.views.View;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;

public interface CayenneSerializer {

	public Collection<CayenneDataObject> serialize(
			Collection<Entity> entities
			,ObjectContext context);

	public CayenneDataObject serialize(
			Entity entity
			,ObjectContext context);

	public CayenneDataObject fillProperties(
			CayenneDataObject cayenneDO
			,Entity entity
			,ObjectContext context);
	
	public <E extends Entity> Collection<E> deSerialize(
			Collection<CayenneDataObject> cayenneDOs
			,Class<E> entityClass
			,ObjectContext context);
	
	public <E extends Entity> E deSerialize(
			CayenneDataObject cayenneDO
			,Class<E> entityClass
			,ObjectContext context);
	
	public <E extends Entity> Collection<E> deSerialize(
			Collection<CayenneDataObject> cayenneDOs
			,Class<E> entityClass
			,Class<? extends View> view
			,ObjectContext context);

	public <E extends Entity> E deSerialize(
			CayenneDataObject cayenneDO
			,Class<E> entityClass
			,Class<? extends View> view
			,ObjectContext context);
	
	public <E extends Entity> Collection<E> deSerialize(
			List<DataRow> dataRows
			,Class<E> entityClass
			,Class<? extends View> view
			,ObjectContext context);
	
	public <E extends Entity> E deSerialize(
			DataRow dataRow
			,Class<E> entityClass
			,Class<? extends View> view
			,ObjectContext context);

}