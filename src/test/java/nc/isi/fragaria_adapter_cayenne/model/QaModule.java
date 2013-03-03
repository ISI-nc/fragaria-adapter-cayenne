package nc.isi.fragaria_adapter_cayenne.model;

import nc.isi.fragaria_adapter_cayenne.FragariaCayenneModule;
import nc.isi.fragaria_adapter_rewrite.services.FragariaDomainModule;
import nc.isi.fragaria_dsloader_yaml.YamlDsLoaderModule;

import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.annotations.SubModule;

@SubModule({ FragariaDomainModule.class,FragariaCayenneModule.class, YamlDsLoaderModule.class })
public class QaModule {
	public static final String[] PACKAGE_NAME = { "nc.isi" };

	public void contributeReflectionProvider(Configuration<String> configuration) {
		configuration.add(PACKAGE_NAME[0]);
	}

	public void contributeViewInitializer(Configuration<String> configuration) {
		configuration.add(PACKAGE_NAME[0]);
	}
	
}