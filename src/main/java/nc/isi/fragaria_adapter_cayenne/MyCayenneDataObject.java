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
 * Wrapper permettant de passer les "entities" au context de sorte à pouvoir utiliser
 * les fonctionnalités Cayenne. 
 */
public class MyCayenneDataObject extends CayenneDataObject implements DataObject{
	private Entity entity;
	private Source source;
	
	public MyCayenneDataObject() {
		super();
		source = Source.DB;
	}
	
	public MyCayenneDataObject(Entity entity) {
		super();
		checkNotNull(entity);
		this.entity = entity;
		this.objectId = new ObjectId(entity.getClass().getSimpleName(), "id", entity.getId());
		source = Source.ENTITY;
	}
	
	public void update(Entity modifiedEntity){
		checkNotNull(entity);
		if(entity.getId() == modifiedEntity.getId())
			this.entity = modifiedEntity;
		else
			throw new RuntimeException("Impossible d'updater l'objet (id : "+entity.getId()+" ne correspond pas à l'id : "+modifiedEntity.getId());
	}
	
	@Override
	public Object readProperty(String propName) {
		if(source==Source.ENTITY){
			return readPropertyFromEntity(propName);
		}else{
			return readPropertyFromDb(propName);
		}
	}

	private Object readPropertyFromEntity(String propName) {
		if(isEntity(entity.getMetadata().read(entity, propName))){
			Entity prop = (Entity) entity.getMetadata().read(entity, propName);
			return prop.getId();
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
	
	protected boolean isEntity(Object o) {
		return o != null && isEntity(o.getClass());
	}
	
	

	protected boolean isEntity(Class<?> cl) {
		Boolean isEntity = false;
		Class<?> type = cl;
		while(type!=null){
			if(type.equals(Entity.class)){
				isEntity = true;
				break;
			}else{
				type = type.getSuperclass();
			}
		}
		return isEntity;
	}

	public Source getSource() {
		return source;
	}
}
