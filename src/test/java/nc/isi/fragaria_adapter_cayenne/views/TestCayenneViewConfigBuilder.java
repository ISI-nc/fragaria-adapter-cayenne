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
				resourceFinder.getResourcesMatching("etablissement-abc.sql"));
		CayenneViewConfig conf = (CayenneViewConfig) builder.build("abc",
				files.get(0));
		
		System.out.println(conf.getScript());
		assertTrue("CREATE VIEW ABC AS SELECT ETABLISSEMENT.id,ETABLISSEMENT.name FROM ETABLISSEMENT"
				.equals(conf.getScript()));
	}

}
