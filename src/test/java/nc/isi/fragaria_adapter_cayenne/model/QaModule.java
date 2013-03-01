package nc.isi.fragaria_adapter_cayenne.model;

import nc.isi.fragaria_adapter_cayenne.CayenneConnectionData;
import nc.isi.fragaria_adapter_cayenne.FragariaCayenneModule;
import nc.isi.fragaria_adapter_rewrite.resources.DataSourceMetadata;
import nc.isi.fragaria_adapter_rewrite.resources.Datasource;
import nc.isi.fragaria_adapter_rewrite.resources.DatasourceImpl;
import nc.isi.fragaria_dsloader_yaml.YamlDsLoaderModule;

import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.annotations.SubModule;

@SubModule({ FragariaCayenneModule.class, YamlDsLoaderModule.class })
public class QaModule {
	public static final String[] PACKAGE_NAME = { "nc.isi" };

	public void contributeResourceFinder(Configuration<String> configuration) {
		configuration.add(PACKAGE_NAME[0]);
	}

	public void contributeViewInitializer(Configuration<String> configuration) {
		configuration.add(PACKAGE_NAME[0]);
	}

	public void contributeDataSourceProvider(
			MappedConfiguration<String, Datasource> configuration) {
		configuration.add("loc", new DatasourceImpl("loc",
				new DataSourceMetadata("Cayenne", new CayenneConnectionData(
						"cayenne-config.xml") {
				}, true)));
		configuration.add("rer-histo", new DatasourceImpl("rer-histo",
				new DataSourceMetadata("Cayenne", new CayenneConnectionData(
						"cayenne-rer-histo.xml") {
				}, true)));
	}

}