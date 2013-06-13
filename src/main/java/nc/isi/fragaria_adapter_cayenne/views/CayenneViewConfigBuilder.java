package nc.isi.fragaria_adapter_cayenne.views;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import nc.isi.fragaria_adapter_rewrite.entities.Entity;
import nc.isi.fragaria_adapter_rewrite.entities.views.QueryView;
import nc.isi.fragaria_adapter_rewrite.entities.views.ViewConfig;
import nc.isi.fragaria_adapter_rewrite.entities.views.ViewConfigBuilder;

import com.google.common.base.Throwables;

/**
 * 
 * @author bjonathas
 * 
 *         ViewConfigBuilder for Cayenne. It creates the CayenneViewConfig by
 *         reading configuration files with .sql extension. These files contain
 *         a sql script used to create view into the database.
 */
public class CayenneViewConfigBuilder implements ViewConfigBuilder {
	public static final String SQL = ".*\\.sql";

	@Override
	public ViewConfig build(String name, String file) {
		CayenneViewConfig config = new CayenneViewConfig(name);
		if (file.matches(SQL)) {
			try {
				config.setScript(getScript(file));
			} catch (IOException e) {
				throw Throwables.propagate(e);
			}
		} else {
			throw new IllegalArgumentException(String.format(
					"Unknown file type : %s", file));
		}
		return config;
	}

	private String getScript(String file) throws IOException {
		InputStream input = null;
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		String script = "";
		try {
			input = classLoader.getResourceAsStream(file);
			BufferedReader br = new BufferedReader(new InputStreamReader(input));
			String line;
			while ((line = br.readLine()) != null) {
				script += line;
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
		CayenneViewConfig config = new CayenneViewConfig(entityClass
				.getSimpleName().toLowerCase());
		return config;
	}

}
