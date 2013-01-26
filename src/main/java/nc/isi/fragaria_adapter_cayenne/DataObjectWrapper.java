package nc.isi.fragaria_adapter_cayenne;


import nc.isi.fragaria_adapter_rewrite.entities.AbstractEntity;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectId;
/**
 * 
 * @author bjonathas
 * Wrapper permettant de passer les "entities" au context de sorte à pouvoir utiliser
 * les fonctionnalités Cayenne. 
 */
public class DataObjectWrapper extends CayenneDataObject implements DataObject{
	private AbstractEntity entity;
	
	public DataObjectWrapper(AbstractEntity entity) {
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
		return entity.getMetadata().read(entity, propName);
	}
}
