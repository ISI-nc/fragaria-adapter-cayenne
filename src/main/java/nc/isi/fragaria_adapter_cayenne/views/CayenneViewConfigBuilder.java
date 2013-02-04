package nc.isi.fragaria_adapter_cayenne.views;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import nc.isi.fragaria_adapter_rewrite.entities.Entity;
import nc.isi.fragaria_adapter_rewrite.entities.views.QueryView;
import nc.isi.fragaria_adapter_rewrite.entities.views.ViewConfig;
import nc.isi.fragaria_adapter_rewrite.entities.views.ViewConfigBuilder;

import com.google.common.base.Throwables;

public class CayenneViewConfigBuilder implements ViewConfigBuilder{
	public static final String SQL = ".*\\.sql";

	@Override
	public ViewConfig build(String name, File file) {
		CayenneViewConfig config = new CayenneViewConfig(name);
		if (file.getName().matches(SQL)) {
			try {
				config.setScript(getScript(file));
			} catch (IOException e) {
				throw Throwables.propagate(e);
			}
		} else {
			throw new IllegalArgumentException(String.format(
					"Unknown file type : %s", file.getName()));
		}
		return config;
	}

	private String getScript(File file) throws IOException {
		InputStream input = null;
		String script ="";
		try {
			input = new FileInputStream(file);
			BufferedReader br
        	= new BufferedReader(
        		new InputStreamReader(input));
	    	String line;
			while ((line = br.readLine()) != null) {
				script+=line;
	    	} 
			return script;
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} finally {
			if (input != null) {
				input.close();
			}
		}
	}

	@Override
	public ViewConfig buildDefault(Class<? extends Entity> entityClass,
			Class<? extends QueryView> view) {
		return null;
	}

}
