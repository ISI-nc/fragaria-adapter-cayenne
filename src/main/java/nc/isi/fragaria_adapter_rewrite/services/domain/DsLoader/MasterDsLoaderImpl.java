package nc.isi.fragaria_adapter_rewrite.services.domain.DsLoader;

import java.util.List;
import java.util.Map;

import nc.isi.fragaria_adapter_rewrite.services.domain.Datasource;

import com.google.common.collect.Maps;

public class MasterDsLoaderImpl implements MasterDsLoader {

	private final List<SpecificDsLoader> list;

	public MasterDsLoaderImpl(List<SpecificDsLoader> list) {
		this.list = list;
	}

	@Override
	public Map<String, Datasource> getDs() {
		Map<String, Datasource> map = Maps.newHashMap();
		for (SpecificDsLoader loader : list)
			map.putAll(loader.getDs());
		return map;
	}

}
