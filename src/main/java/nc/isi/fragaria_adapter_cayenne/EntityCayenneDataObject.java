package nc.isi.fragaria_adapter_cayenne;



import static com.google.common.base.Preconditions.checkNotNull;
import nc.isi.fragaria_adapter_rewrite.entities.Entity;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.Fault;
import org.apache.cayenne.ObjectId;
/**
 * 
 * @author bjonathas
 * Superclass des ObjEntity crées à partir du CayenneModeler permettant de wrapper une Entity afin de pouvoir utiliser
 * les fonctionnalités Cayenne. 
 */
public class EntityCayenneDataObject extends CayenneDataObject implements DataObject{
	private Entity entity;
	private final Source source;
	
	public EntityCayenneDataObject() {
		super();
		this.source = Source.DB;
	}
	
	public EntityCayenneDataObject(Entity entity) {
		super();
		checkNotNull(entity);
		this.entity = entity;
		this.objectId = new ObjectId(entity.getClass().getSimpleName(), "id", entity.getId());
		this.source = Source.ENTITY;
	}
	
	public void updateFrom(Entity modifiedEntity){
		checkNotNull(entity);
		if(entity.getId() == modifiedEntity.getId()){
			this.entity = modifiedEntity;
			this.setPersistenceState(4);
		}
		else
			throw new RuntimeException("Impossible d'updater l'objet (id : "+entity.getId()+" ne correspond pas à l'id : "+modifiedEntity.getId());
	}
	
	@Override
	public Object readProperty(String propName) {
		switch (source) {
		case ENTITY:
			return readPropertyFromEntity(propName);
		case DB:
			return readPropertyFromDb(propName);
		default:
			throw new RuntimeException("La source doit être déterminée");
		}
	}

	private Object readPropertyFromEntity(String propName) {
		if(Entity.class.isAssignableFrom(entity.getMetadata().propertyType(propName))){
			
			Entity prop = (Entity) entity.getMetadata().read(entity, propName);
			if(prop!=null)
				return prop.getId();
			else
				return null;
		}else
			return entity.getMetadata().read(entity, propName);
	}

	private Object readPropertyFromDb(String propName) {
		if (objectContext != null) {
		        // will resolve faults ourselves below as checking class descriptors for the
		        // "lazyFaulting" flag is inefficient. Passing "false" here to suppress fault
		        // processing
		        objectContext.prepareForAccess(this, propName, false);
		    }

		    Object object = readPropertyDirectly(propName);

		    if (object instanceof Fault) {
		        object = ((Fault) object).resolveFault(this, propName);
		        writePropertyDirectly(propName, object);
		    }

		    return object;
	}
	

	
	

	

	public Source getSource() {
		return source;
	}
}
