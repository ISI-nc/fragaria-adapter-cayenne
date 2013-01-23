package nc.isi.fragaria_adapter_rewrite.services.domain;

import java.util.Collection;

public interface ConnectionDataBuilder {

	public ConnectionData build(String dsType, Object... params);

	public ConnectionData build(String dsType, Collection<Object> params);

}