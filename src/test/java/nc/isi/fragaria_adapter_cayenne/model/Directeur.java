package nc.isi.fragaria_adapter_cayenne.model;

import java.util.Collection;

import nc.isi.fragaria_adapter_cayenne.model.CityViews.Name;
import nc.isi.fragaria_adapter_rewrite.annotations.BackReference;
import nc.isi.fragaria_adapter_rewrite.entities.AbstractEntity;
import nc.isi.fragaria_adapter_rewrite.entities.EntityMetadataFactory;
import nc.isi.fragaria_adapter_rewrite.entities.ObjectResolver;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Directeur extends AbstractEntity{
	public static final String NAME = "name";
	public static final String ETABLISSEMENTS = "etablissements";

	public Directeur(ObjectNode objectNode, ObjectResolver objectResolver,
			EntityMetadataFactory entityMetadataFactory) {
		super(objectNode, objectResolver, entityMetadataFactory);
	}

	public Directeur() {
		super();
	}

	@JsonView(Name.class)
	public String getName() {
		return readProperty(String.class, NAME);
	}

	public void setName(String name) {
		writeProperty(NAME, name);
	}
	
	
	
	@BackReference("directeur")
	public Collection<Etablissement> getEtablissements() {
		return readCollection(Etablissement.class, ETABLISSEMENTS);
	}

	public void setEtablissements(Etablissement...etablissement) {
		writeProperty(ETABLISSEMENTS, etablissement);
	}
	
	public void setEtablissements(Collection<Etablissement> etablissements) {
		writeProperty(ETABLISSEMENTS, etablissements);
	}

	@Override
	public String toString() {
		return toJSON().toString();
	}
}
