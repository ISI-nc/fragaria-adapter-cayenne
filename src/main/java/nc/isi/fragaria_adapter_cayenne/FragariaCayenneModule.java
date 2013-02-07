package nc.isi.fragaria_adapter_cayenne;

import nc.isi.fragaria_adapter_cayenne.views.CayenneViewConfigBuilder;
import nc.isi.fragaria_adapter_rewrite.dao.adapters.Adapter;
import nc.isi.fragaria_adapter_rewrite.entities.views.ViewConfigBuilder;
import nc.isi.fragaria_adapter_rewrite.services.FragariaDomainModule;

import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.MappedConfiguration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.SubModule;

@SubModule(FragariaDomainModule.class)
public class FragariaCayenneModule {
	
	public static void bind(ServiceBinder binder) {
		binder.bind(CayenneSerializer.class, CayenneSerializerImpl.class);
		binder.bind(CayenneAdapter.class);
		binder.bind(CayenneViewConfigBuilder.class);
	}

	public void contributeConnectionDataBuilder(
			MappedConfiguration<String, String> configuration) {
		configuration.add("Cayenne", CayenneConnectionData.class.getName());
	}

	public void contributeAdapterManager(
			MappedConfiguration<String, Adapter> configuration,
			CayenneAdapter cayenneAdapter) {
		configuration.add("Cayenne", cayenneAdapter);
	}

	public void contributeViewConfigProvider(Configuration<String> configuration) {
		configuration.add(".sql");
	}

	public void contributeViewConfigBuilderProvider(
			MappedConfiguration<String, ViewConfigBuilder> configuration,
			CayenneViewConfigBuilder cayenneViewConfigBuilder) {
		configuration.add("Cayenne", cayenneViewConfigBuilder);
	}
	

}