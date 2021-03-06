package nc.isi.fragaria_adapter_cayenne.views;

import nc.isi.fragaria_adapter_rewrite.entities.views.ViewConfig;
/**
 * 
 * @author bjonathas
 *
 *View Config for Cayenne defined by :
 *	- a name,
 *	- the sql script to create the view in the database
 */
public class CayenneViewConfig  implements ViewConfig{
	private final String name;
	private String script;
	
	public CayenneViewConfig(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}
	

}
