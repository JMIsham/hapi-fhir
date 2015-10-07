package ca.uhn.fhir.jpa.dao;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.fhir.jpa.entity.ResourceTable;
import ca.uhn.fhir.model.dstu2.resource.Organization;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.Constants;

@ContextConfiguration(locations = { "classpath:fhir-spring-search-config-dstu2.xml" })
public class FhirSearchDaoDstu2Test extends BaseJpaDstu2Test {
	
	@Autowired
	private ISearchDao mySearchDao;
	
	@Before
	@Transactional
	public void beforeFlushFT() {
		  FullTextEntityManager ftem = Search.getFullTextEntityManager(myEntityManager);
		  ftem.purgeAll(ResourceTable.class);
		  ftem.flushToIndexes();
	}
	
	@Test
	public void testContentSearch() {
		Long id1;
		{
			Patient patient = new Patient();
			patient.addIdentifier().setSystem("urn:system").setValue("001");
			patient.addName().addGiven("testSearchStringParamWithNonNormalized_h\u00F6ra");
			patient.addName().addFamily("AAAS");
			patient.addName().addFamily("CCC");
			id1 = myPatientDao.create(patient).getId().toUnqualifiedVersionless().getIdPartAsLong();
		}
		Long id2;
		{
			Patient patient = new Patient();
			patient.addIdentifier().setSystem("urn:system").setValue("002");
			patient.addName().addGiven("testSearchStringParamWithNonNormalized_HORA");
			patient.addName().addFamily("AAAB");
			patient.addName().addFamily("CCC");
			id2 = myPatientDao.create(patient).getId().toUnqualifiedVersionless().getIdPartAsLong();
		}
		Long id3;
		{
			Organization org = new Organization();
			org.setName("DDD");
			id3 = myOrganizationDao.create(org).getId().toUnqualifiedVersionless().getIdPartAsLong();
		}

		SearchParameterMap map = new SearchParameterMap();
		String resourceName = "Patient";

		// One term
		{
			StringAndListParam content = new StringAndListParam();
			content.addAnd(new StringOrListParam().addOr(new StringParam("AAAS")));
			
			map.add(Constants.PARAM_CONTENT, content);
			List<Long> found = mySearchDao.search(resourceName, map);
			assertThat(found, containsInAnyOrder(id1));
		}
		// OR
		{
			StringAndListParam content = new StringAndListParam();
			content.addAnd(new StringOrListParam().addOr(new StringParam("AAAS")).addOr(new StringParam("AAAB")));
			
			map.add(Constants.PARAM_CONTENT, content);
			List<Long> found = mySearchDao.search(resourceName, map);
			assertThat(found, containsInAnyOrder(id1, id2));
		}		
		// AND
		{
			StringAndListParam content = new StringAndListParam();
			content.addAnd(new StringOrListParam().addOr(new StringParam("AAAS")));
			content.addAnd(new StringOrListParam().addOr(new StringParam("CCC")));
			
			map.add(Constants.PARAM_CONTENT, content);
			List<Long> found = mySearchDao.search(resourceName, map);
			assertThat(found, containsInAnyOrder(id1));
		}
		// AND OR
		{
			StringAndListParam content = new StringAndListParam();
			content.addAnd(new StringOrListParam().addOr(new StringParam("AAAB")).addOr(new StringParam("AAAS")));
			content.addAnd(new StringOrListParam().addOr(new StringParam("CCC")));
			
			map.add(Constants.PARAM_CONTENT, content);
			List<Long> found = mySearchDao.search(resourceName, map);
			assertThat(found, containsInAnyOrder(id1, id2));
		}
		// All Resource Types
		{
			StringAndListParam content = new StringAndListParam();
			content.addAnd(new StringOrListParam().addOr(new StringParam("CCC")).addOr(new StringParam("DDD")));
			
			map.add(Constants.PARAM_CONTENT, content);
			List<Long> found = mySearchDao.search(null, map);
			assertThat(found, containsInAnyOrder(id1, id2, id3));
		}

	}
	
	@Test
	public void testNarrativeSearch() {
		Long id1;
		{
			Patient patient = new Patient();
			patient.getText().setDiv("<div>AAAS<p>FOO</p> CCC    </div>");
			id1 = myPatientDao.create(patient).getId().toUnqualifiedVersionless().getIdPartAsLong();
		}
		Long id2;
		{
			Patient patient = new Patient();
			patient.getText().setDiv("<div>AAAB<p>FOO</p> CCC    </div>");
			id2 = myPatientDao.create(patient).getId().toUnqualifiedVersionless().getIdPartAsLong();
		}

		SearchParameterMap map = new SearchParameterMap();
		String resourceName = "Patient";

		// One term
		{
			StringAndListParam content = new StringAndListParam();
			content.addAnd(new StringOrListParam().addOr(new StringParam("AAAS")));
			
			map.add(Constants.PARAM_TEXT, content);
			List<Long> found = mySearchDao.search(resourceName, map);
			assertThat(found, containsInAnyOrder(id1));
		}
		// OR
		{
			StringAndListParam content = new StringAndListParam();
			content.addAnd(new StringOrListParam().addOr(new StringParam("AAAS")).addOr(new StringParam("AAAB")));
			
			map.add(Constants.PARAM_TEXT, content);
			List<Long> found = mySearchDao.search(resourceName, map);
			assertThat(found, containsInAnyOrder(id1, id2));
		}		
		// AND
		{
			StringAndListParam content = new StringAndListParam();
			content.addAnd(new StringOrListParam().addOr(new StringParam("AAAS")));
			content.addAnd(new StringOrListParam().addOr(new StringParam("CCC")));
			
			map.add(Constants.PARAM_TEXT, content);
			List<Long> found = mySearchDao.search(resourceName, map);
			assertThat(found, containsInAnyOrder(id1));
		}
		// AND OR
		{
			StringAndListParam content = new StringAndListParam();
			content.addAnd(new StringOrListParam().addOr(new StringParam("AAAB")).addOr(new StringParam("AAAS")));
			content.addAnd(new StringOrListParam().addOr(new StringParam("CCC")));
			
			map.add(Constants.PARAM_TEXT, content);
			List<Long> found = mySearchDao.search(resourceName, map);
			assertThat(found, containsInAnyOrder(id1, id2));
		}
		// Tag Contents
		{
			StringAndListParam content = new StringAndListParam();
			content.addAnd(new StringOrListParam().addOr(new StringParam("div")));
			
			map.add(Constants.PARAM_TEXT, content);
			List<Long> found = mySearchDao.search(resourceName, map);
			assertThat(found, empty());
		}
	}

}