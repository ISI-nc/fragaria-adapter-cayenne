package nc.isi.fragaria_adapter_cayenne.model;

import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;

public enum CayenneQaRegistry {
	INSTANCE;

	private final Registry registry = RegistryBuilder
			.buildAndStartupRegistry(QaModule.class);

	public Registry getRegistry() {
		return registry;
	}
}