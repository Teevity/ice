package com.netflix.ice.basic;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Test;

import org.json.JSONArray;
import com.netflix.ice.tag.Product;


/**
 * Note: These tests require a running instance of Edda running at the url configured in ice.properties, so they 
 * strictly speaking not unit tests any more...
 */
public class EddaResourceServiceTest {

	@Test
	public void test() throws Exception {
		EddaResourceService service = new EddaResourceService(new Properties());

		service.init();

		// does nothing really...
		service.commit();

		assertNotNull(service.getProductsWithResources());

		assertEquals("Product-name for unsupported resource", "somename", service.getResource(null, null, new Product("somename"), null, null, 0));
		assertEquals("Error for empty resourceId", "Error", service.getResource(null, null, Product.ec2, null, null, 0));
		assertEquals("Error for empty resourceId", "Error", service.getResource(null, null, Product.ec2, "", null, 0));

		assertEquals("Unknown for resourceIds that we do not find", "Unknown", service.getResource(null, null, Product.ec2, "someunknowninstance", null, 0));

		JSONArray instances = service.readInstanceArray();

		String resource = service.getResource(null, null, Product.ec2, instances.getString(0), null, 0);
		assertFalse("Not Error for an actual instance", "Error".equals(resource));

		resource = service.getResource(null, null, Product.ec2_instance, instances.getString(0), null, 0);
		assertFalse("Not Error for an actual instance", "Error".equals(resource));

		for(int i = 0;i < instances.length();i++) {
			resource = service.getResource(null, null, Product.ec2_instance, instances.getString(i), null, 0);
			assertFalse("Not Error for an actual instance", "Error".equals(resource));
		}
	}

	@Test
	public void testWrongURL() throws Exception {
		// use a normal setup for retrieving the instances
		EddaResourceService service = new EddaResourceService(new Properties());
		JSONArray instances = service.readInstanceArray();

		// overwrite config with an invalid hostname
		Properties prop = new Properties();
		prop.setProperty("ice.eddaresourceservice.url", "http://invalidhostname:18081/edda/api/v2/");
		service = new EddaResourceService(prop);

		// now the retrieved resources should return an error even for valid instances
		String resource = service.getResource(null, null, Product.ec2, instances.getString(0), null, 0);
		assertTrue("Error even for an actual instance when using wrong URL", "Error".equals(resource));

		// overwrite config with an invalid URL
		prop.setProperty("ice.eddaresourceservice.url", "sasie://invalidhostname:18081/edda/api/v2/");
		service = new EddaResourceService(prop);

		// now the retrieved resources should return an error even for valid instances
		resource = service.getResource(null, null, Product.ec2, instances.getString(0), null, 0);
		assertTrue("Error even for an actual instance when using wrong URL", "Error".equals(resource));
	}
}
