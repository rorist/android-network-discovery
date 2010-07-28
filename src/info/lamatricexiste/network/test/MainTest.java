package info.lamatricexiste.network.test;

import info.lamatricexiste.network.ActivityDiscovery;
import info.lamatricexiste.network.R;
import android.test.ActivityInstrumentationTestCase2;

import com.jayway.android.robotium.solo.Solo;

public class MainTest extends ActivityInstrumentationTestCase2<ActivityDiscovery> {

	private Solo solo;

	public MainTest() {
		super("info.lamatricexiste.network", ActivityDiscovery.class);
	}

	public void setUp() throws Exception {
		solo = new Solo(getInstrumentation(), getActivity());
	}

	@Override
	public void tearDown() throws Exception {
		try {
			solo.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		getActivity().finish();
		super.tearDown();
	}

	/**
	 * Use cases
	 */

//	public void testDiscover() {
//		solo.assertCurrentActivity("ActivityDiscovery expected", "ActivityDiscovery");
//		solo.clickOnText("Discover");
//		solo.waitForText("Discovery finished !");
//		solo.scrollDown();
//	}
//
//	public void testDiscoverCancel() {
//		solo.clickOnText("Discover");
//		solo.sleep(200);
//		solo.clickOnText("Cancel");
//		solo.waitForText("Discovery canceled !");
//	}
	
	public void testPortscanFromList(){
		solo.clickOnText("Discover");
		solo.waitForText("Discovery finished !");
		solo.clickInList(0, R.id.output);
		solo.assertCurrentActivity("ActivityPortscan expected", "ActivityPortscan");
		solo.waitForText("Scan finished !");
	}

}
