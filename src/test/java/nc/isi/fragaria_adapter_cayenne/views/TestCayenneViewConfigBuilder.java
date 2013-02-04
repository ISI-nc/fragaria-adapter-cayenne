package nc.isi.fragaria_adapter_cayenne.views;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import nc.isi.fragaria_adapter_cayenne.model.CayenneQaRegistry;
import nc.isi.fragaria_reflection.services.ResourceFinder;

public class TestCayenneViewConfigBuilder  extends TestCase{
	private final ResourceFinder resourceFinder = CayenneQaRegistry.INSTANCE
			.getRegistry().getService(ResourceFinder.class);

	public void testBuildFromJsFile() {
		CayenneViewConfigBuilder builder = new CayenneViewConfigBuilder();
		List<File> files = new ArrayList<>(
				resourceFinder.getResourcesMatching("test.sql"));
		CayenneViewConfig conf = (CayenneViewConfig) builder.build("test",
				files.get(0));
		
		System.out.println(conf.getScript());
		assertTrue("CREATE OR REPLACE VIEW abc AS  SELECT etablissement.id   FROM etablissement;ALTER TABLE abc  OWNER TO dev;"
				.equals(conf.getScript()));
	}

}
