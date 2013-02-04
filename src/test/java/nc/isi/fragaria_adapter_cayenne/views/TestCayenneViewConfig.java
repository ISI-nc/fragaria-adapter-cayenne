package nc.isi.fragaria_adapter_cayenne.views;

import junit.framework.TestCase;

public class TestCayenneViewConfig  extends TestCase{
	private static final String EMIT_ABC = "CREATE OR REPLACE VIEW abc AS  SELECT etablissement.id FROM etablissement; ALTER TABLE abc OWNER TO dev;";

	public void testEquals() {
		
	}
}
