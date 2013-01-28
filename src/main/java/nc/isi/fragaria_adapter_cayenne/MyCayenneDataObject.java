package nc.isi.fragaria_adapter_cayenne;



import nc.isi.fragaria_adapter_rewrite.entities.AbstractEntity;
import nc.isi.fragaria_adapter_rewrite.entities.Entity;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectId;
/**
 * 
 * @author bjonathas
 * Wrapper permettant de passer les "entities" au context de sorte à pouvoir utiliser
 * les fonctionnalités Cayenne. 
 */
public class MyCayenneDataObject extends CayenneDataObject implements DataObject{
	private AbstractEntity entity;
	
	public MyCayenneDataObject(AbstractEntity entity) {
		super();
		this.entity = entity;
		this.objectId = new ObjectId(entity.getClass().getSimpleName(), "id", entity.getId());
	}
	
	public void update(AbstractEntity modifiedEntity){
		if(entity.getId()==modifiedEntity.getId())
			this.entity = modifiedEntity;
		else
			throw new RuntimeException("Impossible d'updater l'objet (id : "+entity.getId()+" ne correspond pas à l'id : "+modifiedEntity.getId());
	}

	//TODO prendre exemple sur class cayenne etab,medecin patient dans v0 pour regarder quelles methodes sont utilisées et les implémenter
	
	@Override
	public Object readProperty(String propName) {
		if(isEntity(entity.getMetadata().read(entity, propName))){
			Entity prop = (Entity) entity.getMetadata().read(entity, propName);
			this.setToOneTarget(propName, new MyCayenneDataObject((AbstractEntity) prop), false);
			return prop.getId();
		}else
			return entity.getMetadata().read(entity, propName);
	}
	
	protected boolean isEntity(Object o) {
		return o != null && isEntity(o.getClass());
	}
	
	

	protected boolean isEntity(Class<?> cl) {
		Boolean isModel = false;
		Class<?> type = cl;
		while(type!=null){
			if(type.equals(Entity.class)){
				isModel = true;
				break;
			}else{
				type = type.getSuperclass();
			}
		}
		return isModel;
	}
}
